package be.nikiroo.fanfix.library;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.WebLibraryServer.WLoginResult;
import be.nikiroo.fanfix.library.web.WebLibraryServerIndex;
import be.nikiroo.fanfix.library.web.templates.WebLibraryServerTemplates;
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

	WebLibraryServerTemplates templates = WebLibraryServerTemplates
			.getInstance();

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

	/**
	 * Wait until all operations are done and stop the server.
	 * <p>
	 * All the new R/W operations will be refused after a call to stop.
	 */
	protected abstract Response stop(WLoginResult login);

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
				try {
					if (!login.isSuccess()
							&& WebLibraryUrls.isSupportedUrl(uri, true)) {
						rep = loginPage(login, uri);
					}

					if (rep == null) {
						if (WebLibraryUrls.isSupportedUrl(uri, false)) {
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
							} else if (WebLibraryUrls.EXIT_URL.equals(uri)) {
								rep = WebLibraryServerHtml.this.stop(login);
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
								} else if (uri.endsWith(".png")) {
									mimeType = "image/png";
								} else if (uri.endsWith(".ico")) {
									mimeType = "image/x-icon";
								} else if (uri.endsWith(".java")) {
									mimeType = "text/plain";
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
					}
				} catch (Exception e) {
					Instance.getInstance().getTraceHandler().error(
							new IOException("Cannot process web request", e));
					rep = newFixedLengthResponse(Status.INTERNAL_ERROR,
							NanoHTTPD.MIME_PLAINTEXT, "An error occured");
				}

				return rep;
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

	protected void doStop() {
		server.stop();
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

	private Response loginPage(WLoginResult login, String uri)
			throws IOException {
		List<Template> content = new ArrayList<Template>();

		if (login.isBadLogin()) {
			content.add(templates.message("Bad login or password", true));
		} else if (login.isBadCookie()) {
			content.add(templates.message("Your session timed out", true));
		}

		content.add(templates.login(uri));

		return NanoHTTPD.newChunkedResponse(Status.FORBIDDEN,
				NanoHTTPD.MIME_HTML,
				templates.index(true, false, content).read());
	}

	private Response root(IHTTPSession session, Map<String, String> cookies,
			WLoginResult login) throws IOException {
		BasicLibrary lib = Instance.getInstance().getLibrary();
		MetaResultList result = new MetaResultList(metas(login));

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

		List<Template> selects = new ArrayList<Template>();
		boolean sourcesSel = false;
		boolean authorsSel = false;
		boolean tagsSel = false;

		if (!browser.isEmpty()) {
			List<Template> options = new ArrayList<Template>();

			if (browser.equals("sources")) {
				sourcesSel = true;
				filterSource = browser2.isEmpty() ? filterSource : browser2;

				// TODO: if 1 group -> no group
				Map<String, List<String>> sources = result.getSourcesGrouped();
				for (String source : sources.keySet()) {
					options.add(
							templates.browserOption(source, source, browser2));
				}
			} else if (browser.equals("authors")) {
				authorsSel = true;
				filterAuthor = browser2.isEmpty() ? filterAuthor : browser2;

				// TODO: if 1 group -> no group
				Map<String, List<String>> authors = result.getAuthorsGrouped();
				for (String author : authors.keySet()) {
					options.add(
							templates.browserOption(author, author, browser2));
				}
			} else if (browser.equals("tags")) {
				tagsSel = true;
				filterTag = browser2.isEmpty() ? filterTag : browser2;

				for (String tag : result.getTags()) {
					options.add(templates.browserOption(tag, tag, browser2));
				}
			}

			selects.add(templates.browserSelect("browser2", browser2, options));
		}

		if (!browser2.isEmpty()) {
			List<Template> options = new ArrayList<Template>();

			if (browser.equals("sources")) {
				filterSource = browser3.isEmpty() ? filterSource : browser3;
				Map<String, List<String>> sourcesGrouped = result
						.getSourcesGrouped();
				List<String> sources = sourcesGrouped.get(browser2);
				if (sources != null && !sources.isEmpty()) {
					// TODO: single empty value
					for (String source : sources) {
						options.add(templates.browserOption(source, source,
								browser3));
					}
				}
			} else if (browser.equals("authors")) {
				filterAuthor = browser3.isEmpty() ? filterAuthor : browser3;
				Map<String, List<String>> authorsGrouped = result
						.getAuthorsGrouped();
				List<String> authors = authorsGrouped.get(browser2);
				if (authors != null && !authors.isEmpty()) {
					// TODO: single empty value
					for (String author : authors) {
						options.add(templates.browserOption(author, author,
								browser3));
					}
				}
			}

			selects.add(templates.browserSelect("browser3", browser3, options));
		}

		List<Template> booklines = new ArrayList<Template>();
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

			String author = "";
			if (meta.getAuthor() != null && !meta.getAuthor().isEmpty()) {
				author = "(" + meta.getAuthor() + ")";
			}

			booklines.add(templates.bookline( //
					meta.getLuid(), //
					WebLibraryUrls.getViewUrl(meta.getLuid(), null, null), //
					meta.getTitle(), //
					author, //
					lib.isCached(meta.getLuid()) //
			));
		}

		// Add the browser in front of the booklines
		booklines.add(0, templates.browser(browser, filter, selects));

		return newInputStreamResponse(NanoHTTPD.MIME_HTML,
				templates.index(true, false, booklines).read());
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

			boolean disabledLeft = false;
			boolean disabledRight = false;
			boolean disabledZoomReal = false;
			boolean disabledZoomWidth = false;
			boolean disabledZoomWidthLimited = false;
			boolean disabledZoomHeight = false;

			Template viewerItem = null;
			if (paragraph <= 0) {
				first = WebLibraryUrls.getViewUrl(luid, 0, null);
				previous = WebLibraryUrls.getViewUrl(luid,
						(Math.max(chapter - 1, 0)), null);
				next = WebLibraryUrls.getViewUrl(luid,
						(Math.min(chapter + 1, story.getChapters().size())),
						null);
				last = WebLibraryUrls.getViewUrl(luid,
						story.getChapters().size(), null);

				Template desc = null;
				if (chapter <= 0) {
					List<Template> desclines = new ArrayList<Template>();
					Map<String, String> details = BasicLibrary
							.getMetaDesc(story.getMeta());
					for (String key : details.keySet()) {
						desclines.add(templates.viewerDescline(key,
								details.get(key)));
					}

					desc = templates.viewerDesc( //
							story.getMeta().getTitle(), //
							next, //
							WebLibraryUrls.getStoryUrlCover(luid), //
							desclines //
					);
				}

				String content;
				if (chap.getParagraphs().size() <= 0) {
					content = "No content provided.";
				} else {
					content = new TextOutput(false).convert(chap, chapter > 0);
				}

				viewerItem = templates.viewerText(desc, content);

				if (chapter <= 0)
					disabledLeft = true;
				if (chapter >= story.getChapters().size())
					disabledRight = true;
			} else {
				first = WebLibraryUrls.getViewUrl(luid, chapter, 1);
				previous = WebLibraryUrls.getViewUrl(luid, chapter,
						(Math.max(paragraph - 1, 1)));
				next = WebLibraryUrls.getViewUrl(luid, chapter,
						(Math.min(paragraph + 1, chap.getParagraphs().size())));
				last = WebLibraryUrls.getViewUrl(luid, chapter,
						chap.getParagraphs().size());

				if (paragraph <= 1)
					disabledLeft = true;
				if (paragraph >= chap.getParagraphs().size())
					disabledRight = true;

				// First -> previous *chapter*
				if (chapter > 0)
					disabledLeft = false;
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
					// default values:
					String zoomStyle = "max-width: 800px;";
					disabledZoomWidthLimited = true;

					String zoomOption = cookies.get("zoom");
					if (zoomOption != null && !zoomOption.isEmpty()) {
						if (zoomOption.equals("real")) {
							zoomStyle = "";
							disabledZoomWidthLimited = false;
							disabledZoomReal = true;
						} else if (zoomOption.equals("widthlimited")) {
							// default
						} else if (zoomOption.equals("width")) {
							zoomStyle = "max-width: 100%;";
							disabledZoomWidthLimited = false;
							disabledZoomWidth = true;
						} else if (zoomOption.equals("height")) {
							// see height of navbar + optionbar
							zoomStyle = "max-height: calc(100% - 128px);";
							disabledZoomWidthLimited = false;
							disabledZoomHeight = true;
						}
					}

					viewerItem = templates.viewerImage(
							WebLibraryUrls.getStoryUrl(luid, chapter,
									paragraph), //
							disabledRight ? null : next, //
							zoomStyle //
					);
				} else {
					viewerItem = templates.viewerText(null,
							new TextOutput(false).convert(para));
				}
			}

			// List of chap/para links for navbar

			List<Template> links = new ArrayList<Template>();
			links.add(templates.viewerLink( //
					"Description", //
					WebLibraryUrls.getViewUrl(luid, 0, null), //
					paragraph == 0 && chapter == 0 //
			));
			if (paragraph > 0) {
				for (int i = 1; i <= chap.getParagraphs().size(); i++) {
					links.add(templates.viewerLink( //
							"Image " + i, //
							WebLibraryUrls.getViewUrl(luid, chapter, i), //
							paragraph == i //
					));
				}
			} else {
				int i = 1;
				for (Chapter c : story.getChapters()) {
					String chapName = "Chapter " + c.getNumber();
					if (c.getName() != null && !c.getName().isEmpty()) {
						chapName += ": " + c.getName();
					}

					links.add(templates.viewerLink( //
							chapName, //
							WebLibraryUrls.getViewUrl(luid, i, null), //
							chapter == i //
					));

					i++;
				}
			}

			// Navbar

			Template navbar = templates.viewerNavbar( //
					paragraph > 0 ? paragraph : chapter, //
					links, //
					first, //
					previous, //
					next, //
					last, //
					disabledLeft, //
					disabledLeft, //
					disabledRight, //
					disabledRight //
			);

			// Buttons on the optionbar

			List<Template> buttons = new ArrayList<Template>();
			buttons.add(templates.viewerOptionbarButton( //
					"Back", "/", "back", false));
			if (paragraph > 0) {
				buttons.add(templates.viewerOptionbarButton( //
						"1:1", uri + "?optionName=zoom&optionValue=real",
						"zoomreal", disabledZoomReal));
				buttons.add(templates.viewerOptionbarButton( //
						"]width[",
						uri + "?optionName=zoom&optionValue=widthlimited",
						"zoomwidthlimited", disabledZoomWidthLimited));
				buttons.add(templates.viewerOptionbarButton( //
						"Width", uri + "?optionName=zoom&optionValue=width",
						"zoomwidth", disabledZoomWidth));
				buttons.add(templates.viewerOptionbarButton( //
						"Height", uri + "?optionName=zoom&optionValue=height",
						"zoomheight", disabledZoomHeight));
			}

			// Optionbar

			Template optionbar = templates.viewerOptionbar( //
					(paragraph > 0) ? 5 : 1, //
					buttons //
			);

			// Full content

			return newInputStreamResponse(NanoHTTPD.MIME_HTML, //
					templates.index(false, (paragraph > 0), Arrays.asList( //
							navbar, //
							viewerItem, //
							optionbar //
					)).read());
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
}
