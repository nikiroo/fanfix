package be.nikiroo.fanfix.supported;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;

/**
 * This class is the base class used by the other support classes. It can be
 * used outside of this package, and have static method that you can use to get
 * access to the correct support class.
 * <p>
 * It will be used with 'resources' (usually web pages or files).
 * 
 * @author niki
 */
public abstract class BasicSupport {
	private Document sourceNode;
	private URL source;
	private SupportType type;
	private URL currentReferer; // with only one 'r', as in 'HTTP'...

	/**
	 * Check if the given resource is supported by this {@link BasicSupport}.
	 * 
	 * @param url
	 *            the resource to check for
	 * 
	 * @return TRUE if it is
	 */
	protected abstract boolean supports(URL url);

	/**
	 * Return TRUE if the support will return HTML encoded content values for
	 * the chapters content.
	 * 
	 * @return TRUE for HTML
	 */
	protected abstract boolean isHtml();

	/**
	 * Return the {@link MetaData} of this story.
	 * 
	 * @return the associated {@link MetaData}, never NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract MetaData getMeta() throws IOException;

	/**
	 * Return the story description.
	 * 
	 * @return the description
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract String getDesc() throws IOException;

	/**
	 * Return the list of chapters (name and resource).
	 * <p>
	 * Can be NULL if this {@link BasicSupport} do no use chapters.
	 * 
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the chapters or NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract List<Entry<String, URL>> getChapters(Progress pg)
			throws IOException;

	/**
	 * Return the content of the chapter (possibly HTML encoded, if
	 * {@link BasicSupport#isHtml()} is TRUE).
	 * 
	 * @param chapUrl
	 *            the chapter {@link URL}
	 * @param number
	 *            the chapter number
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the content
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract String getChapterContent(URL chapUrl, int number,
			Progress pg) throws IOException;

	/**
	 * Return the list of cookies (values included) that must be used to
	 * correctly fetch the resources.
	 * <p>
	 * You are expected to call the super method implementation if you override
	 * it.
	 * 
	 * @return the cookies
	 */
	public Map<String, String> getCookies() {
		return new HashMap<String, String>();
	}

	/**
	 * OAuth authorisation (aka, "bearer XXXXXXX").
	 * 
	 * @return the OAuth string
	 */
	public String getOAuth() {
		return null;
	}

	/**
	 * Return the canonical form of the main {@link URL}.
	 * 
	 * @param source
	 *            the source {@link URL}, which can be NULL
	 * 
	 * @return the canonical form of this {@link URL} or NULL if the source was
	 *         NULL
	 */
	protected URL getCanonicalUrl(URL source) {
		return source;
	}

	/**
	 * The main {@link Node} for this {@link Story}.
	 * 
	 * @return the node
	 */
	protected Element getSourceNode() {
		return sourceNode;
	}

	/**
	 * The main {@link URL} for this {@link Story}.
	 * 
	 * @return the URL
	 */
	protected URL getSource() {
		return source;
	}

	/**
	 * The current referer {@link URL} (only one 'r', as in 'HTML'...), i.e.,
	 * the current {@link URL} we work on.
	 * 
	 * @return the referer
	 */
	public URL getCurrentReferer() {
		return currentReferer;
	}

	/**
	 * The current referer {@link URL} (only one 'r', as in 'HTML'...), i.e.,
	 * the current {@link URL} we work on.
	 * 
	 * @param currentReferer
	 *            the new referer
	 */
	protected void setCurrentReferer(URL currentReferer) {
		this.currentReferer = currentReferer;
	}

	/**
	 * The support type.
	 * 
	 * @return the type
	 */
	public SupportType getType() {
		return type;
	}

	/**
	 * The support type.
	 * 
	 * @param type
	 *            the new type
	 */
	protected void setType(SupportType type) {
		this.type = type;
	}

	/**
	 * Open an input link that will be used for the support.
	 * <p>
	 * Can return NULL, in which case you are supposed to work without a source
	 * node.
	 * 
	 * @param source
	 *            the source {@link URL}
	 * 
	 * @return the {@link InputStream}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected Document loadDocument(URL source) throws IOException {
		String url = getCanonicalUrl(source).toString();
		return DataUtil.load(Instance.getCache().open(source, this, false),
				"UTF-8", url.toString());
	}

	/**
	 * Log into the support (can be a no-op depending upon the support).
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected void login() throws IOException {
	}

	/**
	 * Now that we have processed the {@link Story}, close the resources if any.
	 */
	protected void close() {
		setCurrentReferer(null);
	}

	/**
	 * Process the given story resource into a partially filled {@link Story}
	 * object containing the name and metadata.
	 * 
	 * @param getDesc
	 *            retrieve the description of the story, or not
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the {@link Story}, never NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected Story processMeta(boolean getDesc, Progress pg)
			throws IOException {
		if (pg == null) {
			pg = new Progress();
		} else {
			pg.setMinMax(0, 100);
		}

		pg.setProgress(30);

		Story story = new Story();
		MetaData meta = getMeta();
		if (meta.getCreationDate() == null || meta.getCreationDate().isEmpty()) {
			meta.setCreationDate(StringUtils.fromTime(new Date().getTime()));
		}
		story.setMeta(meta);

		pg.setProgress(50);

		if (meta.getCover() == null) {
			meta.setCover(BasicSupportHelper.getDefaultCover(meta.getSubject()));
		}

		pg.setProgress(60);

		if (getDesc) {
			String descChapterName = Instance.getTrans().getString(
					StringId.DESCRIPTION);
			story.getMeta().setResume(
					BasicSupportPara.makeChapter(this, source, 0,
							descChapterName, //
							getDesc(), isHtml(), null));
		}

		pg.done();
		return story;
	}

	/**
	 * Process the given story resource into a fully filled {@link Story}
	 * object.
	 * 
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the {@link Story}, never NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	// ADD final when BasicSupport_Deprecated is gone
	public Story process(Progress pg) throws IOException {
		setCurrentReferer(source);
		login();
		sourceNode = loadDocument(source);

		try {
			return doProcess(pg);
		} finally {
			close();
		}
	}

	/**
	 * Actual processing step, without the calls to other methods.
	 * <p>
	 * Will convert the story resource into a fully filled {@link Story} object.
	 * 
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the {@link Story}, never NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected Story doProcess(Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		} else {
			pg.setMinMax(0, 100);
		}

		pg.setProgress(1);
		Progress pgMeta = new Progress();
		pg.addProgress(pgMeta, 10);
		Story story = processMeta(true, pgMeta);
		pgMeta.done(); // 10%

		pg.setName("Retrieving " + story.getMeta().getTitle());

		Progress pgGetChapters = new Progress();
		pg.addProgress(pgGetChapters, 10);
		story.setChapters(new ArrayList<Chapter>());
		List<Entry<String, URL>> chapters = getChapters(pgGetChapters);
		pgGetChapters.done(); // 20%

		if (chapters != null) {
			Progress pgChaps = new Progress("Extracting chapters", 0,
					chapters.size() * 300);
			pg.addProgress(pgChaps, 80);

			long words = 0;
			int i = 1;
			for (Entry<String, URL> chap : chapters) {
				pgChaps.setName("Extracting chapter " + i);
				URL chapUrl = chap.getValue();
				String chapName = chap.getKey();
				if (chapUrl != null) {
					setCurrentReferer(chapUrl);
				}

				pgChaps.setProgress(i * 100);
				Progress pgGetChapterContent = new Progress();
				Progress pgMakeChapter = new Progress();
				pgChaps.addProgress(pgGetChapterContent, 100);
				pgChaps.addProgress(pgMakeChapter, 100);

				String content = getChapterContent(chapUrl, i,
						pgGetChapterContent);
				pgGetChapterContent.done();
				Chapter cc = BasicSupportPara.makeChapter(this, chapUrl, i,
						chapName, content, isHtml(), pgMakeChapter);
				pgMakeChapter.done();

				words += cc.getWords();
				story.getChapters().add(cc);
				story.getMeta().setWords(words);

				i++;
			}

			pgChaps.setName("Extracting chapters");
			pgChaps.done();
		}

		pg.done();

		return story;
	}

	/**
	 * Return a {@link BasicSupport} implementation supporting the given
	 * resource if possible.
	 * 
	 * @param url
	 *            the story resource
	 * 
	 * @return an implementation that supports it, or NULL
	 */
	public static BasicSupport getSupport(URL url) {
		if (url == null) {
			return null;
		}

		// TEXT and INFO_TEXT always support files (not URLs though)
		for (SupportType type : SupportType.values()) {
			if (type != SupportType.TEXT && type != SupportType.INFO_TEXT) {
				BasicSupport support = getSupport(type, url);
				if (support != null && support.supports(url)) {
					return support;
				}
			}
		}

		for (SupportType type : new SupportType[] { SupportType.INFO_TEXT,
				SupportType.TEXT }) {
			BasicSupport support = getSupport(type, url);
			if (support != null && support.supports(url)) {
				return support;
			}
		}

		return null;
	}

	/**
	 * Return a {@link BasicSupport} implementation supporting the given type.
	 * 
	 * @param type
	 *            the type
	 * @param url
	 *            the {@link URL} to support (can be NULL to get an
	 *            "abstract support"; if not NULL, will be used as the source
	 *            URL)
	 * 
	 * @return an implementation that supports it, or NULL
	 */
	public static BasicSupport getSupport(SupportType type, URL url) {
		BasicSupport support = null;

		switch (type) {
		case EPUB:
			support = new Epub();
			break;
		case INFO_TEXT:
			support = new InfoText();
			break;
		case FIMFICTION:
			try {
				// Can fail if no client key or NO in options
				support = new FimfictionApi();
			} catch (IOException e) {
				support = new Fimfiction();
			}
			break;
		case FANFICTION:
			support = new Fanfiction();
			break;
		case TEXT:
			support = new Text();
			break;
		case MANGAFOX:
			support = new MangaFox();
			break;
		case E621:
			support = new E621();
			break;
		case YIFFSTAR:
			support = new YiffStar();
			break;
		case E_HENTAI:
			support = new EHentai();
			break;
		case MANGA_LEL:
			support = new MangaLel();
			break;
		case CBZ:
			support = new Cbz();
			break;
		case HTML:
			support = new Html();
			break;
		}

		if (support != null) {
			support.setType(type);
			support.source = support.getCanonicalUrl(url);
		}

		return support;
	}
}
