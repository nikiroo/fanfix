package be.nikiroo.fanfix.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.WebLibraryServer.WLoginResult;
import be.nikiroo.fanfix.library.web.WebLibraryServerIndex;
import be.nikiroo.fanfix.reader.TextOutput;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.NanoHTTPD;
import be.nikiroo.utils.NanoHTTPD.IHTTPSession;
import be.nikiroo.utils.NanoHTTPD.Response;
import be.nikiroo.utils.NanoHTTPD.Response.Status;
import be.nikiroo.utils.TraceHandler;
import be.nikiroo.utils.Version;

abstract class WebLibraryServerHtml implements Runnable {
	private NanoHTTPD server;
	protected TraceHandler tracer = new TraceHandler();

	abstract protected WLoginResult login(String who, String cookie);

	abstract protected WLoginResult login(String who, String key,
			String subkey);

	abstract protected WLoginResult login(boolean badLogin, boolean badCookie);

	abstract protected Response getList(String uri, WLoginResult login)
			throws IOException;

	abstract protected Response getStoryPart(String uri, WLoginResult login);

	abstract protected Response setStoryPart(String uri, String value,
			WLoginResult login) throws IOException;

	abstract protected Response getCover(String uri, WLoginResult login)
			throws IOException;

	abstract protected Response setCover(String uri, String luid,
			WLoginResult login) throws IOException;

	abstract protected List<MetaData> metas(WLoginResult login)
			throws IOException;

	abstract protected Story story(String luid, WLoginResult login)
			throws IOException;

	protected abstract Response imprt(String uri, String url,
			WLoginResult login) throws IOException;

	protected abstract Response imprtProgress(String uri, WLoginResult login);

	protected abstract Response delete(String uri, WLoginResult login)
			throws IOException;

	public WebLibraryServerHtml(boolean secure) throws IOException {
		Integer port = Instance.getInstance().getConfig()
				.getInteger(Config.SERVER_PORT);
		if (port == null) {
			throw new IOException(
					"Cannot start web server: port not specified");
		}

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

				WLoginResult login = null;
				Map<String, String> params = session.getParms();
				String who = session.getRemoteHostName()
						+ session.getRemoteIpAddress();
				if (params.get("login") != null) {
					login = login(who, params.get("password"),
							params.get("login"));
				} else {
					String cookie = cookies.get("cookie");
					login = login(who, cookie);
				}

				if (login.isSuccess()) {
					// refresh cookie
					session.getCookies().set(new Cookie("cookie",
							login.getCookie(), "30; path=/"));

					// set options
					String optionName = params.get("optionName");
					if (optionName != null && !optionName.isEmpty()) {
						String optionNo = params.get("optionNo");
						String optionValue = params.get("optionValue");
						if (optionNo != null || optionValue == null
								|| optionValue.isEmpty()) {
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
				if (!login.isSuccess() && WebLibraryUrls.isSupportedUrl(uri)) {
					rep = loginPage(login, uri);
				}

				if (rep == null) {
					try {
						if (WebLibraryUrls.isSupportedUrl(uri)) {
							if (WebLibraryUrls.INDEX_URL.equals(uri)) {
								rep = root(session, cookies, login);
							} else if (WebLibraryUrls.VERSION_URL.equals(uri)) {
								rep = newFixedLengthResponse(Status.OK,
										MIME_PLAINTEXT,
										Version.getCurrentVersion().toString());
							} else if (WebLibraryUrls.isCoverUrl(uri)) {
								String luid = params.get("luid");
								if (luid != null) {
									rep = setCover(uri, luid, login);
								} else {
									rep = getCover(uri, login);
								}
							} else if (WebLibraryUrls.isListUrl(uri)) {
								rep = getList(uri, login);
							} else if (WebLibraryUrls.isStoryUrl(uri)) {
								String value = params.get("value");
								if (value != null) {
									rep = setStoryPart(uri, value, login);
								} else {
									rep = getStoryPart(uri, login);
								}
							} else if (WebLibraryUrls.isViewUrl(uri)) {
								rep = getViewer(cookies, uri, login);
							} else if (WebLibraryUrls.LOGOUT_URL.equals(uri)) {
								session.getCookies().delete("cookie");
								cookies.remove("cookie");
								rep = loginPage(login(false, false), uri);
							} else if (WebLibraryUrls.isImprtUrl(uri)) {
								String url = params.get("url");
								if (url != null) {
									rep = imprt(uri, url, login);
								} else {
									rep = imprtProgress(uri, login);
								}
							} else if (WebLibraryUrls.isDeleteUrl(uri)) {
								rep = delete(uri, login);
							} else {
								getTraceHandler().error(
										"Supported URL was not processed: "
												+ uri);
								rep = newFixedLengthResponse(
										Status.INTERNAL_ERROR,
										NanoHTTPD.MIME_PLAINTEXT,
										"An error happened");
							}
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
							}

							if (rep == null) {
								getTraceHandler().trace("404: " + uri);
								rep = newFixedLengthResponse(Status.NOT_FOUND,
										NanoHTTPD.MIME_PLAINTEXT, "Not Found");
							}
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
			}
		};

		if (ssf != null)

		{
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
	 * The traces handler for this {@link WebLibraryServerHtml}.
	 * 
	 * @return the traces handler
	 */
	public TraceHandler getTraceHandler() {
		return tracer;
	}

	/**
	 * The traces handler for this {@link WebLibraryServerHtml}.
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

	private Response loginPage(WLoginResult login, String uri) {
		StringBuilder builder = new StringBuilder();

		appendPreHtml(builder, true);

		if (login.isBadLogin()) {
			builder.append("<div class='error'>Bad login or password</div>");
		} else if (login.isBadCookie()) {
			builder.append("<div class='error'>Your session timed out</div>");
		}

		if (WebLibraryUrls.LOGOUT_URL.equals(uri)) {
			uri = WebLibraryUrls.INDEX_URL;
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

	private Response root(IHTTPSession session, Map<String, String> cookies,
			WLoginResult login) throws IOException {
		BasicLibrary lib = Instance.getInstance().getLibrary();
		MetaResultList result = new MetaResultList(metas(login));
		StringBuilder builder = new StringBuilder();

		appendPreHtml(builder, true);

		Map<String, String> params = session.getParms();

		String filter = cookies.get("filter");
		if (params.get("optionNo") != null)
			filter = null;
		if (filter == null) {
			filter = "";
		}

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
		builder.append("\t<span class='label'>Filter: </span>\n");
		builder.append(
				"\t<input name='optionName'  type='hidden' value='filter' />\n");
		builder.append("\t<input name='optionValue' type='text'   value='"
				+ filter + "' place-holder='...' />\n");
		builder.append("\t<input name='optionNo'  type='submit' value='x' />");
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
			builder.append(
					WebLibraryUrls.getViewUrl(meta.getLuid(), null, null));
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

	private Response getViewer(Map<String, String> cookies, String uri,
			WLoginResult login) {
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
		int chapter = 0;
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
			Story story = story(luid, login);
			if (story == null) {
				return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND,
						NanoHTTPD.MIME_PLAINTEXT, "Story not found");
			}

			StringBuilder builder = new StringBuilder();
			appendPreHtml(builder, false);

			// For images documents, always go to the images if not chap 0 desc
			if (story.getMeta().isImageDocument()) {
				if (chapter > 0 && paragraph <= 0)
					paragraph = 1;
			}

			Chapter chap = null;
			if (chapter <= 0) {
				chap = story.getMeta().getResume();
			} else {
				try {
					chap = story.getChapters().get(chapter - 1);
				} catch (IndexOutOfBoundsException e) {
					return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND,
							NanoHTTPD.MIME_PLAINTEXT, "Chapter not found");
				}
			}

			String first, previous, next, last;

			StringBuilder content = new StringBuilder();

			String disabledLeft = "";
			String disabledRight = "";
			String disabledZoomReal = "";
			String disabledZoomWidth = "";
			String disabledZoomHeight = "";

			if (paragraph <= 0) {
				first = WebLibraryUrls.getViewUrl(luid, 0, null);
				previous = WebLibraryUrls.getViewUrl(luid,
						(Math.max(chapter - 1, 0)), null);
				next = WebLibraryUrls.getViewUrl(luid,
						(Math.min(chapter + 1, story.getChapters().size())),
						null);
				last = WebLibraryUrls.getViewUrl(luid,
						story.getChapters().size(), null);

				StringBuilder desc = new StringBuilder();

				if (chapter <= 0) {
					desc.append("<h1 class='title'>");
					desc.append(story.getMeta().getTitle());
					desc.append("</h1>\n");
					desc.append("<div class='desc'>\n");
					desc.append("\t<a href='" + next + "' class='cover'>\n");
					desc.append("\t\t<img src='/story/" + luid + "/cover'/>\n");
					desc.append("\t</a>\n");
					desc.append("\t<table class='details'>\n");
					Map<String, String> details = BasicLibrary
							.getMetaDesc(story.getMeta());
					for (String key : details.keySet()) {
						appendTableRow(desc, 2, key, details.get(key));
					}
					desc.append("\t</table>\n");
					desc.append("</div>\n");
					desc.append("<h1 class='title'>Description</h1>\n");
				}

				content.append("<div class='viewer text'>\n");
				content.append(desc);
				String description = new TextOutput(false).convert(chap,
						chapter > 0);
				content.append(chap.getParagraphs().size() <= 0
						? "No content provided."
						: description);
				content.append("</div>\n");

				if (chapter <= 0)
					disabledLeft = " disabled='disbaled'";
				if (chapter >= story.getChapters().size())
					disabledRight = " disabled='disbaled'";
			} else {
				first = WebLibraryUrls.getViewUrl(luid, chapter, 1);
				previous = WebLibraryUrls.getViewUrl(luid, chapter,
						(Math.max(paragraph - 1, 1)));
				next = WebLibraryUrls.getViewUrl(luid, chapter,
						(Math.min(paragraph + 1, chap.getParagraphs().size())));
				last = WebLibraryUrls.getViewUrl(luid, chapter,
						chap.getParagraphs().size());

				if (paragraph <= 1)
					disabledLeft = " disabled='disbaled'";
				if (paragraph >= chap.getParagraphs().size())
					disabledRight = " disabled='disbaled'";

				// First -> previous *chapter*
				if (chapter > 0)
					disabledLeft = "";
				first = WebLibraryUrls.getViewUrl(luid,
						(Math.max(chapter - 1, 0)), null);
				if (paragraph <= 1) {
					previous = first;
				}

				Paragraph para = null;
				try {
					para = chap.getParagraphs().get(paragraph - 1);
				} catch (IndexOutOfBoundsException e) {
					return NanoHTTPD.newFixedLengthResponse(Status.NOT_FOUND,
							NanoHTTPD.MIME_PLAINTEXT,
							"Paragraph " + paragraph + " not found");
				}

				if (para.getType() == ParagraphType.IMAGE) {
					String zoomStyle = "max-width: 100%;";
					disabledZoomWidth = " disabled='disabled'";
					String zoomOption = cookies.get("zoom");
					if (zoomOption != null && !zoomOption.isEmpty()) {
						if (zoomOption.equals("real")) {
							zoomStyle = "";
							disabledZoomWidth = "";
							disabledZoomReal = " disabled='disabled'";
						} else if (zoomOption.equals("width")) {
							zoomStyle = "max-width: 100%;";
						} else if (zoomOption.equals("height")) {
							// see height of navbar + optionbar
							zoomStyle = "max-height: calc(100% - 128px);";
							disabledZoomWidth = "";
							disabledZoomHeight = " disabled='disabled'";
						}
					}

					String javascript = "document.getElementById(\"previous\").click(); return false;";
					content.append(String.format("" //
							+ "<a class='viewer link' oncontextmenu='%s' href='%s'>"
							+ "<img class='viewer img' style='%s' src='%s'/>"
							+ "</a>", //
							javascript, //
							next, //
							zoomStyle, //
							WebLibraryUrls.getStoryUrl(luid, chapter,
									paragraph)));
				} else {
					content.append(String.format("" //
							+ "<div class='viewer text'>%s</div>", //
							para.getContent()));
				}
			}

			builder.append(String.format("" //
					+ "<div class='bar navbar'>\n" //
					+ "\t<a%s class='button first' href='%s'>&lt;&lt;</a>\n"//
					+ "\t<a%s id='previous' class='button previous' href='%s'>&lt;</a>\n" //
					+ "\t<div class='gotobox itemsbox'>\n" //
					+ "\t\t<div class='button goto'>%d</div>\n" //
					+ "\t\t<div class='items goto'>\n", //
					disabledLeft, first, //
					disabledLeft, previous, //
					paragraph > 0 ? paragraph : chapter //
			));

			// List of chap/para links

			appendItemA(builder, 3, WebLibraryUrls.getViewUrl(luid, 0, null),
					"Description", paragraph == 0 && chapter == 0);
			if (paragraph > 0) {
				for (int i = 1; i <= chap.getParagraphs().size(); i++) {
					appendItemA(builder, 3,
							WebLibraryUrls.getViewUrl(luid, chapter, i),
							"Image " + i, paragraph == i);
				}
			} else {
				int i = 1;
				for (Chapter c : story.getChapters()) {
					String chapName = "Chapter " + c.getNumber();
					if (c.getName() != null && !c.getName().isEmpty()) {
						chapName += ": " + c.getName();
					}

					appendItemA(builder, 3,
							WebLibraryUrls.getViewUrl(luid, i, null), chapName,
							chapter == i);

					i++;
				}
			}

			builder.append(String.format("" //
					+ "\t\t</div>\n" //
					+ "\t</div>\n" //
					+ "\t<a%s class='button next' href='%s'>&gt;</a>\n" //
					+ "\t<a%s class='button last' href='%s'>&gt;&gt;</a>\n"//
					+ "</div>\n", //
					disabledRight, next, //
					disabledRight, last //
			));

			builder.append(content);

			builder.append("<div class='bar optionbar ");
			if (paragraph > 0) {
				builder.append("s4");
			} else {
				builder.append("s1");
			}
			builder.append("'>\n");
			builder.append("	<a class='button back' href='/'>BACK</a>\n");

			if (paragraph > 0) {
				builder.append(String.format("" //
						+ "\t<a%s class='button zoomreal'   href='%s'>REAL</a>\n"//
						+ "\t<a%s class='button zoomwidth'  href='%s'>WIDTH</a>\n"//
						+ "\t<a%s class='button zoomheight' href='%s'>HEIGHT</a>\n"//
						+ "</div>\n", //
						disabledZoomReal,
						uri + "?optionName=zoom&optionValue=real", //
						disabledZoomWidth,
						uri + "?optionName=zoom&optionValue=width", //
						disabledZoomHeight,
						uri + "?optionName=zoom&optionValue=height" //
				));
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

	protected Response newInputStreamResponse(String mimeType, InputStream in) {
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
			builder.append("\t<img class='ico' src='/") //
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

	private void appendTableRow(StringBuilder builder, int depth,
			String... tds) {
		for (int i = 0; i < depth; i++) {
			builder.append("\t");
		}

		int col = 1;
		builder.append("<tr>");
		for (String td : tds) {
			builder.append("<td class='col");
			builder.append(col++);
			builder.append("'>");
			builder.append(td);
			builder.append("</td>");
		}
		builder.append("</tr>\n");
	}

	private void appendItemA(StringBuilder builder, int depth, String link,
			String name, boolean selected) {
		for (int i = 0; i < depth; i++) {
			builder.append("\t");
		}

		builder.append("<a href='");
		builder.append(link);
		builder.append("' class='item goto");
		if (selected) {
			builder.append(" selected");
		}
		builder.append("'>");
		builder.append(name);
		builder.append("</a>\n");
	}
}
