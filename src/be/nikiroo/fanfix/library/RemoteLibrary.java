package be.nikiroo.fanfix.library;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.server.ConnectActionClientObject;

/**
 * This {@link BasicLibrary} will access a remote server to list the available
 * stories, and download the ones you try to load to the local directory
 * specified in the configuration.
 * 
 * @author niki
 */
public class RemoteLibrary extends BasicLibrary {
	private String host;
	private int port;
	private final String md5;

	/**
	 * Create a {@link RemoteLibrary} linked to the given server.
	 * 
	 * @param key
	 *            the key that will allow us to exchange information with the
	 *            server
	 * @param host
	 *            the host to contact or NULL for localhost
	 * @param port
	 *            the port to contact it on
	 */
	public RemoteLibrary(String key, String host, int port) {
		this.md5 = StringUtils.getMd5Hash(key);
		this.host = host;
		this.port = port;
	}

	@Override
	public String getLibraryName() {
		return host + ":" + port;
	}

	@Override
	public Status getStatus() {
		final Status[] result = new Status[1];

		result[0] = Status.INVALID;

		ConnectActionClientObject action = null;
		try {
			action = new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Object rep = send(new Object[] { md5, "PING" });
					if ("PONG".equals(rep)) {
						result[0] = Status.READY;
					} else {
						result[0] = Status.UNAUTORIZED;
					}
				}

				@Override
				protected void onError(Exception e) {
					result[0] = Status.UNAVAILABLE;
				}
			};

		} catch (UnknownHostException e) {
			result[0] = Status.INVALID;
		} catch (IllegalArgumentException e) {
			result[0] = Status.INVALID;
		} catch (Exception e) {
			result[0] = Status.UNAVAILABLE;
		}

		if (action != null) {
			try {
				action.connect();
			} catch (Exception e) {
				result[0] = Status.UNAVAILABLE;
			}
		}

		return result[0];
	}

	@Override
	public Image getCover(final String luid) {
		final Image[] result = new Image[1];

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Object rep = send(new Object[] { md5, "GET_COVER", luid });
					result[0] = (Image) rep;
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	public Image getCustomSourceCover(final String source) {
		final Image[] result = new Image[1];

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Object rep = send(new Object[] { md5,
							"GET_CUSTOM_SOURCE_COVER", source });
					result[0] = (Image) rep;
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	public synchronized Story getStory(final String luid, Progress pg) {
		final Progress pgF = pg;
		final Story[] result = new Story[1];

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;
					if (pg == null) {
						pg = new Progress();
					}

					Object rep = send(new Object[] { md5, "GET_STORY", luid });

					MetaData meta = null;
					if (rep instanceof MetaData) {
						meta = (MetaData) rep;
						if (meta.getWords() <= Integer.MAX_VALUE) {
							pg.setMinMax(0, (int) meta.getWords());
						}
					}

					List<Object> list = new ArrayList<Object>();
					for (Object obj = send(null); obj != null; obj = send(null)) {
						list.add(obj);
						pg.add(1);
					}

					result[0] = RemoteLibraryServer.rebuildStory(list);
					pg.done();
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return result[0];
	}

	@Override
	public synchronized Story save(final Story story, final String luid,
			Progress pg) throws IOException {
		final String[] luidSaved = new String[1];
		Progress pgSave = new Progress();
		Progress pgRefresh = new Progress();
		if (pg == null) {
			pg = new Progress();
		}

		pg.setMinMax(0, 10);
		pg.addProgress(pgSave, 9);
		pg.addProgress(pgRefresh, 1);

		final Progress pgF = pgSave;

		new ConnectActionClientObject(host, port, true) {
			@Override
			public void action(Version serverVersion) throws Exception {
				Progress pg = pgF;
				if (story.getMeta().getWords() <= Integer.MAX_VALUE) {
					pg.setMinMax(0, (int) story.getMeta().getWords());
				}

				send(new Object[] { md5, "SAVE_STORY", luid });

				List<Object> list = RemoteLibraryServer.breakStory(story);
				for (Object obj : list) {
					send(obj);
					pg.add(1);
				}

				luidSaved[0] = (String) send(null);

				pg.done();
			}

			@Override
			protected void onError(Exception e) {
				Instance.getTraceHandler().error(e);
			}
		}.connect();

		// because the meta changed:
		MetaData meta = getInfo(luidSaved[0]);
		if (story.getMeta().getClass() != null) {
			// If already available locally:
			meta.setCover(story.getMeta().getCover());
		} else {
			// If required:
			meta.setCover(getCover(meta.getLuid()));
		}
		story.setMeta(meta);

		pg.done();

		return story;
	}

	@Override
	public synchronized void delete(final String luid) throws IOException {
		new ConnectActionClientObject(host, port, true) {
			@Override
			public void action(Version serverVersion) throws Exception {
				send(new Object[] { md5, "DELETE_STORY", luid });
			}

			@Override
			protected void onError(Exception e) {
				Instance.getTraceHandler().error(e);
			}
		}.connect();
	}

	@Override
	public void setSourceCover(final String source, final String luid) {
		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					send(new Object[] { md5, "SET_SOURCE_COVER", source, luid });
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	@Override
	// Could work (more slowly) without it
	public Story imprt(final URL url, Progress pg) throws IOException {
		// Import the file locally if it is actually a file
		if (url == null || url.getProtocol().equalsIgnoreCase("file")) {
			return super.imprt(url, pg);
		}

		// Import it remotely if it is an URL

		if (pg == null) {
			pg = new Progress();
		}

		pg.setMinMax(0, 2);
		Progress pgImprt = new Progress();
		Progress pgGet = new Progress();
		pg.addProgress(pgImprt, 1);
		pg.addProgress(pgGet, 1);

		final Progress pgF = pgImprt;
		final String[] luid = new String[1];

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;

					Object rep = send(new Object[] { md5, "IMPORT",
							url.toString() });

					while (true) {
						if (!RemoteLibraryServer.updateProgress(pg, rep)) {
							break;
						}

						rep = send(null);
					}

					pg.done();
					luid[0] = (String) rep;
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}

		if (luid[0] == null) {
			throw new IOException("Remote failure");
		}

		Story story = getStory(luid[0], pgGet);
		pgGet.done();

		pg.done();
		return story;
	}

	@Override
	// Could work (more slowly) without it
	public synchronized void changeSource(final String luid,
			final String newSource, Progress pg) throws IOException {
		final Progress pgF = pg == null ? new Progress() : pg;

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;

					Object rep = send(new Object[] { md5, "CHANGE_SOURCE",
							luid, newSource });
					while (true) {
						if (!RemoteLibraryServer.updateProgress(pg, rep)) {
							break;
						}

						rep = send(null);
					}
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	@Override
	public synchronized File getFile(final String luid, Progress pg) {
		throw new java.lang.InternalError(
				"Operation not supportorted on remote Libraries");
	}

	/**
	 * Stop the server.
	 */
	public void exit() {
		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					send(new Object[] { md5, "EXIT" });
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	@Override
	public synchronized MetaData getInfo(String luid) {
		List<MetaData> metas = getMetasList(luid, null);
		if (!metas.isEmpty()) {
			return metas.get(0);
		}

		return null;
	}

	@Override
	protected List<MetaData> getMetas(Progress pg) {
		return getMetasList("*", pg);
	}

	@Override
	protected void updateInfo(MetaData meta) {
		// Will be taken care of directly server side
	}

	@Override
	protected void deleteInfo(String luid) {
		// Will be taken care of directly server side
	}

	// The following methods are only used by Save and Delete in BasicLibrary:

	@Override
	protected int getNextId() {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected void doDelete(String luid) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected Story doSave(Story story, Progress pg) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}

	//

	/**
	 * Return the meta of the given story or a list of all known metas if the
	 * luid is "*".
	 * <p>
	 * Will not get the covers.
	 * 
	 * @param luid
	 *            the luid of the story or *
	 * @param pg
	 *            the optional progress
	 * 
	 * 
	 * @return the metas
	 */
	private List<MetaData> getMetasList(final String luid, Progress pg) {
		final Progress pgF = pg;
		final List<MetaData> metas = new ArrayList<MetaData>();

		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					Progress pg = pgF;
					if (pg == null) {
						pg = new Progress();
					}

					Object rep = send(new Object[] { md5, "GET_METADATA", luid });

					while (true) {
						if (!RemoteLibraryServer.updateProgress(pg, rep)) {
							break;
						}

						rep = send(null);
					}

					if (rep instanceof MetaData[]) {
						for (MetaData meta : (MetaData[]) rep) {
							metas.add(meta);
						}
					} else if (rep != null) {
						metas.add((MetaData) rep);
					}
				}

				@Override
				protected void onError(Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}.connect();
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
		}

		return metas;
	}
}
