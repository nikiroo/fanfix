package be.nikiroo.fanfix.library;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.JsonIO;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.LoginResult;
import be.nikiroo.utils.NanoHTTPD;
import be.nikiroo.utils.NanoHTTPD.Response;
import be.nikiroo.utils.NanoHTTPD.Response.Status;
import be.nikiroo.utils.Progress;

public class WebLibraryServer extends WebLibraryServerHtml {
	class WLoginResult extends LoginResult {
		public WLoginResult(boolean badLogin, boolean badCookie) {
			super(badLogin, badCookie);
		}

		public WLoginResult(String who, String key, String subkey, boolean rw,
				boolean wl, boolean bl) {
			super(who, key, subkey, (rw ? "|rw" : "") + (wl ? "|wl" : "")
					+ (bl ? "|bl" : "") + "|");
		}

		public WLoginResult(String cookie, String who, String key,
				List<String> subkeys) {
			super(cookie, who, key, subkeys,
					subkeys == null || subkeys.isEmpty());
		}

		public boolean isRw() {
			return getOption().contains("|rw|");
		}

		public boolean isWl() {
			return getOption().contains("|wl|");
		}

		public boolean isBl() {
			return getOption().contains("|bl|");
		}
	}

	private Map<String, Story> storyCache = new HashMap<String, Story>();
	private LinkedList<String> storyCacheOrder = new LinkedList<String>();
	private long storyCacheSize = 0;
	private long maxStoryCacheSize;

	private List<String> whitelist;
	private List<String> blacklist;

	private Map<String, Progress> imprts = new HashMap<String, Progress>();

	public WebLibraryServer(boolean secure) throws IOException {
		super(secure);

		int cacheMb = Instance.getInstance().getConfig()
				.getInteger(Config.SERVER_MAX_CACHE_MB, 100);
		maxStoryCacheSize = cacheMb * 1024 * 1024;

		setTraceHandler(Instance.getInstance().getTraceHandler());

		whitelist = Instance.getInstance().getConfig()
				.getList(Config.SERVER_WHITELIST, new ArrayList<String>());
		blacklist = Instance.getInstance().getConfig()
				.getList(Config.SERVER_BLACKLIST, new ArrayList<String>());
	}

	/**
	 * Start the server (listen on the network for new connections).
	 * <p>
	 * Can only be called once.
	 * <p>
	 * This call is asynchronous, and will just start a new {@link Thread} on
	 * itself (see {@link WebLibraryServer#run()}).
	 */
	public void start() {
		new Thread(this).start();
	}

	@Override
	protected WLoginResult login(boolean badLogin, boolean badCookie) {
		return new WLoginResult(false, false);
	}

	@Override
	protected WLoginResult login(String who, String cookie) {
		List<String> subkeys = Instance.getInstance().getConfig()
				.getList(Config.SERVER_ALLOWED_SUBKEYS);
		String realKey = Instance.getInstance().getConfig()
				.getString(Config.SERVER_KEY);

		return new WLoginResult(cookie, who, realKey, subkeys);
	}

	// allow rw/wl
	@Override
	protected WLoginResult login(String who, String key, String subkey) {
		String realKey = Instance.getInstance().getConfig()
				.getString(Config.SERVER_KEY, "");

		// I don't like NULLs...
		key = key == null ? "" : key;
		subkey = subkey == null ? "" : subkey;

		if (!realKey.equals(key)) {
			return new WLoginResult(true, false);
		}

		// defaults are true (as previous versions without the feature)
		boolean rw = true;
		boolean wl = true;
		boolean bl = true;

		rw = Instance.getInstance().getConfig().getBoolean(Config.SERVER_RW,
				rw);

		List<String> allowed = Instance.getInstance().getConfig().getList(
				Config.SERVER_ALLOWED_SUBKEYS, new ArrayList<String>());

		if (!allowed.isEmpty()) {
			if (!allowed.contains(subkey)) {
				return new WLoginResult(true, false);
			}

			if ((subkey + "|").contains("|rw|")) {
				rw = true;
			}
			if ((subkey + "|").contains("|wl|")) {
				wl = false; // |wl| = bypass whitelist
			}
			if ((subkey + "|").contains("|bl|")) {
				bl = false; // |bl| = bypass blacklist
			}
		}

		return new WLoginResult(who, key, subkey, rw, wl, bl);
	}

	@Override
	protected Response getList(String uri, WLoginResult login)
			throws IOException {
		if (WebLibraryUrls.LIST_URL_METADATA.equals(uri)) {
			List<JSONObject> jsons = new ArrayList<JSONObject>();
			for (MetaData meta : metas(login)) {
				jsons.add(JsonIO.toJson(meta));
			}

			return newInputStreamResponse("application/json",
					new ByteArrayInputStream(
							new JSONArray(jsons).toString().getBytes()));
		}

		return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
				NanoHTTPD.MIME_PLAINTEXT, null);
	}

	// /story/luid/chapter/para <-- text/image
	// /story/luid/cover <-- image
	// /story/luid/metadata <-- json
	// /story/luid/json <-- json, whole chapter (no images)
	@Override
	protected Response getStoryPart(String uri, WLoginResult login) {
		String[] uriParts = uri.split("/");
		int off = 2;

		if (uriParts.length < off + 2) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT, null);
		}

		String luid = uriParts[off + 0];
		String chapterStr = uriParts[off + 1];
		String imageStr = uriParts.length < off + 3 ? null : uriParts[off + 2];

		// 1-based (0 = desc)
		int chapter = 0;
		if (chapterStr != null && !"cover".equals(chapterStr)
				&& !"metadata".equals(chapterStr)
				&& !"json".equals(chapterStr)) {
			try {
				chapter = Integer.parseInt(chapterStr);
				if (chapter < 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
						NanoHTTPD.MIME_PLAINTEXT, "Chapter is not valid");
			}
		}

		// 1-based
		int paragraph = 1;
		if (imageStr != null) {
			try {
				paragraph = Integer.parseInt(imageStr);
				if (paragraph < 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
						NanoHTTPD.MIME_PLAINTEXT, "Paragraph is not valid");
			}
		}

		String mimeType = NanoHTTPD.MIME_PLAINTEXT;
		InputStream in = null;
		try {
			if ("cover".equals(chapterStr)) {
				Image img = storyCover(luid, login);
				if (img != null) {
					in = img.newInputStream();
				}
				// TODO: get correct image type
				mimeType = "image/png";
			} else if ("metadata".equals(chapterStr)) {
				MetaData meta = meta(luid, login);
				JSONObject json = JsonIO.toJson(meta);
				mimeType = "application/json";
				in = new ByteArrayInputStream(json.toString().getBytes());
			} else if ("json".equals(chapterStr)) {
				Story story = story(luid, login);
				JSONObject json = JsonIO.toJson(story);
				mimeType = "application/json";
				in = new ByteArrayInputStream(json.toString().getBytes());
			} else {
				Story story = story(luid, login);
				if (story != null) {
					if (chapter == 0) {
						StringBuilder builder = new StringBuilder();
						for (Paragraph p : story.getMeta().getResume()) {
							if (builder.length() == 0) {
								builder.append("\n");
							}
							builder.append(p.getContent());
						}

						in = new ByteArrayInputStream(
								builder.toString().getBytes("utf-8"));
					} else {
						Paragraph para = story.getChapters().get(chapter - 1)
								.getParagraphs().get(paragraph - 1);
						Image img = para.getContentImage();
						if (para.getType() == ParagraphType.IMAGE) {
							// TODO: get correct image type
							mimeType = "image/png";
							in = img.newInputStream();
						} else {
							in = new ByteArrayInputStream(
									para.getContent().getBytes("utf-8"));
						}
					}
				}
			}
		} catch (IndexOutOfBoundsException e) {
			return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND,
					NanoHTTPD.MIME_PLAINTEXT,
					"Chapter or paragraph does not exist");
		} catch (IOException e) {
			Instance.getInstance().getTraceHandler()
					.error(new IOException("Cannot get image: " + uri, e));
			return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR,
					NanoHTTPD.MIME_PLAINTEXT, "Error when processing request");
		}

		return newInputStreamResponse(mimeType, in);
	}

	// /story/luid/source
	// /story/luid/title
	// /story/luid/author
	@Override
	protected Response setStoryPart(String uri, String value,
			WLoginResult login) throws IOException {
		String[] uriParts = uri.split("/");
		int off = 2; // "" and "story"

		if (uriParts.length < off + 2) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT, "Invalid story part request");
		}

		if (!login.isRw()) {
			return NanoHTTPD.newFixedLengthResponse(Status.FORBIDDEN,
					NanoHTTPD.MIME_PLAINTEXT, "SET story part not allowed");
		}

		String luid = uriParts[off + 0];
		String type = uriParts[off + 1];

		if (!Arrays.asList("source", "title", "author").contains(type)) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT,
					"Invalid SET story part: " + type);
		}

		if (meta(luid, login) != null) {
			BasicLibrary lib = Instance.getInstance().getLibrary();
			if ("source".equals(type)) {
				lib.changeSource(luid, value, null);
			} else if ("title".equals(type)) {
				lib.changeTitle(luid, value, null);
			} else if ("author".equals(type)) {
				lib.changeAuthor(luid, value, null);
			}
		}

		return newInputStreamResponse(NanoHTTPD.MIME_PLAINTEXT, null);
	}

	@Override
	protected Response getCover(String uri, WLoginResult login)
			throws IOException {
		String[] uriParts = uri.split("/");
		int off = 2; // "" and "cover"

		if (uriParts.length < off + 2) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT, "Invalid cover request");
		}

		String type = uriParts[off + 0];
		String id = uriParts[off + 1];

		InputStream in = null;

		if ("story".equals(type)) {
			Image img = storyCover(id, login);
			if (img != null) {
				in = img.newInputStream();
			}
		} else if ("source".equals(type)) {
			Image img = sourceCover(id, login);
			if (img != null) {
				in = img.newInputStream();
			}
		} else if ("author".equals(type)) {
			Image img = authorCover(id, login);
			if (img != null) {
				in = img.newInputStream();
			}
		} else {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT,
					"Invalid GET cover type: " + type);
		}

		// TODO: get correct image type
		return newInputStreamResponse("image/png", in);
	}

	@Override
	protected Response setCover(String uri, String luid, WLoginResult login)
			throws IOException {
		String[] uriParts = uri.split("/");
		int off = 2; // "" and "cover"

		if (uriParts.length < off + 2) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT, "Invalid cover request");
		}

		if (!login.isRw()) {
			return NanoHTTPD.newFixedLengthResponse(Status.FORBIDDEN,
					NanoHTTPD.MIME_PLAINTEXT, "Cover request not allowed");
		}

		String type = uriParts[off + 0];
		String id = uriParts[off + 1];

		if ("source".equals(type)) {
			sourceCover(id, login, luid);
		} else if ("author".equals(type)) {
			authorCover(id, login, luid);
		} else {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT,
					"Invalid SET cover type: " + type);
		}

		return newInputStreamResponse(NanoHTTPD.MIME_PLAINTEXT, null);
	}

	@Override
	protected Response imprt(String uri, String urlStr, WLoginResult login)
			throws IOException {
		final BasicLibrary lib = Instance.getInstance().getLibrary();

		if (!login.isRw()) {
			return NanoHTTPD.newFixedLengthResponse(Status.FORBIDDEN,
					NanoHTTPD.MIME_PLAINTEXT, "Import not allowed");
		}

		final URL url = new URL(urlStr);
		final Progress pg = new Progress();
		final String luid = lib.getNextId();

		synchronized (imprts) {
			imprts.put(luid, pg);
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					lib.imprt(url, pg);
				} catch (IOException e) {
					Instance.getInstance().getTraceHandler().error(e);
				} finally {
					synchronized (imprts) {
						imprts.remove(luid);
					}
				}
			}
		}, "Import story: " + urlStr).start();

		return NanoHTTPD.newFixedLengthResponse(Status.OK,
				NanoHTTPD.MIME_PLAINTEXT, luid);
	}

	@Override
	protected Response imprtProgress(String uri, WLoginResult login) {
		String[] uriParts = uri.split("/");
		int off = 2; // "" and "import"

		if (uriParts.length < off + 1) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT, "Invalid cover request");
		}

		String luid = uriParts[off + 0];

		Progress pg = null;
		synchronized (imprts) {
			pg = imprts.get(luid);
		}
		if (pg != null) {
			return NanoHTTPD.newFixedLengthResponse(Status.OK,
					"application/json", JsonIO.toJson(pg).toString());
		}

		return newInputStreamResponse(NanoHTTPD.MIME_PLAINTEXT, null);
	}

	@Override
	protected Response delete(String uri, WLoginResult login)
			throws IOException {
		String[] uriParts = uri.split("/");
		int off = 2; // "" and "delete"

		if (uriParts.length < off + 1) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT, "Invalid delete request");
		}

		if (!login.isRw()) {
			return NanoHTTPD.newFixedLengthResponse(Status.FORBIDDEN,
					NanoHTTPD.MIME_PLAINTEXT, "Delete not allowed");
		}

		String luid = uriParts[off + 0];

		BasicLibrary lib = Instance.getInstance().getLibrary();
		lib.delete(luid);

		return newInputStreamResponse(NanoHTTPD.MIME_PLAINTEXT, null);
	}

	@Override
	protected List<MetaData> metas(WLoginResult login) throws IOException {
		BasicLibrary lib = Instance.getInstance().getLibrary();
		List<MetaData> metas = new ArrayList<MetaData>();
		for (MetaData meta : lib.getList().getMetas()) {
			if (isAllowed(meta, login)) {
				metas.add(meta);
			}
		}

		return metas;
	}

	// NULL if not whitelist OK or if not found
	@Override
	protected Story story(String luid, WLoginResult login) throws IOException {
		synchronized (storyCache) {
			if (storyCache.containsKey(luid)) {
				Story story = storyCache.get(luid);
				if (!isAllowed(story.getMeta(), login))
					return null;

				return story;
			}
		}

		Story story = null;
		MetaData meta = meta(luid, login);
		if (meta != null) {
			BasicLibrary lib = Instance.getInstance().getLibrary();
			story = lib.getStory(luid, null);
			long size = sizeOf(story);

			synchronized (storyCache) {
				// Could have been added by another request
				if (!storyCache.containsKey(luid)) {
					while (!storyCacheOrder.isEmpty()
							&& storyCacheSize + size > maxStoryCacheSize) {
						String oldestLuid = storyCacheOrder.removeFirst();
						Story oldestStory = storyCache.remove(oldestLuid);
						maxStoryCacheSize -= sizeOf(oldestStory);
					}

					storyCacheOrder.add(luid);
					storyCache.put(luid, story);
				}
			}
		}

		return story;
	}

	private MetaData meta(String luid, WLoginResult login) throws IOException {
		BasicLibrary lib = Instance.getInstance().getLibrary();
		MetaData meta = lib.getInfo(luid);
		if (!isAllowed(meta, login))
			return null;

		return meta;
	}

	private Image storyCover(String luid, WLoginResult login)
			throws IOException {
		MetaData meta = meta(luid, login);
		if (meta != null) {
			BasicLibrary lib = Instance.getInstance().getLibrary();
			return lib.getCover(meta.getLuid());
		}

		return null;
	}

	private Image authorCover(String author, WLoginResult login)
			throws IOException {
		Image img = null;

		List<MetaData> metas = new MetaResultList(metas(login)).filter(null,
				author, null);
		if (metas.size() > 0) {
			BasicLibrary lib = Instance.getInstance().getLibrary();
			img = lib.getCustomAuthorCover(author);
			if (img == null)
				img = lib.getCover(metas.get(0).getLuid());
		}

		return img;

	}

	private void authorCover(String author, WLoginResult login, String luid)
			throws IOException {
		if (meta(luid, login) != null) {
			List<MetaData> metas = new MetaResultList(metas(login)).filter(null,
					author, null);
			if (metas.size() > 0) {
				BasicLibrary lib = Instance.getInstance().getLibrary();
				lib.setAuthorCover(author, luid);
			}
		}
	}

	private Image sourceCover(String source, WLoginResult login)
			throws IOException {
		Image img = null;

		List<MetaData> metas = new MetaResultList(metas(login)).filter(source,
				null, null);
		if (metas.size() > 0) {
			BasicLibrary lib = Instance.getInstance().getLibrary();
			img = lib.getCustomSourceCover(source);
			if (img == null)
				img = lib.getCover(metas.get(0).getLuid());
		}

		return img;
	}

	private void sourceCover(String source, WLoginResult login, String luid)
			throws IOException {
		if (meta(luid, login) != null) {
			List<MetaData> metas = new MetaResultList(metas(login))
					.filter(source, null, null);
			if (metas.size() > 0) {
				BasicLibrary lib = Instance.getInstance().getLibrary();
				lib.setSourceCover(source, luid);
			}
		}
	}

	private boolean isAllowed(MetaData meta, WLoginResult login) {
		MetaResultList one = new MetaResultList(Arrays.asList(meta));
		if (login.isWl() && !whitelist.isEmpty()) {
			if (one.filter(whitelist, null, null).isEmpty()) {
				return false;
			}
		}
		if (login.isBl() && !blacklist.isEmpty()) {
			if (!one.filter(blacklist, null, null).isEmpty()) {
				return false;
			}
		}

		return true;
	}

	private long sizeOf(Story story) {
		long size = 0;
		for (Chapter chap : story) {
			for (Paragraph para : chap) {
				if (para.getType() == ParagraphType.IMAGE) {
					size += para.getContentImage().getSize();
				} else {
					size += para.getContent().length();
				}
			}
		}

		return size;
	}

	public static void main(String[] args) throws IOException {
		Instance.init();
		WebLibraryServer web = new WebLibraryServer(false);
		web.run();
	}
}
