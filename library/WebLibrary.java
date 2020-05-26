package be.nikiroo.fanfix.library;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.JsonIO;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;

/**
 * This {@link BasicLibrary} will access a remote server to list the available
 * stories, and download the ones you try to load to the local directory
 * specified in the configuration.
 * <p>
 * This remote library uses http:// or https://.
 * 
 * @author niki
 */
public class WebLibrary extends BasicLibrary {
	private String host;
	private int port;
	private final String key;
	private final String subkey;

	// informative only (server will make the actual checks)
	private boolean rw;

	/**
	 * Create a {@link RemoteLibrary} linked to the given server.
	 * <p>
	 * Note that the key is structured:
	 * <tt><b><i>xxx</i></b>(|<b><i>yyy</i></b>|<b>wl</b>)(|<b>rw</b>)</tt>
	 * <p>
	 * Note that anything before the first pipe (<tt>|</tt>) character is
	 * considered to be the encryption key, anything after that character is
	 * called the subkey (including the other pipe characters and flags!).
	 * <p>
	 * This is important because the subkey (including the pipe characters and
	 * flags) must be present as-is in the server configuration file to be
	 * allowed.
	 * <ul>
	 * <li><b><i>xxx</i></b>: the encryption key used to communicate with the
	 * server</li>
	 * <li><b><i>yyy</i></b>: the secondary key</li>
	 * <li><b>rw</b>: flag to allow read and write access if it is not the
	 * default on this server</li>
	 * <li><b>wl</b>: flag to allow access to all the stories (bypassing the
	 * whitelist if it exists)</li>
	 * </ul>
	 * <p>
	 * Some examples:
	 * <ul>
	 * <li><b>my_key</b>: normal connection, will take the default server
	 * options</li>
	 * <li><b>my_key|agzyzz|wl</b>: will ask to bypass the white list (if it
	 * exists)</li>
	 * <li><b>my_key|agzyzz|rw</b>: will ask read-write access (if the default
	 * is read-only)</li>
	 * <li><b>my_key|agzyzz|wl|rw</b>: will ask both read-write access and white
	 * list bypass</li>
	 * </ul>
	 * 
	 * @param key
	 *            the key that will allow us to exchange information with the
	 *            server
	 * @param host
	 *            the host to contact or NULL for localhost
	 * @param port
	 *            the port to contact it on
	 */
	public WebLibrary(String key, String host, int port) {
		int index = -1;
		if (key != null) {
			index = key.indexOf('|');
		}

		if (index >= 0) {
			this.key = key.substring(0, index);
			this.subkey = key.substring(index + 1);
		} else {
			this.key = key;
			this.subkey = "";
		}

		this.rw = subkey.contains("|rw");

		this.host = host;
		this.port = port;
	}

	/**
	 * Return the version of the program running server-side.
	 * <p>
	 * Never returns NULL.
	 * 
	 * @return the version or an empty {@link Version} if not known
	 */
	public Version getVersion() {
		try {
			InputStream in = post(WebLibraryUrls.VERSION_URL);
			try {
				return new Version(IOUtils.readSmallStream(in));
			} finally {
				in.close();
			}
		} catch (IOException e) {
		}

		return new Version();
	}

	/**
	 * Stop the server.
	 * 
	 * @throws IOException
	 *             in case of I/O errors
	 */
	public void stop() throws IOException {
		try {
			post(WebLibraryUrls.EXIT_URL, null).close();
		} catch (Exception e) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e1) {
			}
			if (getStatus() != Status.UNAVAILABLE) {
				throw new IOException("Cannot exit the library", e);
			}
		}
	}

	@Override
	public Status getStatus() {
		try {
			post(WebLibraryUrls.INDEX_URL).close();
		} catch (IOException e) {
			try {
				post("/style.css").close();
				return Status.UNAUTHORIZED;
			} catch (IOException ioe) {
				return Status.UNAVAILABLE;
			}
		}

		return rw ? Status.READ_WRITE : Status.READ_ONLY;
	}

	@Override
	public String getLibraryName() {
		return (rw ? "[READ-ONLY] " : "") + host + ":" + port + " ("
				+ getVersion() + ")";
	}

	@Override
	public Image getCover(String luid) throws IOException {
		InputStream in = post(WebLibraryUrls.getStoryUrlCover(luid));
		try {
			Image img = new Image(in);
			if (img.getSize() == 0) {
				img.close();
				img = null;
			}

			return img;
		} finally {
			in.close();
		}
	}

	@Override
	public Image getCustomSourceCover(String source) throws IOException {
		InputStream in = post(WebLibraryUrls.getCoverUrlSource(source));
		try {
			Image img = new Image(in);
			if (img.getSize() == 0) {
				img.close();
				img = null;
			}

			return img;
		} finally {
			in.close();
		}
	}

	@Override
	public Image getCustomAuthorCover(String author) throws IOException {
		InputStream in = post(WebLibraryUrls.getCoverUrlAuthor(author));
		try {
			Image img = new Image(in);
			if (img.getSize() == 0) {
				img.close();
				img = null;
			}

			return img;
		} finally {
			in.close();
		}
	}

	@Override
	public void setSourceCover(String source, String luid) throws IOException {
		Map<String, String> post = new HashMap<String, String>();
		post.put("luid", luid);
		post(WebLibraryUrls.getCoverUrlSource(source), post).close();
	}

	@Override
	public void setAuthorCover(String author, String luid) throws IOException {
		Map<String, String> post = new HashMap<String, String>();
		post.put("luid", luid);
		post(WebLibraryUrls.getCoverUrlAuthor(author), post).close();
	}

	@Override
	public synchronized Story getStory(final String luid, Progress pg)
			throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		Story story;
		InputStream in = post(WebLibraryUrls.getStoryUrlJson(luid));
		try {
			JSONObject json = new JSONObject(IOUtils.readSmallStream(in));
			story = JsonIO.toStory(json);
		} finally {
			in.close();
		}

		int max = 0;
		for (Chapter chap : story) {
			max += chap.getParagraphs().size();
		}
		pg.setMinMax(0, max);

		story.getMeta().setCover(getCover(luid));
		int chapNum = 1;
		for (Chapter chap : story) {
			int number = 1;
			for (Paragraph para : chap) {
				if (para.getType() == ParagraphType.IMAGE) {
					InputStream subin = post(
							WebLibraryUrls.getStoryUrl(luid, chapNum, number));
					try {
						Image img = new Image(subin);
						if (img.getSize() > 0) {
							para.setContentImage(img);
						}
					} finally {
						subin.close();
					}
				}

				pg.add(1);
				number++;
			}

			chapNum++;
		}

		pg.done();
		return story;
	}

	@Override
	protected List<MetaData> getMetas(Progress pg) throws IOException {
		List<MetaData> metas = new ArrayList<MetaData>();
		InputStream in = post(WebLibraryUrls.LIST_URL_METADATA);
		JSONArray jsonArr = new JSONArray(IOUtils.readSmallStream(in));
		for (int i = 0; i < jsonArr.length(); i++) {
			JSONObject json = jsonArr.getJSONObject(i);
			metas.add(JsonIO.toMetaData(json));
		}

		return metas;
	}

	@Override
	// Could work (more slowly) without it
	public MetaData imprt(final URL url, Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		// Import the file locally if it is actually a file

		if (url == null || url.getProtocol().equalsIgnoreCase("file")) {
			return super.imprt(url, pg);
		}

		// Import it remotely if it is an URL

		try {
			String luid = null;

			Map<String, String> post = new HashMap<String, String>();
			post.put("url", url.toString());
			InputStream in = post(WebLibraryUrls.IMPRT_URL_IMPORT, post);
			try {
				luid = IOUtils.readSmallStream(in);
			} finally {
				in.close();
			}

			Progress subPg = null;
			do {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}

				in = post(WebLibraryUrls.getImprtProgressUrl(luid));
				try {
					subPg = JsonIO.toProgress(
							new JSONObject(IOUtils.readSmallStream(in)));
					pg.setName(subPg.getName());
					pg.setMinMax(subPg.getMin(), subPg.getMax());
					pg.setProgress(subPg.getProgress());
				} catch (Exception e) {
					subPg = null;
				} finally {
					in.close();
				}
			} while (subPg != null);

			in = post(WebLibraryUrls.getStoryUrlMetadata(luid));
			try {
				return JsonIO.toMetaData(
						new JSONObject(IOUtils.readSmallStream(in)));
			} finally {
				in.close();
			}
		} finally {
			pg.done();
		}
	}

	@Override
	// Could work (more slowly) without it
	protected synchronized void changeSTA(final String luid,
			final String newSource, final String newTitle,
			final String newAuthor, Progress pg) throws IOException {
		MetaData meta = getInfo(luid);
		if (meta != null) {
			if (!meta.getSource().equals(newSource)) {
				Map<String, String> post = new HashMap<String, String>();
				post.put("value", newSource);
				post(WebLibraryUrls.getStoryUrlSource(luid), post).close();
			}
			if (!meta.getTitle().equals(newTitle)) {
				Map<String, String> post = new HashMap<String, String>();
				post.put("value", newTitle);
				post(WebLibraryUrls.getStoryUrlTitle(luid), post).close();
			}
			if (!meta.getAuthor().equals(newAuthor)) {
				Map<String, String> post = new HashMap<String, String>();
				post.put("value", newAuthor);
				post(WebLibraryUrls.getStoryUrlAuthor(luid), post).close();
			}
		}
	}

	@Override
	public synchronized void delete(String luid) throws IOException {
		post(WebLibraryUrls.getDeleteUrlStory(luid), null).close();
	}

	@Override
	protected void updateInfo(MetaData meta) {
		// Will be taken care of directly server side
	}

	@Override
	protected void invalidateInfo(String luid) {
		// Will be taken care of directly server side
	}

	// The following methods are only used by Save and Delete in BasicLibrary:

	@Override
	protected String getNextId() {
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

	@Override
	public File getFile(final String luid, Progress pg) {
		throw new java.lang.InternalError(
				"Operation not supportorted on remote Libraries");
	}

	// starts with "/", never NULL
	private InputStream post(String path) throws IOException {
		return post(path, null);
	}

	// starts with "/", never NULL
	private InputStream post(String path, Map<String, String> post)
			throws IOException {
		URL url = new URL(host + ":" + port + path);

		if (post == null) {
			post = new HashMap<String, String>();
		}
		post.put("login", subkey);
		post.put("password", key);

		return Instance.getInstance().getCache().openNoCache(url, null, post,
				null, null);
	}
}
