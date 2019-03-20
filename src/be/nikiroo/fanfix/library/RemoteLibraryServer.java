package be.nikiroo.fanfix.library;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Progress.ProgressListener;
import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.server.ConnectActionServerObject;
import be.nikiroo.utils.serial.server.ServerObject;

/**
 * Create a new remote server that will listen for order on the given port.
 * <p>
 * The available commands are given as arrays of objects (first item is the key,
 * second is the command, the rest are the arguments).
 * <p>
 * The md5 is always a String (the MD5 hash of the access key), the commands are
 * also Strings; the parameters vary depending upon the command.
 * <ul>
 * <li>[md5] PING: will return PONG if the key is accepted</li>
 * <li>[md5] GET_METADATA *: will return the metadata of all the stories in the
 * library (array)</li>
 * *
 * <li>[md5] GET_METADATA [luid]: will return the metadata of the story of LUID
 * luid</li>
 * <li>[md5] GET_STORY [luid]: will return the given story if it exists (or NULL
 * if not)</li>
 * <li>[md5] SAVE_STORY [luid]: save the story (that must be sent just after the
 * command) with the given LUID, then return the LUID</li>
 * <li>[md5] IMPORT [url]: save the story found at the given URL, then return
 * the LUID</li>
 * <li>[md5] DELETE_STORY [luid]: delete the story of LUID luid</li>
 * <li>[md5] GET_COVER [luid]: return the cover of the story</li>
 * <li>[md5] GET_CUSTOM_COVER ["SOURCE"|"AUTHOR"] [source]: return the cover for
 * this source/author</li>
 * <li>[md5] SET_COVER ["SOURCE"|"AUTHOR"] [value] [luid]: set the default cover
 * for the given source/author to the cover of the story denoted by luid</li>
 * <li>[md5] CHANGE_SOURCE [luid] [new source]: change the source of the story
 * of LUID luid</li>
 * <li>[md5] EXIT: stop the server</li>
 * </ul>
 * 
 * @author niki
 */
public class RemoteLibraryServer extends ServerObject {
	private final String md5;

	/**
	 * Create a new remote server (will not be active until
	 * {@link RemoteLibraryServer#start()} is called).
	 * 
	 * @param key
	 *            the key that will restrict access to this server
	 * @param port
	 *            the port to listen on
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public RemoteLibraryServer(String key, int port) throws IOException {
		super("Fanfix remote library", port, true);
		this.md5 = StringUtils.getMd5Hash(key);

		setTraceHandler(Instance.getTraceHandler());
	}

	@Override
	protected Object onRequest(ConnectActionServerObject action,
			Version clientVersion, Object data) throws Exception {
		String md5 = "";
		String command = "";
		Object[] args = new Object[0];
		if (data instanceof Object[]) {
			Object[] dataArray = (Object[]) data;
			if (dataArray.length >= 2) {
				md5 = "" + dataArray[0];
				command = "" + dataArray[1];

				args = new Object[dataArray.length - 2];
				for (int i = 2; i < dataArray.length; i++) {
					args[i - 2] = dataArray[i];
				}
			}
		}

		String trace = "[ " + command + "] ";
		for (Object arg : args) {
			trace += arg + " ";
		}
		getTraceHandler().trace(trace);

		if (!md5.equals(this.md5)) {
			getTraceHandler().trace("Key rejected.");
			return null;
		}

		long start = new Date().getTime();
		Object rep = doRequest(action, command, args);

		getTraceHandler().trace(
				String.format("[>%s]: %d ms", command,
						(new Date().getTime() - start)));

		return rep;
	}

	private Object doRequest(ConnectActionServerObject action, String command,
			Object[] args) throws NoSuchFieldException, NoSuchMethodException,
			ClassNotFoundException, IOException {
		if ("PING".equals(command)) {
			return "PONG";
		} else if ("GET_METADATA".equals(command)) {
			if ("*".equals(args[0])) {
				Progress pg = createPgForwarder(action);

				List<MetaData> metas = new ArrayList<MetaData>();

				for (MetaData meta : Instance.getLibrary().getMetas(pg)) {
					MetaData light;
					if (meta.getCover() == null) {
						light = meta;
					} else {
						light = meta.clone();
						light.setCover(null);
					}

					metas.add(light);
				}

				forcePgDoneSent(pg);
				return metas.toArray(new MetaData[] {});
			}

			return new MetaData[] { Instance.getLibrary().getInfo(
					(String) args[0]) };
		} else if ("GET_STORY".equals(command)) {
			MetaData meta = Instance.getLibrary().getInfo((String) args[0]);
			meta = meta.clone();
			meta.setCover(null);

			action.send(meta);
			action.rec();

			Story story = Instance.getLibrary()
					.getStory((String) args[0], null);
			for (Object obj : breakStory(story)) {
				action.send(obj);
				action.rec();
			}
		} else if ("SAVE_STORY".equals(command)) {
			List<Object> list = new ArrayList<Object>();

			action.send(null);
			Object obj = action.rec();
			while (obj != null) {
				list.add(obj);
				action.send(null);
				obj = action.rec();
			}

			Story story = rebuildStory(list);
			Instance.getLibrary().save(story, (String) args[0], null);
			return story.getMeta().getLuid();
		} else if ("IMPORT".equals(command)) {
			Progress pg = createPgForwarder(action);
			Story story = Instance.getLibrary().imprt(
					new URL((String) args[0]), pg);
			forcePgDoneSent(pg);
			return story.getMeta().getLuid();
		} else if ("DELETE_STORY".equals(command)) {
			Instance.getLibrary().delete((String) args[0]);
		} else if ("GET_COVER".equals(command)) {
			return Instance.getLibrary().getCover((String) args[0]);
		} else if ("GET_CUSTOM_COVER".equals(command)) {
			if ("SOURCE".equals(args[0])) {
				return Instance.getLibrary().getCustomSourceCover(
						(String) args[1]);
			} else if ("AUTHOR".equals(args[0])) {
				return Instance.getLibrary().getCustomAuthorCover(
						(String) args[1]);
			} else {
				return null;
			}
		} else if ("SET_COVER".equals(command)) {
			if ("SOURCE".equals(args[0])) {
				Instance.getLibrary().setSourceCover((String) args[1],
						(String) args[2]);
			} else if ("AUTHOR".equals(args[0])) {
				Instance.getLibrary().setAuthorCover((String) args[1],
						(String) args[2]);
			}
		} else if ("CHANGE_STA".equals(command)) {
			Progress pg = createPgForwarder(action);
			Instance.getLibrary().changeSTA((String) args[0], (String) args[1],
					(String) args[2], (String) args[3], pg);
			forcePgDoneSent(pg);
		} else if ("EXIT".equals(command)) {
			stop(0, false);
		}

		return null;
	}

	@Override
	protected void onError(Exception e) {
		getTraceHandler().error(e);
	}

	/**
	 * Break a story in multiple {@link Object}s for easier serialisation.
	 * 
	 * @param story
	 *            the {@link Story} to break
	 * 
	 * @return the list of {@link Object}s
	 */
	static List<Object> breakStory(Story story) {
		List<Object> list = new ArrayList<Object>();

		story = story.clone();
		list.add(story);

		if (story.getMeta().isImageDocument()) {
			for (Chapter chap : story) {
				list.add(chap);
				list.addAll(chap.getParagraphs());
				chap.setParagraphs(new ArrayList<Paragraph>());
			}
			story.setChapters(new ArrayList<Chapter>());
		}

		return list;
	}

	/**
	 * Rebuild a story from a list of broke up {@link Story} parts.
	 * 
	 * @param list
	 *            the list of {@link Story} parts
	 * 
	 * @return the reconstructed {@link Story}
	 */
	static Story rebuildStory(List<Object> list) {
		Story story = null;
		Chapter chap = null;

		for (Object obj : list) {
			if (obj instanceof Story) {
				story = (Story) obj;
			} else if (obj instanceof Chapter) {
				chap = (Chapter) obj;
				story.getChapters().add(chap);
			} else if (obj instanceof Paragraph) {
				chap.getParagraphs().add((Paragraph) obj);
			}
		}

		return story;
	}

	/**
	 * Update the {@link Progress} with the adequate {@link Object} received
	 * from the network via {@link RemoteLibraryServer}.
	 * 
	 * @param pg
	 *            the {@link Progress} to update
	 * @param rep
	 *            the object received from the network
	 * 
	 * @return TRUE if it was a progress event, FALSE if not
	 */
	static boolean updateProgress(Progress pg, Object rep) {
		if (rep instanceof Integer[]) {
			Integer[] a = (Integer[]) rep;
			if (a.length == 3) {
				int min = a[0];
				int max = a[1];
				int progress = a[2];

				if (min >= 0 && min <= max) {
					pg.setMinMax(min, max);
					pg.setProgress(progress);

					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Create a {@link Progress} that will forward its progress over the
	 * network.
	 * 
	 * @param action
	 *            the {@link ConnectActionServerObject} to use to forward it
	 * 
	 * @return the {@link Progress}
	 */
	private static Progress createPgForwarder(
			final ConnectActionServerObject action) {
		final Boolean[] isDoneForwarded = new Boolean[] { false };
		final Progress pg = new Progress() {
			@Override
			public boolean isDone() {
				return isDoneForwarded[0];
			}
		};

		final Integer[] p = new Integer[] { -1, -1, -1 };
		final Long[] lastTime = new Long[] { new Date().getTime() };
		pg.addProgressListener(new ProgressListener() {
			@Override
			public void progress(Progress progress, String name) {
				int min = pg.getMin();
				int max = pg.getMax();
				int relativeProgress = min
						+ (int) Math.round(pg.getRelativeProgress()
								* (max - min));

				// Do not re-send the same value twice over the wire,
				// unless more than 2 seconds have elapsed (to maintain the
				// connection)
				if ((p[0] != min || p[1] != max || p[2] != relativeProgress)
						|| (new Date().getTime() - lastTime[0] > 2000)) {
					p[0] = min;
					p[1] = max;
					p[2] = relativeProgress;

					try {
						action.send(new Integer[] { min, max, relativeProgress });
						action.rec();
					} catch (Exception e) {
						Instance.getTraceHandler().error(e);
					}

					lastTime[0] = new Date().getTime();
				}

				isDoneForwarded[0] = (pg.getProgress() >= pg.getMax());
			}
		});

		return pg;
	}

	// with 30 seconds timeout
	private static void forcePgDoneSent(Progress pg) {
		long start = new Date().getTime();
		pg.done();
		while (!pg.isDone() && new Date().getTime() - start < 30000) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Instance.getTraceHandler().error(e);
			}
		}
	}
}
