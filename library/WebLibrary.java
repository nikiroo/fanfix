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

		// TODO: not supported yet
		this.rw = false;
	}

	@Override
	public Status getStatus() {
		try {
			download("/");
		} catch (IOException e) {
			try {
				download("/style.css");
				return Status.UNAUTHORIZED;
			} catch (IOException ioe) {
				return Status.INVALID;
			}
		}

		return rw ? Status.READ_WRITE : Status.READ_ONLY;
	}

	@Override
	public String getLibraryName() {
		return (rw ? "[READ-ONLY] " : "") + host + ":" + port;
	}

	@Override
	public Image getCover(String luid) throws IOException {
		InputStream in = download("/story/" + luid + "/cover");
		if (in != null) {
			return new Image(in);
		}

		return null;
	}

	@Override
	public Image getCustomSourceCover(final String source) throws IOException {
		// TODO maybe global system in BasicLib ?
		return null;
	}

	@Override
	public Image getCustomAuthorCover(final String author) throws IOException {
		// TODO maybe global system in BasicLib ?
		return null;
	}

	@Override
	public void setSourceCover(String source, String luid) throws IOException {
		// TODO Auto-generated method stub
		throw new IOException("Not implemented yet");
	}

	@Override
	public void setAuthorCover(String author, String luid) throws IOException {
		// TODO Auto-generated method stub
		throw new IOException("Not implemented yet");
	}

	@Override
	public synchronized Story getStory(final String luid, Progress pg)
			throws IOException {

		// TODO: pg

		Story story;
		InputStream in = download("/story/" + luid + "/json");
		try {
			JSONObject json = new JSONObject(IOUtils.readSmallStream(in));
			story = JsonIO.toStory(json);
		} finally {
			in.close();
		}

		story.getMeta().setCover(getCover(luid));
		int chapNum = 1;
		for (Chapter chap : story) {
			int number = 1;
			for (Paragraph para : chap) {
				if (para.getType() == ParagraphType.IMAGE) {
					InputStream subin = download(
							"/story/" + luid + "/" + chapNum + "/" + number);
					try {
						para.setContentImage(new Image(subin));
					} finally {
						subin.close();
					}
				}

				number++;
			}

			chapNum++;
		}

		return story;
	}

	@Override
	protected List<MetaData> getMetas(Progress pg) throws IOException {
		List<MetaData> metas = new ArrayList<MetaData>();
		InputStream in = download("/list/luids");
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
		if (true)
			throw new IOException("Not implemented yet");

		// Import the file locally if it is actually a file

		if (url == null || url.getProtocol().equalsIgnoreCase("file")) {
			return super.imprt(url, pg);
		}

		// Import it remotely if it is an URL

		// TODO
		return super.imprt(url, pg);
	}

	@Override
	// Could work (more slowly) without it
	protected synchronized void changeSTA(final String luid,
			final String newSource, final String newTitle,
			final String newAuthor, Progress pg) throws IOException {
		// TODO
		super.changeSTA(luid, newSource, newTitle, newAuthor, pg);
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

	@Override
	public File getFile(final String luid, Progress pg) {
		throw new java.lang.InternalError(
				"Operation not supportorted on remote Libraries");
	}

	// starts with "/"
	private InputStream download(String path) throws IOException {
		URL url = new URL(host + ":" + port + path);

		Map<String, String> post = new HashMap<String, String>();
		post.put("login", subkey);
		post.put("password", key);

		return Instance.getInstance().getCache().openNoCache(url, null, post,
				null, null);
	}
}
