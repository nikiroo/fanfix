package be.nikiroo.fanfix.library;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import org.json.JSONArray;
import org.json.JSONObject;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.JsonIO;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.web.WebLibraryServerIndex;
import be.nikiroo.fanfix.reader.TextOutput;
import be.nikiroo.utils.CookieUtils;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.NanoHTTPD;
import be.nikiroo.utils.NanoHTTPD.IHTTPSession;
import be.nikiroo.utils.NanoHTTPD.Response;
import be.nikiroo.utils.NanoHTTPD.Response.Status;
import be.nikiroo.utils.TraceHandler;
import be.nikiroo.utils.Version;

public class WebLibraryServer implements Runnable {
	static private String VIEWER_URL_BASE = "/view/story/";
	static private String VIEWER_URL = VIEWER_URL_BASE + "{luid}/{chap}/{para}";
	static private String STORY_URL_BASE = "/story/";
	static private String STORY_URL = STORY_URL_BASE + "{luid}/{chap}/{para}";
	static private String STORY_URL_COVER = STORY_URL_BASE + "{luid}/cover";
	static private String LIST_URL = "/list/";

	private class LoginResult {
		private boolean success;
		private boolean rw;
		private boolean wl;
		private String wookie;
		private String token;
		private boolean badLogin;
		private boolean badToken;

		public LoginResult(String who, String key, String subkey,
				boolean success, boolean rw, boolean wl) {
			this.success = success;
			this.rw = rw;
			this.wl = wl;
			this.wookie = CookieUtils.generateCookie(who + key, 0);

			String opts = "";
			if (rw)
				opts += "|rw";
			if (!wl)
				opts += "|wl";

			this.token = wookie + "~"
					+ CookieUtils.generateCookie(wookie + subkey + opts, 0)
					+ "~" + opts;
			this.badLogin = !success;
		}

		public LoginResult(String token, String who, String key,
				List<String> subkeys) {

			if (token != null) {
				String hashes[] = token.split("~");
				if (hashes.length >= 2) {
					String wookie = hashes[0];
					String rehashed = hashes[1];
					String opts = hashes.length > 2 ? hashes[2] : "";

					if (CookieUtils.validateCookie(who + key, wookie)) {
						if (subkeys == null) {
							subkeys = new ArrayList<String>();
						}
						subkeys = new ArrayList<String>(subkeys);
						subkeys.add("");

						for (String subkey : subkeys) {
							if (CookieUtils.validateCookie(
									wookie + subkey + opts, rehashed)) {
								this.wookie = wookie;
								this.token = token;
								this.success = true;

								this.rw = opts.contains("|rw");
								this.wl = !opts.contains("|wl");
							}
						}
					}
				}

				this.badToken = !success;
			}

			// No token -> no bad token
		}

		public boolean isSuccess() {
			return success;
		}

		public boolean isRw() {
			return rw;
		}

		public boolean isWl() {
			return wl;
		}

		public String getToken() {
			return token;
		}

		public boolean isBadLogin() {
			return badLogin;
		}

		public boolean isBadToken() {
			return badToken;
		}
	}

	private NanoHTTPD server;
	private Map<String, Story> storyCache = new HashMap<String, Story>();
	private LinkedList<String> storyCacheOrder = new LinkedList<String>();
	private long storyCacheSize = 0;
	private long maxStoryCacheSize;
	private TraceHandler tracer = new TraceHandler();

	public WebLibraryServer(boolean secure) throws IOException {
		Integer port = Instance.getInstance().getConfig()
				.getInteger(Config.SERVER_PORT);
		if (port == null) {
			throw new IOException(
					"Cannot start web server: port not specified");
		}

		int cacheMb = Instance.getInstance().getConfig()
				.getInteger(Config.SERVER_MAX_CACHE_MB, 100);
		maxStoryCacheSize = cacheMb * 1024 * 1024;

		setTraceHandler(Instance.getInstance().getTraceHandler());

		SSLServerSocketFactory ssf = null;
		if (secure) {
			String keystorePath = Instance.getInstance().getConfig()
					.getString(Config.SERVER_SSL_KEYSTORE, "");
			String keystorePass = Instance.getInstance().getConfig()
					.getString(Config.SERVER_SSL_KEYSTORE_PASS);

			if (secure && keystorePath.isEmpty()) {
				throw new IOException(
						"Cannot start a secure web server: no keystore.jks file povided");
			}

			if (!keystorePath.isEmpty()) {
				File keystoreFile = new File(keystorePath);
				try {
					KeyStore keystore = KeyStore
							.getInstance(KeyStore.getDefaultType());
					InputStream keystoreStream = new FileInputStream(
							keystoreFile);
					try {
						keystore.load(keystoreStream,
								keystorePass.toCharArray());
						KeyManagerFactory keyManagerFactory = KeyManagerFactory
								.getInstance(KeyManagerFactory
										.getDefaultAlgorithm());
						keyManagerFactory.init(keystore,
								keystorePass.toCharArray());
						ssf = NanoHTTPD.makeSSLSocketFactory(keystore,
								keyManagerFactory);
					} finally {
						keystoreStream.close();
					}
				} catch (Exception e) {
					throw new IOException(e.getMessage());
				}
			}
		}

		server = new NanoHTTPD(port) {
			@Override
			public Response serve(final IHTTPSession session) {
				super.serve(session);

				String query = session.getQueryParameterString(); // a=a%20b&dd=2
				Method method = session.getMethod(); // GET, POST..
				String uri = session.getUri(); // /home.html

				// need them in real time (not just those sent by the UA)
				Map<String, String> cookies = new HashMap<String, String>();
				for (String cookie : session.getCookies()) {
					cookies.put(cookie, session.getCookies().read(cookie));
				}

				List<String> whitelist = Instance.getInstance().getConfig()
						.getList(Config.SERVER_WHITELIST);
				if (whitelist == null) {
					whitelist = new ArrayList<String>();
				}

				LoginResult login = null;
				Map<String, String> params = session.getParms();
				String who = session.getRemoteHostName()
						+ session.getRemoteIpAddress();
				if (params.get("login") != null) {
					login = login(who, params.get("password"),
							params.get("login"), whitelist);
				} else {
					String token = cookies.get("token");
					login = login(who, token, Instance.getInstance().getConfig()
							.getList(Config.SERVER_ALLOWED_SUBKEYS));
				}

				if (login.isSuccess()) {
					// refresh token
					session.getCookies().set(new Cookie("token",
							login.getToken(), "30; path=/"));

					// set options
					String optionName = params.get("optionName");
					if (optionName != null && !optionName.isEmpty()) {
						String optionValue = params.get("optionValue");
						if (optionValue == null || optionValue.isEmpty()) {
							session.getCookies().delete(optionName);
							cookies.remove(optionName);
						} else {
							session.getCookies().set(new Cookie(optionName,
									optionValue, "; path=/"));
							cookies.put(optionName, optionValue);
						}
					}
				}

				Response rep = null;
				if (!login.isSuccess() && (uri.equals("/") //
						|| uri.startsWith(STORY_URL_BASE) //
						|| uri.startsWith(VIEWER_URL_BASE) //
						|| uri.startsWith(LIST_URL))) {
					rep = loginPage(login, uri);
				}

				if (rep == null) {
					try {
						if (uri.equals("/")) {
							rep = root(session, cookies, whitelist);
						} else if (uri.startsWith(LIST_URL)) {
							rep = getList(uri, whitelist);
						} else if (uri.startsWith(STORY_URL_BASE)) {
							rep = getStoryPart(uri, whitelist);
						} else if (uri.startsWith(VIEWER_URL_BASE)) {
							rep = getViewer(cookies, uri, whitelist);
						} else if (uri.equals("/logout")) {
							session.getCookies().delete("token");
							cookies.remove("token");
							rep = loginPage(login, uri);
						} else {
							if (uri.startsWith("/"))
								uri = uri.substring(1);
							InputStream in = IOUtils.openResource(
									WebLibraryServerIndex.class, uri);
							if (in != null) {
								String mimeType = MIME_PLAINTEXT;
								if (uri.endsWith(".css")) {
									mimeType = "text/css";
								} else if (uri.endsWith(".html")) {
									mimeType = "text/html";
								} else if (uri.endsWith(".js")) {
									mimeType = "text/javascript";
								}
								rep = newChunkedResponse(Status.OK, mimeType,
										in);
							} else {
								getTraceHandler().trace("404: " + uri);
							}
						}

						if (rep == null) {
							rep = newFixedLengthResponse(Status.NOT_FOUND,
									NanoHTTPD.MIME_PLAINTEXT, "Not Found");
						}
					} catch (Exception e) {
						Instance.getInstance().getTraceHandler().error(
								new IOException("Cannot process web request",
										e));
						rep = newFixedLengthResponse(Status.INTERNAL_ERROR,
								NanoHTTPD.MIME_PLAINTEXT, "An error occured");
					}
				}

				return rep;

				// Get status: for story, use "luid" + active map of current
				// luids
				// map must use a addRef/removeRef and delete at 0

				// http://localhost:2000/?token=ok

				//
				// MetaData meta = new MetaData();
				// meta.setTitle("Title");
				// meta.setLuid("000");
				//
				// JSONObject json = new JSONObject();
				// json.put("", MetaData.class.getName());
				// json.put("title", meta.getTitle());
				// json.put("luid", meta.getLuid());
				//
				// return newFixedLengthResponse(json.toString());
			}
		};

		if (ssf != null) {
			getTraceHandler().trace("Install SSL on the web server...");
			server.makeSecure(ssf, null);
			getTraceHandler().trace("Done.");
		}
	}

	@Override
	public void run() {
		try {
			server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
		} catch (IOException e) {
			tracer.error(new IOException("Cannot start the web server", e));
		}
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

	/**
	 * The traces handler for this {@link WebLibraryServer}.
	 * 
	 * @return the traces handler
	 */
	public TraceHandler getTraceHandler() {
		return tracer;
	}

	/**
	 * The traces handler for this {@link WebLibraryServer}.
	 * 
	 * @param tracer
	 *            the new traces handler
	 */
	public void setTraceHandler(TraceHandler tracer) {
		if (tracer == null) {
			tracer = new TraceHandler(false, false, false);
		}

		this.tracer = tracer;
	}

	private LoginResult login(String who, String token, List<String> subkeys) {
		String realKey = Instance.getInstance().getConfig()
				.getString(Config.SERVER_KEY);
		realKey = realKey == null ? "" : realKey;
		return new LoginResult(token, who, realKey, subkeys);
	}

	// allow rw/wl
	private LoginResult login(String who, String key, String subkey,
			List<String> whitelist) {
		String realKey = Instance.getInstance().getConfig()
				.getString(Config.SERVER_KEY);

		// I don't like NULLs...
		realKey = realKey == null ? "" : realKey;
		key = key == null ? "" : key;
		subkey = subkey == null ? "" : subkey;

		if (!realKey.equals(key)) {
			return new LoginResult(null, null, null, false, false, false);
		}

		// defaults are positive (as previous versions without the feature)
		boolean rw = true;
		boolean wl = true;

		if (whitelist.isEmpty()) {
			wl = false;
		}

		rw = Instance.getInstance().getConfig().getBoolean(Config.SERVER_RW,
				rw);
		if (!subkey.isEmpty()) {
			List<String> allowed = Instance.getInstance().getConfig()
					.getList(Config.SERVER_ALLOWED_SUBKEYS);
			if (allowed != null && allowed.contains(subkey)) {
				if ((subkey + "|").contains("|rw|")) {
					rw = true;
				}
				if ((subkey + "|").contains("|wl|")) {
					wl = false; // |wl| = bypass whitelist
				}
			} else {
				return new LoginResult(null, null, null, false, false, false);
			}
		}

		return new LoginResult(who, key, subkey, true, rw, wl);
	}

	private Response loginPage(LoginResult login, String uri) {
		StringBuilder builder = new StringBuilder();

		appendPreHtml(builder, true);

		if (login.isBadLogin()) {
			builder.append("<div class='error'>Bad login or password</div>");
		} else if (login.isBadToken()) {
			builder.append("<div class='error'>Your session timed out</div>");
		}

		if (uri.equals("/logout")) {
			uri = "/";
		}

		builder.append(
				"<form method='POST' action='" + uri + "' class='login'>\n");
		builder.append(
				"<p>You must be logged into the system to see the stories.</p>");
		builder.append("\t<input type='text' name='login' />\n");
		builder.append("\t<input type='password' name='password' />\n");
		builder.append("\t<input type='submit' value='Login' />\n");
		builder.append("</form>\n");

		appendPostHtml(builder);

		return NanoHTTPD.newFixedLengthResponse(Status.FORBIDDEN,
				NanoHTTPD.MIME_HTML, builder.toString());
	}

	protected Response getList(String uri, List<String> whitelist)
			throws IOException {
		if (uri.equals("/list/luids")) {
			BasicLibrary lib = Instance.getInstance().getLibrary();
			List<MetaData> metas = lib.getList().filter(whitelist, null, null);
			List<JSONObject> jsons = new ArrayList<JSONObject>();
			for (MetaData meta : metas) {
				jsons.add(JsonIO.toJson(meta));
			}

			return newInputStreamResponse("application/json",
					new ByteArrayInputStream(
							new JSONArray(jsons).toString().getBytes()));
		}

		return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
				NanoHTTPD.MIME_PLAINTEXT, null);
	}

	private Response root(IHTTPSession session, Map<String, String> cookies,
			List<String> whitelist) throws IOException {
		BasicLibrary lib = Instance.getInstance().getLibrary();
		MetaResultList result = lib.getList();
		result = new MetaResultList(result.filter(whitelist, null, null));
		StringBuilder builder = new StringBuilder();

		appendPreHtml(builder, true);

		String filter = cookies.get("filter");
		if (filter == null) {
			filter = "";
		}

		Map<String, String> params = session.getParms();
		String browser = params.get("browser") == null ? ""
				: params.get("browser");
		String browser2 = params.get("browser2") == null ? ""
				: params.get("browser2");
		String browser3 = params.get("browser3") == null ? ""
				: params.get("browser3");

		String filterSource = null;
		String filterAuthor = null;
		String filterTag = null;

		// TODO: javascript in realtime, using visible=false + hide [submit]

		builder.append("<form class='browser'>\n");
		builder.append("<div class='breadcrumbs'>\n");

		builder.append("\t<select name='browser'>");
		appendOption(builder, 2, "", "", browser);
		appendOption(builder, 2, "Sources", "sources", browser);
		appendOption(builder, 2, "Authors", "authors", browser);
		appendOption(builder, 2, "Tags", "tags", browser);
		builder.append("\t</select>\n");

		if (!browser.isEmpty()) {
			builder.append("\t<select name='browser2'>");
			if (browser.equals("sources")) {
				filterSource = browser2.isEmpty() ? filterSource : browser2;
				// TODO: if 1 group -> no group
				appendOption(builder, 2, "", "", browser2);
				Map<String, List<String>> sources = result.getSourcesGrouped();
				for (String source : sources.keySet()) {
					appendOption(builder, 2, source, source, browser2);
				}
			} else if (browser.equals("authors")) {
				filterAuthor = browser2.isEmpty() ? filterAuthor : browser2;
				// TODO: if 1 group -> no group
				appendOption(builder, 2, "", "", browser2);
				Map<String, List<String>> authors = result.getAuthorsGrouped();
				for (String author : authors.keySet()) {
					appendOption(builder, 2, author, author, browser2);
				}
			} else if (browser.equals("tags")) {
				filterTag = browser2.isEmpty() ? filterTag : browser2;
				appendOption(builder, 2, "", "", browser2);
				for (String tag : result.getTags()) {
					appendOption(builder, 2, tag, tag, browser2);
				}
			}
			builder.append("\t</select>\n");
		}

		if (!browser2.isEmpty()) {
			if (browser.equals("sources")) {
				filterSource = browser3.isEmpty() ? filterSource : browser3;
				Map<String, List<String>> sourcesGrouped = result
						.getSourcesGrouped();
				List<String> sources = sourcesGrouped.get(browser2);
				if (sources != null && !sources.isEmpty()) {
					// TODO: single empty value
					builder.append("\t<select name='browser3'>");
					appendOption(builder, 2, "", "", browser3);
					for (String source : sources) {
						appendOption(builder, 2, source, source, browser3);
					}
					builder.append("\t</select>\n");
				}
			} else if (browser.equals("authors")) {
				filterAuthor = browser3.isEmpty() ? filterAuthor : browser3;
				Map<String, List<String>> authorsGrouped = result
						.getAuthorsGrouped();
				List<String> authors = authorsGrouped.get(browser2);
				if (authors != null && !authors.isEmpty()) {
					// TODO: single empty value
					builder.append("\t<select name='browser3'>");
					appendOption(builder, 2, "", "", browser3);
					for (String author : authors) {
						appendOption(builder, 2, author, author, browser3);
					}
					builder.append("\t</select>\n");
				}
			}
		}

		builder.append("\t<input type='submit' value='Select'/>\n");
		builder.append("</div>\n");

		// TODO: javascript in realtime, using visible=false + hide [submit]
		builder.append("<div class='filter'>\n");
		builder.append("\tFilter: \n");
		builder.append(
				"\t<input name='optionName'  type='hidden' value='filter' />\n");
		builder.append("\t<input name='optionValue' type='text'   value='"
				+ filter + "' place-holder='...' />\n");
		builder.append(
				"\t<input name='submit' type='submit' value='Filter' />\n");
		builder.append("</div>\n");
		builder.append("</form>\n");

		builder.append("\t<div class='books'>");
		for (MetaData meta : result.getMetas()) {
			if (!filter.isEmpty() && !meta.getTitle().toLowerCase()
					.contains(filter.toLowerCase())) {
				continue;
			}

			// TODO Sub sources
			if (filterSource != null
					&& !filterSource.equals(meta.getSource())) {
				continue;
			}

			// TODO: sub authors
			if (filterAuthor != null
					&& !filterAuthor.equals(meta.getAuthor())) {
				continue;
			}

			if (filterTag != null && !meta.getTags().contains(filterTag)) {
				continue;
			}

			builder.append("<div class='book_line'>");
			builder.append("<a href='");
			builder.append(getViewUrl(meta.getLuid(), 0, null));
			builder.append("'");
			builder.append(" class='link'>");

			if (lib.isCached(meta.getLuid())) {
				// ◉ = &#9673;
				builder.append(
						"<span class='cache_icon cached'>&#9673;</span>");
			} else {
				// ○ = &#9675;
				builder.append(
						"<span class='cache_icon uncached'>&#9675;</span>");
			}
			builder.append("<span class='luid'>");
			builder.append(meta.getLuid());
			builder.append("</span>");
			builder.append("<span class='title'>");
			builder.append(meta.getTitle());
			builder.append("</span>");
			builder.append("<span class='author'>");
			if (meta.getAuthor() != null && !meta.getAuthor().isEmpty()) {
				builder.append("(").append(meta.getAuthor()).append(")");
			}
			builder.append("</span>");
			builder.append("</a></div>\n");
		}
		builder.append("</div>");

		appendPostHtml(builder);
		return NanoHTTPD.newFixedLengthResponse(builder.toString());
	}

	// /story/luid/chapter/para <-- text/image
	// /story/luid/cover <-- image
	// /story/luid/metadata <-- json
	private Response getStoryPart(String uri, List<String> whitelist) {
		String[] cover = uri.split("/");
		int off = 2;

		if (cover.length < off + 2) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT, null);
		}

		String luid = cover[off + 0];
		String chapterStr = cover[off + 1];
		String imageStr = cover.length < off + 3 ? null : cover[off + 2];

		// 1-based (0 = desc)
		int chapter = 0;
		if (chapterStr != null && !"cover".equals(chapterStr)
				&& !"metadata".equals(chapterStr)) {
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
				Image img = getCover(luid, whitelist);
				if (img != null) {
					in = img.newInputStream();
				}
			} else if ("metadata".equals(chapterStr)) {
				MetaData meta = meta(luid, whitelist);
				JSONObject json = JsonIO.toJson(meta);
				mimeType = "application/json";
				in = new ByteArrayInputStream(json.toString().getBytes());
			} else {
				Story story = story(luid, whitelist);
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

	private Response getViewer(Map<String, String> cookies, String uri,
			List<String> whitelist) {
		String[] cover = uri.split("/");
		int off = 2;

		if (cover.length < off + 2) {
			return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
					NanoHTTPD.MIME_PLAINTEXT, null);
		}

		String type = cover[off + 0];
		String luid = cover[off + 1];
		String chapterStr = cover.length < off + 3 ? null : cover[off + 2];
		String paragraphStr = cover.length < off + 4 ? null : cover[off + 3];

		// 1-based (0 = desc)
		int chapter = -1;
		if (chapterStr != null) {
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
		int paragraph = 0;
		if (paragraphStr != null) {
			try {
				paragraph = Integer.parseInt(paragraphStr);
				if (paragraph <= 0) {
					throw new NumberFormatException();
				}
			} catch (NumberFormatException e) {
				return NanoHTTPD.newFixedLengthResponse(Status.BAD_REQUEST,
						NanoHTTPD.MIME_PLAINTEXT, "Paragraph is not valid");
			}
		}

		try {
			Story story = story(luid, whitelist);
			if (story == null) {
				return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND,
						NanoHTTPD.MIME_PLAINTEXT, "Story not found");
			}

			StringBuilder builder = new StringBuilder();
			appendPreHtml(builder, false);

			if (chapter < 0) {
				builder.append(story);
			} else {
				if (chapter == 0) {
					// TODO: description
					chapter = 1;
				}

				Chapter chap = null;
				try {
					chap = story.getChapters().get(chapter - 1);
				} catch (IndexOutOfBoundsException e) {
					return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND,
							NanoHTTPD.MIME_PLAINTEXT, "Chapter not found");
				}

				if (story.getMeta().isImageDocument() && paragraph <= 0) {
					paragraph = 1;
				}

				String first, previous, next, last;
				String content;

				if (paragraph <= 0) {
					first = getViewUrl(luid, 1, null);
					previous = getViewUrl(luid, (Math.max(chapter - 1, 1)),
							null);
					next = getViewUrl(luid,
							(Math.min(chapter + 1, story.getChapters().size())),
							null);
					last = getViewUrl(luid, story.getChapters().size(), null);

					content = "<div class='viewer text'>\n"
							+ new TextOutput(false).convert(chap, true)
							+ "</div>\n";
				} else {
					first = getViewUrl(luid, chapter, 1);
					previous = getViewUrl(luid, chapter,
							(Math.max(paragraph - 1, 1)));
					next = getViewUrl(luid, chapter, (Math.min(paragraph + 1,
							chap.getParagraphs().size())));
					last = getViewUrl(luid, chapter,
							chap.getParagraphs().size());

					Paragraph para = null;
					try {
						para = chap.getParagraphs().get(paragraph - 1);
					} catch (IndexOutOfBoundsException e) {
						return NanoHTTPD.newFixedLengthResponse(
								Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT,
								"Paragraph not found");
					}

					if (para.getType() == ParagraphType.IMAGE) {
						String zoomStyle = "max-width: 100%;";
						String zoomOption = cookies.get("zoom");
						if (zoomOption != null && !zoomOption.isEmpty()) {
							if (zoomOption.equals("real")) {
								zoomStyle = "";
							} else if (zoomOption.equals("width")) {
								zoomStyle = "max-width: 100%;";
							} else if (zoomOption.equals("height")) {
								// see height of navbar + optionbar
								zoomStyle = "max-height: calc(100% - 128px);";
							}
						}
						content = String.format("" //
								+ "<a class='viewer link' href='%s'>" //
								+ "<img class='viewer img' style='%s' src='%s'/>"
								+ "</a>", //
								next, //
								zoomStyle, //
								getStoryUrl(luid, chapter, paragraph));
					} else {
						content = para.getContent();
					}

				}

				builder.append(String.format("" //
						+ "<div class='bar navbar'>\n" //
						+ "\t<a class='button first' href='%s'>&lt;&lt;</a>\n"//
						+ "\t<a class='button previous' href='%s'>&lt;</a>\n"//
						+ "\t<a class='button next' href='%s'>&gt;</a>\n"//
						+ "\t<a class='button last' href='%s'>&gt;&gt;</a>\n"//
						+ "</div>\n" //
						+ "%s", //
						first, //
						previous, //
						next, //
						last, //
						content //
				));

				builder.append("<div class='bar optionbar ");
				if (paragraph > 0) {
					builder.append("s4");
				} else {
					builder.append("s1");
				}
				builder.append("'>\n");
				builder.append(
						"	<a class='button back' href='/'>BACK</a>\n");

				if (paragraph > 0) {
					builder.append(String.format("" //
							+ "\t<a class='button zoomreal'   href='%s'>REAL</a>\n"//
							+ "\t<a class='button zoomwidth'  href='%s'>WIDTH</a>\n"//
							+ "\t<a class='button zoomheight' href='%s'>HEIGHT</a>\n"//
							+ "</div>\n", //
							uri + "?optionName=zoom&optionValue=real", //
							uri + "?optionName=zoom&optionValue=width", //
							uri + "?optionName=zoom&optionValue=height" //
					));
				}
			}

			appendPostHtml(builder);
			return NanoHTTPD.newFixedLengthResponse(Status.OK,
					NanoHTTPD.MIME_HTML, builder.toString());
		} catch (IOException e) {
			Instance.getInstance().getTraceHandler()
					.error(new IOException("Cannot get image: " + uri, e));
			return NanoHTTPD.newFixedLengthResponse(Status.INTERNAL_ERROR,
					NanoHTTPD.MIME_PLAINTEXT, "Error when processing request");
		}
	}

	private Response newInputStreamResponse(String mimeType, InputStream in) {
		if (in == null) {
			return NanoHTTPD.newFixedLengthResponse(Status.NO_CONTENT, "",
					null);
		}
		return NanoHTTPD.newChunkedResponse(Status.OK, mimeType, in);
	}

	private String getContentOf(String file) {
		InputStream in = IOUtils.openResource(WebLibraryServerIndex.class,
				file);
		if (in != null) {
			try {
				return IOUtils.readSmallStream(in);
			} catch (IOException e) {
				Instance.getInstance().getTraceHandler().error(
						new IOException("Cannot get file: index.pre.html", e));
			}
		}

		return "";
	}

	private String getViewUrl(String luid, int chap, Integer para) {
		return VIEWER_URL //
				.replace("{luid}", luid) //
				.replace("{chap}", Integer.toString(chap)) //
				.replace("/{para}",
						para == null ? "" : "/" + Integer.toString(para));
	}

	private String getStoryUrl(String luid, int chap, Integer para) {
		return STORY_URL //
				.replace("{luid}", luid) //
				.replace("{chap}", Integer.toString(chap)) //
				.replace("{para}", para == null ? "" : Integer.toString(para));
	}

	private String getStoryUrlCover(String luid) {
		return STORY_URL_COVER //
				.replace("{luid}", luid);
	}

	private MetaData meta(String luid, List<String> whitelist)
			throws IOException {
		BasicLibrary lib = Instance.getInstance().getLibrary();
		MetaData meta = lib.getInfo(luid);
		if (!whitelist.isEmpty() && !whitelist.contains(meta.getSource())) {
			return null;
		}

		return meta;
	}

	private Image getCover(String luid, List<String> whitelist)
			throws IOException {
		MetaData meta = meta(luid, whitelist);
		if (meta != null) {
			return meta.getCover();
		}

		return null;
	}

	// NULL if not whitelist OK or if not found
	private Story story(String luid, List<String> whitelist)
			throws IOException {
		synchronized (storyCache) {
			if (storyCache.containsKey(luid)) {
				Story story = storyCache.get(luid);
				if (!whitelist.isEmpty()
						&& !whitelist.contains(story.getMeta().getSource())) {
					return null;
				}

				return story;
			}
		}

		Story story = null;
		MetaData meta = meta(luid, whitelist);
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

	private void appendPreHtml(StringBuilder builder, boolean banner) {
		String favicon = "favicon.ico";
		String icon = Instance.getInstance().getUiConfig()
				.getString(UiConfig.PROGRAM_ICON);
		if (icon != null) {
			favicon = "icon_" + icon.replace("-", "_") + ".png";
		}

		builder.append(
				getContentOf("index.pre.html").replace("favicon.ico", favicon));

		if (banner) {
			builder.append("<div class='banner'>\n");
			builder.append("\t<img class='ico' src='") //
					.append(favicon) //
					.append("'/>\n");
			builder.append("\t<h1>Fanfix</h1>\n");
			builder.append("\t<h2>") //
					.append(Version.getCurrentVersion()) //
					.append("</h2>\n");
			builder.append("</div>\n");
		}
	}

	private void appendPostHtml(StringBuilder builder) {
		builder.append(getContentOf("index.post.html"));
	}

	private void appendOption(StringBuilder builder, int depth, String name,
			String value, String selected) {
		for (int i = 0; i < depth; i++) {
			builder.append("\t");
		}
		builder.append("<option value='").append(value).append("'");
		if (value.equals(selected)) {
			builder.append(" selected='selected'");
		}
		builder.append(">").append(name).append("</option>\n");
	}
}
