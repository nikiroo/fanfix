package be.nikiroo.fanfix.supported;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.ImageUtils;
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
	/**
	 * The supported input types for which we can get a {@link BasicSupport}
	 * object.
	 * 
	 * @author niki
	 */
	public enum SupportType {
		/** EPUB files created with this program */
		EPUB,
		/** Pure text file with some rules */
		TEXT,
		/** TEXT but with associated .info file */
		INFO_TEXT,
		/** My Little Pony fanfictions */
		FIMFICTION,
		/** Fanfictions from a lot of different universes */
		FANFICTION,
		/** Website with lots of Mangas */
		MANGAFOX,
		/** Furry website with comics support */
		E621,
		/** Furry website with stories */
		YIFFSTAR,
		/** Comics and images groups, mostly but not only NSFW */
		E_HENTAI,
		/** CBZ files */
		CBZ,
		/** HTML files */
		HTML;

		/**
		 * A description of this support type (more information than the
		 * {@link BasicSupport#getSourceName()}).
		 * 
		 * @return the description
		 */
		public String getDesc() {
			String desc = Instance.getTrans().getStringX(StringId.INPUT_DESC,
					this.name());

			if (desc == null) {
				desc = Instance.getTrans().getString(StringId.INPUT_DESC, this);
			}

			return desc;
		}

		/**
		 * The name of this support type (a short version).
		 * 
		 * @return the name
		 */
		public String getSourceName() {
			BasicSupport support = BasicSupport.getSupport(this);
			if (support != null) {
				return support.getSourceName();
			}

			return null;
		}

		@Override
		public String toString() {
			return super.toString().toLowerCase();
		}

		/**
		 * Call {@link SupportType#valueOf(String)} after conversion to upper
		 * case.
		 * 
		 * @param typeName
		 *            the possible type name
		 * 
		 * @return NULL or the type
		 */
		public static SupportType valueOfUC(String typeName) {
			return SupportType.valueOf(typeName == null ? null : typeName
					.toUpperCase());
		}

		/**
		 * Call {@link SupportType#valueOf(String)} after conversion to upper
		 * case but return NULL for NULL instead of raising exception.
		 * 
		 * @param typeName
		 *            the possible type name
		 * 
		 * @return NULL or the type
		 */
		public static SupportType valueOfNullOkUC(String typeName) {
			if (typeName == null) {
				return null;
			}

			return SupportType.valueOfUC(typeName);
		}

		/**
		 * Call {@link SupportType#valueOf(String)} after conversion to upper
		 * case but return NULL in case of error instead of raising an
		 * exception.
		 * 
		 * @param typeName
		 *            the possible type name
		 * 
		 * @return NULL or the type
		 */
		public static SupportType valueOfAllOkUC(String typeName) {
			try {
				return SupportType.valueOfUC(typeName);
			} catch (Exception e) {
				return null;
			}
		}
	}

	private InputStream in;
	private SupportType type;
	private URL currentReferer; // with only one 'r', as in 'HTTP'...

	// quote chars
	private char openQuote = Instance.getTrans().getCharacter(
			StringId.OPEN_SINGLE_QUOTE);
	private char closeQuote = Instance.getTrans().getCharacter(
			StringId.CLOSE_SINGLE_QUOTE);
	private char openDoubleQuote = Instance.getTrans().getCharacter(
			StringId.OPEN_DOUBLE_QUOTE);
	private char closeDoubleQuote = Instance.getTrans().getCharacter(
			StringId.CLOSE_DOUBLE_QUOTE);

	/**
	 * The name of this support class.
	 * 
	 * @return the name
	 */
	protected abstract String getSourceName();

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
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the associated {@link MetaData}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract MetaData getMeta(URL source, InputStream in)
			throws IOException;

	/**
	 * Return the story description.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the description
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract String getDesc(URL source, InputStream in)
			throws IOException;

	/**
	 * Return the list of chapters (name and resource).
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the chapters
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract List<Entry<String, URL>> getChapters(URL source,
			InputStream in, Progress pg) throws IOException;

	/**
	 * Return the content of the chapter (possibly HTML encoded, if
	 * {@link BasicSupport#isHtml()} is TRUE).
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
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
	protected abstract String getChapterContent(URL source, InputStream in,
			int number, Progress pg) throws IOException;

	/**
	 * Log into the support (can be a no-op depending upon the support).
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	@SuppressWarnings("unused")
	public void login() throws IOException {
	}

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
	 *            the source {@link URL}
	 * 
	 * @return the canonical form of this {@link URL}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	@SuppressWarnings("unused")
	public URL getCanonicalUrl(URL source) throws IOException {
		return source;
	}

	/**
	 * Process the given story resource into a partially filled {@link Story}
	 * object containing the name and metadata, except for the description.
	 * 
	 * @param url
	 *            the story resource
	 * 
	 * @return the {@link Story}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Story processMeta(URL url) throws IOException {
		return processMeta(url, true, false, null);
	}

	/**
	 * Process the given story resource into a partially filled {@link Story}
	 * object containing the name and metadata.
	 * 
	 * @param url
	 *            the story resource
	 * @param close
	 *            close "this" and "in" when done
	 * @param getDesc
	 *            retrieve the description of the story, or not
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the {@link Story}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected Story processMeta(URL url, boolean close, boolean getDesc,
			Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		} else {
			pg.setMinMax(0, 100);
		}

		login();
		pg.setProgress(10);

		url = getCanonicalUrl(url);

		setCurrentReferer(url);

		in = openInput(url); // NULL allowed here
		try {
			preprocess(url, getInput());
			pg.setProgress(30);

			Story story = new Story();
			MetaData meta = getMeta(url, getInput());
			if (meta.getCreationDate() == null
					|| meta.getCreationDate().isEmpty()) {
				meta.setCreationDate(StringUtils.fromTime(new Date().getTime()));
			}
			story.setMeta(meta);

			pg.setProgress(50);

			if (meta.getCover() == null) {
				meta.setCover(getDefaultCover(meta.getSubject()));
			}

			pg.setProgress(60);

			if (getDesc) {
				String descChapterName = Instance.getTrans().getString(
						StringId.DESCRIPTION);
				story.getMeta().setResume(
						makeChapter(url, 0, descChapterName,
								getDesc(url, getInput()), null));
			}

			pg.setProgress(100);
			return story;
		} finally {
			if (close) {
				try {
					close();
				} catch (IOException e) {
					Instance.getTraceHandler().error(e);
				}

				if (in != null) {
					in.close();
				}
			}

			setCurrentReferer(null);
		}
	}

	/**
	 * Process the given story resource into a fully filled {@link Story}
	 * object.
	 * 
	 * @param url
	 *            the story resource
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the {@link Story}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Story process(URL url, Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		} else {
			pg.setMinMax(0, 100);
		}

		url = getCanonicalUrl(url);
		pg.setProgress(1);
		try {
			Progress pgMeta = new Progress();
			pg.addProgress(pgMeta, 10);
			Story story = processMeta(url, false, true, pgMeta);
			if (!pgMeta.isDone()) {
				pgMeta.setProgress(pgMeta.getMax()); // 10%
			}

			if (story == null) {
				pg.setProgress(90);
				return null;
			}

			pg.setName("Retrieving " + story.getMeta().getTitle());

			setCurrentReferer(url);

			Progress pgGetChapters = new Progress();
			pg.addProgress(pgGetChapters, 10);
			story.setChapters(new ArrayList<Chapter>());
			List<Entry<String, URL>> chapters = getChapters(url, getInput(),
					pgGetChapters);
			if (!pgGetChapters.isDone()) {
				pgGetChapters.setProgress(pgGetChapters.getMax()); // 20%
			}

			if (chapters != null) {
				Progress pgChaps = new Progress("Extracting chapters", 0,
						chapters.size() * 300);
				pg.addProgress(pgChaps, 80);

				long words = 0;
				int i = 1;
				for (Entry<String, URL> chap : chapters) {
					pgChaps.setName("Extracting chapter " + i);
					InputStream chapIn = null;
					if (chap.getValue() != null) {
						setCurrentReferer(chap.getValue());
						chapIn = Instance.getCache().open(chap.getValue(),
								this, true);
					}
					pgChaps.setProgress(i * 100);
					try {
						Progress pgGetChapterContent = new Progress();
						Progress pgMakeChapter = new Progress();
						pgChaps.addProgress(pgGetChapterContent, 100);
						pgChaps.addProgress(pgMakeChapter, 100);

						String content = getChapterContent(url, chapIn, i,
								pgGetChapterContent);
						if (!pgGetChapterContent.isDone()) {
							pgGetChapterContent.setProgress(pgGetChapterContent
									.getMax());
						}

						Chapter cc = makeChapter(url, i, chap.getKey(),
								content, pgMakeChapter);
						if (!pgMakeChapter.isDone()) {
							pgMakeChapter.setProgress(pgMakeChapter.getMax());
						}

						words += cc.getWords();
						story.getChapters().add(cc);
						if (story.getMeta() != null) {
							story.getMeta().setWords(words);
						}
					} finally {
						if (chapIn != null) {
							chapIn.close();
						}
					}

					i++;
				}

				pgChaps.setName("Extracting chapters");
			} else {
				pg.setProgress(80);
			}

			return story;

		} finally {
			try {
				close();
			} catch (IOException e) {
				Instance.getTraceHandler().error(e);
			}

			if (in != null) {
				in.close();
			}

			setCurrentReferer(null);
		}
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
	 * @param type
	 *            the new type
	 * 
	 * @return this
	 */
	protected BasicSupport setType(SupportType type) {
		this.type = type;
		return this;
	}

	/**
	 * Prepare the support if needed before processing.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @throws IOException
	 *             on I/O error
	 */
	@SuppressWarnings("unused")
	protected void preprocess(URL source, InputStream in) throws IOException {
	}

	/**
	 * Now that we have processed the {@link Story}, close the resources if any.
	 * 
	 * @throws IOException
	 *             on I/O error
	 */
	@SuppressWarnings("unused")
	protected void close() throws IOException {
	}

	/**
	 * Create a {@link Chapter} object from the given information, formatting
	 * the content as it should be.
	 * 
	 * @param source
	 *            the source of the story
	 * @param number
	 *            the chapter number
	 * @param name
	 *            the chapter name
	 * @param content
	 *            the chapter content
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the {@link Chapter}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected Chapter makeChapter(URL source, int number, String name,
			String content, Progress pg) throws IOException {
		// Chapter name: process it correctly, then remove the possible
		// redundant "Chapter x: " in front of it, or "-" (as in
		// "Chapter 5: - Fun!" after the ": " was automatically added)
		String chapterName = processPara(name).getContent().trim();
		for (String lang : Instance.getConfig().getString(Config.CHAPTER)
				.split(",")) {
			String chapterWord = Instance.getConfig().getStringX(
					Config.CHAPTER, lang);
			if (chapterName.startsWith(chapterWord)) {
				chapterName = chapterName.substring(chapterWord.length())
						.trim();
				break;
			}
		}

		if (chapterName.startsWith(Integer.toString(number))) {
			chapterName = chapterName.substring(
					Integer.toString(number).length()).trim();
		}

		while (chapterName.startsWith(":") || chapterName.startsWith("-")) {
			chapterName = chapterName.substring(1).trim();
		}
		//

		Chapter chap = new Chapter(number, chapterName);

		if (content != null) {
			List<Paragraph> paras = makeParagraphs(source, content, pg);
			long words = 0;
			for (Paragraph para : paras) {
				words += para.getWords();
			}
			chap.setParagraphs(paras);
			chap.setWords(words);
		}

		return chap;

	}

	/**
	 * Convert the given content into {@link Paragraph}s.
	 * 
	 * @param source
	 *            the source URL of the story
	 * @param content
	 *            the textual content
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the {@link Paragraph}s
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected List<Paragraph> makeParagraphs(URL source, String content,
			Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		if (isHtml()) {
			// Special <HR> processing:
			content = content.replaceAll("(<hr [^>]*>)|(<hr/>)|(<hr>)",
					"<br/>* * *<br/>");
		}

		List<Paragraph> paras = new ArrayList<Paragraph>();

		if (content != null && !content.trim().isEmpty()) {
			if (isHtml()) {
				String[] tab = content.split("(<p>|</p>|<br>|<br/>)");
				pg.setMinMax(0, tab.length);
				int i = 1;
				for (String line : tab) {
					if (line.startsWith("[") && line.endsWith("]")) {
						pg.setName("Extracting image " + i);
					}
					paras.add(makeParagraph(source, line.trim()));
					pg.setProgress(i++);
				}
				pg.setName(null);
			} else {
				List<String> lines = new ArrayList<String>();
				BufferedReader buff = null;
				try {
					buff = new BufferedReader(
							new InputStreamReader(new ByteArrayInputStream(
									content.getBytes("UTF-8")), "UTF-8"));
					for (String line = buff.readLine(); line != null; line = buff
							.readLine()) {
						lines.add(line.trim());
					}
				} finally {
					if (buff != null) {
						buff.close();
					}
				}

				pg.setMinMax(0, lines.size());
				int i = 0;
				for (String line : lines) {
					if (line.startsWith("[") && line.endsWith("]")) {
						pg.setName("Extracting image " + i);
					}
					paras.add(makeParagraph(source, line));
					pg.setProgress(i++);
				}
				pg.setName(null);
			}

			// Check quotes for "bad" format
			List<Paragraph> newParas = new ArrayList<Paragraph>();
			for (Paragraph para : paras) {
				newParas.addAll(requotify(para));
			}
			paras = newParas;

			// Remove double blanks/brks
			fixBlanksBreaks(paras);
		}

		return paras;
	}

	/**
	 * Convert the given line into a single {@link Paragraph}.
	 * 
	 * @param source
	 *            the source URL of the story
	 * @param line
	 *            the textual content of the paragraph
	 * 
	 * @return the {@link Paragraph}
	 */
	private Paragraph makeParagraph(URL source, String line) {
		URL image = null;
		if (line.startsWith("[") && line.endsWith("]")) {
			image = getImageUrl(this, source,
					line.substring(1, line.length() - 1).trim());
		}

		if (image != null) {
			return new Paragraph(image);
		}

		return processPara(line);
	}

	/**
	 * Fix the {@link ParagraphType#BLANK}s and {@link ParagraphType#BREAK}s of
	 * those {@link Paragraph}s.
	 * <p>
	 * The resulting list will not contain a starting or trailing blank/break
	 * nor 2 blanks or breaks following each other.
	 * 
	 * @param paras
	 *            the list of {@link Paragraph}s to fix
	 */
	protected void fixBlanksBreaks(List<Paragraph> paras) {
		boolean space = false;
		boolean brk = true;
		for (int i = 0; i < paras.size(); i++) {
			Paragraph para = paras.get(i);
			boolean thisSpace = para.getType() == ParagraphType.BLANK;
			boolean thisBrk = para.getType() == ParagraphType.BREAK;

			if (i > 0 && space && thisBrk) {
				paras.remove(i - 1);
				i--;
			} else if ((space || brk) && (thisSpace || thisBrk)) {
				paras.remove(i);
				i--;
			}

			space = thisSpace;
			brk = thisBrk;
		}

		// Remove blank/brk at start
		if (paras.size() > 0
				&& (paras.get(0).getType() == ParagraphType.BLANK || paras.get(
						0).getType() == ParagraphType.BREAK)) {
			paras.remove(0);
		}

		// Remove blank/brk at end
		int last = paras.size() - 1;
		if (paras.size() > 0
				&& (paras.get(last).getType() == ParagraphType.BLANK || paras
						.get(last).getType() == ParagraphType.BREAK)) {
			paras.remove(last);
		}
	}

	/**
	 * Get the default cover related to this subject (see <tt>.info</tt> files).
	 * 
	 * @param subject
	 *            the subject
	 * 
	 * @return the cover if any, or NULL
	 */
	static BufferedImage getDefaultCover(String subject) {
		if (subject != null && !subject.isEmpty()
				&& Instance.getCoverDir() != null) {
			try {
				File fileCover = new File(Instance.getCoverDir(), subject);
				return getImage(null, fileCover.toURI().toURL(), subject);
			} catch (MalformedURLException e) {
			}
		}

		return null;
	}

	/**
	 * Return the list of supported image extensions.
	 * 
	 * @param emptyAllowed
	 *            TRUE to allow an empty extension on first place, which can be
	 *            used when you may already have an extension in your input but
	 *            are not sure about it
	 * 
	 * @return the extensions
	 */
	static String[] getImageExt(boolean emptyAllowed) {
		if (emptyAllowed) {
			return new String[] { "", ".png", ".jpg", ".jpeg", ".gif", ".bmp" };
		}

		return new String[] { ".png", ".jpg", ".jpeg", ".gif", ".bmp" };
	}

	/**
	 * Check if the given resource can be a local image or a remote image, then
	 * refresh the cache with it if it is.
	 * 
	 * @param source
	 *            the story source
	 * @param line
	 *            the resource to check
	 * 
	 * @return the image if found, or NULL
	 * 
	 */
	static BufferedImage getImage(BasicSupport support, URL source, String line) {
		URL url = getImageUrl(support, source, line);
		if (url != null) {
			InputStream in = null;
			try {
				in = Instance.getCache().open(url, getSupport(url), true);
				return ImageUtils.fromStream(in);
			} catch (IOException e) {
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
					}
				}
			}
		}

		return null;
	}

	/**
	 * Check if the given resource can be a local image or a remote image, then
	 * refresh the cache with it if it is.
	 * 
	 * @param source
	 *            the story source
	 * @param line
	 *            the resource to check
	 * 
	 * @return the image URL if found, or NULL
	 * 
	 */
	static URL getImageUrl(BasicSupport support, URL source, String line) {
		URL url = null;

		if (line != null) {
			// try for files
			if (source != null) {
				try {

					String relPath = null;
					String absPath = null;
					try {
						String path = new File(source.getFile()).getParent();
						relPath = new File(new File(path), line.trim())
								.getAbsolutePath();
					} catch (Exception e) {
						// Cannot be converted to path (one possibility to take
						// into account: absolute path on Windows)
					}
					try {
						absPath = new File(line.trim()).getAbsolutePath();
					} catch (Exception e) {
						// Cannot be converted to path (at all)
					}

					for (String ext : getImageExt(true)) {
						if (absPath != null && new File(absPath + ext).exists()) {
							url = new File(absPath + ext).toURI().toURL();
						} else if (relPath != null
								&& new File(relPath + ext).exists()) {
							url = new File(relPath + ext).toURI().toURL();
						}
					}
				} catch (Exception e) {
					// Should not happen since we control the correct arguments
				}
			}

			if (url == null) {
				// try for URLs
				try {
					for (String ext : getImageExt(true)) {
						if (Instance.getCache()
								.check(new URL(line + ext), true)) {
							url = new URL(line + ext);
							break;
						}
					}

					// try out of cache
					if (url == null) {
						for (String ext : getImageExt(true)) {
							try {
								url = new URL(line + ext);
								Instance.getCache().refresh(url, support, true);
								break;
							} catch (IOException e) {
								// no image with this ext
								url = null;
							}
						}
					}
				} catch (MalformedURLException e) {
					// Not an url
				}
			}

			// refresh the cached file
			if (url != null) {
				try {
					Instance.getCache().refresh(url, support, true);
				} catch (IOException e) {
					// woops, broken image
					url = null;
				}
			}
		}

		return url;
	}

	/**
	 * Open the input file that will be used through the support.
	 * <p>
	 * Can return NULL, in which case you are supposed to work without an
	 * {@link InputStream}.
	 * 
	 * @param source
	 *            the source {@link URL}
	 * 
	 * @return the {@link InputStream}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected InputStream openInput(URL source) throws IOException {
		return Instance.getCache().open(source, this, false);
	}

	/**
	 * Reset then return {@link BasicSupport#in}.
	 * 
	 * @return {@link BasicSupport#in}
	 */
	protected InputStream getInput() {
		return reset(in);
	}

	/**
	 * Fix the author name if it is prefixed with some "by" {@link String}.
	 * 
	 * @param author
	 *            the author with a possible prefix
	 * 
	 * @return the author without prefixes
	 */
	protected String fixAuthor(String author) {
		if (author != null) {
			for (String suffix : new String[] { " ", ":" }) {
				for (String byString : Instance.getConfig()
						.getString(Config.BYS).split(",")) {
					byString += suffix;
					if (author.toUpperCase().startsWith(byString.toUpperCase())) {
						author = author.substring(byString.length()).trim();
					}
				}
			}

			// Special case (without suffix):
			if (author.startsWith("©")) {
				author = author.substring(1);
			}
		}

		return author;
	}

	/**
	 * Check quotes for bad format (i.e., quotes with normal paragraphs inside)
	 * and requotify them (i.e., separate them into QUOTE paragraphs and other
	 * paragraphs (quotes or not)).
	 * 
	 * @param para
	 *            the paragraph to requotify (not necessarily a quote)
	 * 
	 * @return the correctly (or so we hope) quotified paragraphs
	 */
	protected List<Paragraph> requotify(Paragraph para) {
		List<Paragraph> newParas = new ArrayList<Paragraph>();

		if (para.getType() == ParagraphType.QUOTE
				&& para.getContent().length() > 2) {
			String line = para.getContent();
			boolean singleQ = line.startsWith("" + openQuote);
			boolean doubleQ = line.startsWith("" + openDoubleQuote);

			// Do not try when more than one quote at a time
			// (some stories are not easily readable if we do)
			if (singleQ
					&& line.indexOf(closeQuote, 1) < line
							.lastIndexOf(closeQuote)) {
				newParas.add(para);
				return newParas;
			}
			if (doubleQ
					&& line.indexOf(closeDoubleQuote, 1) < line
							.lastIndexOf(closeDoubleQuote)) {
				newParas.add(para);
				return newParas;
			}
			//

			if (!singleQ && !doubleQ) {
				line = openDoubleQuote + line + closeDoubleQuote;
				newParas.add(new Paragraph(ParagraphType.QUOTE, line, para
						.getWords()));
			} else {
				char open = singleQ ? openQuote : openDoubleQuote;
				char close = singleQ ? closeQuote : closeDoubleQuote;

				int posDot = -1;
				boolean inQuote = false;
				int i = 0;
				for (char car : line.toCharArray()) {
					if (car == open) {
						inQuote = true;
					} else if (car == close) {
						inQuote = false;
					} else if (car == '.' && !inQuote) {
						posDot = i;
						break;
					}
					i++;
				}

				if (posDot >= 0) {
					String rest = line.substring(posDot + 1).trim();
					line = line.substring(0, posDot + 1).trim();
					long words = 1;
					for (char car : line.toCharArray()) {
						if (car == ' ') {
							words++;
						}
					}
					newParas.add(new Paragraph(ParagraphType.QUOTE, line, words));
					if (!rest.isEmpty()) {
						newParas.addAll(requotify(processPara(rest)));
					}
				} else {
					newParas.add(para);
				}
			}
		} else {
			newParas.add(para);
		}

		return newParas;
	}

	/**
	 * Process a {@link Paragraph} from a raw line of text.
	 * <p>
	 * Will also fix quotes and HTML encoding if needed.
	 * 
	 * @param line
	 *            the raw line
	 * 
	 * @return the processed {@link Paragraph}
	 */
	protected Paragraph processPara(String line) {
		line = ifUnhtml(line).trim();

		boolean space = true;
		boolean brk = true;
		boolean quote = false;
		boolean tentativeCloseQuote = false;
		char prev = '\0';
		int dashCount = 0;
		long words = 1;

		StringBuilder builder = new StringBuilder();
		for (char car : line.toCharArray()) {
			if (car != '-') {
				if (dashCount > 0) {
					// dash, ndash and mdash: - – —
					// currently: always use mdash
					builder.append(dashCount == 1 ? '-' : '—');
				}
				dashCount = 0;
			}

			if (tentativeCloseQuote) {
				tentativeCloseQuote = false;
				if (Character.isLetterOrDigit(car)) {
					builder.append("'");
				} else {
					// handle double-single quotes as double quotes
					if (prev == car) {
						builder.append(closeDoubleQuote);
						continue;
					}

					builder.append(closeQuote);
				}
			}

			switch (car) {
			case ' ': // note: unbreakable space
			case ' ':
			case '\t':
			case '\n': // just in case
			case '\r': // just in case
				if (builder.length() > 0
						&& builder.charAt(builder.length() - 1) != ' ') {
					words++;
				}
				builder.append(' ');
				break;

			case '\'':
				if (space || (brk && quote)) {
					quote = true;
					// handle double-single quotes as double quotes
					if (prev == car) {
						builder.deleteCharAt(builder.length() - 1);
						builder.append(openDoubleQuote);
					} else {
						builder.append(openQuote);
					}
				} else if (prev == ' ' || prev == car) {
					// handle double-single quotes as double quotes
					if (prev == car) {
						builder.deleteCharAt(builder.length() - 1);
						builder.append(openDoubleQuote);
					} else {
						builder.append(openQuote);
					}
				} else {
					// it is a quote ("I'm off") or a 'quote' ("This
					// 'good' restaurant"...)
					tentativeCloseQuote = true;
				}
				break;

			case '"':
				if (space || (brk && quote)) {
					quote = true;
					builder.append(openDoubleQuote);
				} else if (prev == ' ') {
					builder.append(openDoubleQuote);
				} else {
					builder.append(closeDoubleQuote);
				}
				break;

			case '-':
				if (space) {
					quote = true;
				} else {
					dashCount++;
				}
				space = false;
				break;

			case '*':
			case '~':
			case '/':
			case '\\':
			case '<':
			case '>':
			case '=':
			case '+':
			case '_':
			case '–':
			case '—':
				space = false;
				builder.append(car);
				break;

			case '‘':
			case '`':
			case '‹':
			case '﹁':
			case '〈':
			case '「':
				if (space || (brk && quote)) {
					quote = true;
					builder.append(openQuote);
				} else {
					// handle double-single quotes as double quotes
					if (prev == car) {
						builder.deleteCharAt(builder.length() - 1);
						builder.append(openDoubleQuote);
					} else {
						builder.append(openQuote);
					}
				}
				space = false;
				brk = false;
				break;

			case '’':
			case '›':
			case '﹂':
			case '〉':
			case '」':
				space = false;
				brk = false;
				// handle double-single quotes as double quotes
				if (prev == car) {
					builder.deleteCharAt(builder.length() - 1);
					builder.append(closeDoubleQuote);
				} else {
					builder.append(closeQuote);
				}
				break;

			case '«':
			case '“':
			case '﹃':
			case '《':
			case '『':
				if (space || (brk && quote)) {
					quote = true;
					builder.append(openDoubleQuote);
				} else {
					builder.append(openDoubleQuote);
				}
				space = false;
				brk = false;
				break;

			case '»':
			case '”':
			case '﹄':
			case '》':
			case '』':
				space = false;
				brk = false;
				builder.append(closeDoubleQuote);
				break;

			default:
				space = false;
				brk = false;
				builder.append(car);
				break;
			}

			prev = car;
		}

		if (tentativeCloseQuote) {
			tentativeCloseQuote = false;
			builder.append(closeQuote);
		}

		line = builder.toString().trim();

		ParagraphType type = ParagraphType.NORMAL;
		if (space) {
			type = ParagraphType.BLANK;
		} else if (brk) {
			type = ParagraphType.BREAK;
		} else if (quote) {
			type = ParagraphType.QUOTE;
		}

		return new Paragraph(type, line, words);
	}

	/**
	 * Remove the HTML from the input <b>if</b> {@link BasicSupport#isHtml()} is
	 * true.
	 * 
	 * @param input
	 *            the input
	 * 
	 * @return the no html version if needed
	 */
	private String ifUnhtml(String input) {
		if (isHtml() && input != null) {
			return StringUtils.unhtml(input);
		}

		return input;
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
				BasicSupport support = getSupport(type);
				if (support != null && support.supports(url)) {
					return support;
				}
			}
		}

		for (SupportType type : new SupportType[] { SupportType.INFO_TEXT,
				SupportType.TEXT }) {
			BasicSupport support = getSupport(type);
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
	 * 
	 * @return an implementation that supports it, or NULL
	 */
	public static BasicSupport getSupport(SupportType type) {
		switch (type) {
		case EPUB:
			return new Epub().setType(type);
		case INFO_TEXT:
			return new InfoText().setType(type);
		case FIMFICTION:
			try {
				// Can fail if no client key or NO in options
				return new FimfictionApi().setType(type);
			} catch (IOException e) {
				return new Fimfiction().setType(type);
			}
		case FANFICTION:
			return new Fanfiction().setType(type);
		case TEXT:
			return new Text().setType(type);
		case MANGAFOX:
			return new MangaFox().setType(type);
		case E621:
			return new E621().setType(type);
		case YIFFSTAR:
			return new YiffStar().setType(type);
		case E_HENTAI:
			return new EHentai().setType(type);
		case CBZ:
			return new Cbz().setType(type);
		case HTML:
			return new Html().setType(type);
		}

		return null;
	}

	/**
	 * Reset the given {@link InputStream} and return it.
	 * 
	 * @param in
	 *            the {@link InputStream} to reset
	 * 
	 * @return the same {@link InputStream} after reset
	 */
	static protected InputStream reset(InputStream in) {
		try {
			if (in != null) {
				in.reset();
			}
		} catch (IOException e) {
		}

		return in;
	}

	/**
	 * Return the first line from the given input which correspond to the given
	 * selectors.
	 * 
	 * @param in
	 *            the input
	 * @param needle
	 *            a string that must be found inside the target line (also
	 *            supports "^" at start to say "only if it starts with" the
	 *            needle)
	 * @param relativeLine
	 *            the line to return based upon the target line position (-1 =
	 *            the line before, 0 = the target line...)
	 * 
	 * @return the line
	 */
	static protected String getLine(InputStream in, String needle,
			int relativeLine) {
		return getLine(in, needle, relativeLine, true);
	}

	/**
	 * Return a line from the given input which correspond to the given
	 * selectors.
	 * 
	 * @param in
	 *            the input
	 * @param needle
	 *            a string that must be found inside the target line (also
	 *            supports "^" at start to say "only if it starts with" the
	 *            needle)
	 * @param relativeLine
	 *            the line to return based upon the target line position (-1 =
	 *            the line before, 0 = the target line...)
	 * @param first
	 *            takes the first result (as opposed to the last one, which will
	 *            also always spend the input)
	 * 
	 * @return the line
	 */
	static protected String getLine(InputStream in, String needle,
			int relativeLine, boolean first) {
		String rep = null;

		reset(in);

		List<String> lines = new ArrayList<String>();
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		int index = -1;
		scan.useDelimiter("\\n");
		while (scan.hasNext()) {
			lines.add(scan.next());

			if (index == -1) {
				if (needle.startsWith("^")) {
					if (lines.get(lines.size() - 1).startsWith(
							needle.substring(1))) {
						index = lines.size() - 1;
					}

				} else {
					if (lines.get(lines.size() - 1).contains(needle)) {
						index = lines.size() - 1;
					}
				}
			}

			if (index >= 0 && index + relativeLine < lines.size()) {
				rep = lines.get(index + relativeLine);
				if (first) {
					break;
				}
			}
		}

		return rep;
	}

	/**
	 * Return the text between the key and the endKey (and optional subKey can
	 * be passed, in this case we will look for the key first, then take the
	 * text between the subKey and the endKey).
	 * <p>
	 * Will only match the first line with the given key if more than one are
	 * possible. Which also means that if the subKey or endKey is not found on
	 * that line, NULL will be returned.
	 * 
	 * @param in
	 *            the input
	 * @param key
	 *            the key to match (also supports "^" at start to say
	 *            "only if it starts with" the key)
	 * @param subKey
	 *            the sub key or NULL if none
	 * @param endKey
	 *            the end key or NULL for "up to the end"
	 * @return the text or NULL if not found
	 */
	static protected String getKeyLine(InputStream in, String key,
			String subKey, String endKey) {
		return getKeyText(getLine(in, key, 0), key, subKey, endKey);
	}

	/**
	 * Return the text between the key and the endKey (and optional subKey can
	 * be passed, in this case we will look for the key first, then take the
	 * text between the subKey and the endKey).
	 * 
	 * @param in
	 *            the input
	 * @param key
	 *            the key to match (also supports "^" at start to say
	 *            "only if it starts with" the key)
	 * @param subKey
	 *            the sub key or NULL if none
	 * @param endKey
	 *            the end key or NULL for "up to the end"
	 * @return the text or NULL if not found
	 */
	static protected String getKeyText(String in, String key, String subKey,
			String endKey) {
		String result = null;

		String line = in;
		if (line != null && line.contains(key)) {
			line = line.substring(line.indexOf(key) + key.length());
			if (subKey == null || subKey.isEmpty() || line.contains(subKey)) {
				if (subKey != null) {
					line = line.substring(line.indexOf(subKey)
							+ subKey.length());
				}
				if (endKey == null || line.contains(endKey)) {
					if (endKey != null) {
						line = line.substring(0, line.indexOf(endKey));
						result = line;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Return the text between the key and the endKey (optional subKeys can be
	 * passed, in this case we will look for the subKeys first, then take the
	 * text between the key and the endKey).
	 * 
	 * @param in
	 *            the input
	 * @param key
	 *            the key to match
	 * @param endKey
	 *            the end key or NULL for "up to the end"
	 * @param afters
	 *            the sub-keys to find before checking for key/endKey
	 * 
	 * @return the text or NULL if not found
	 */
	static protected String getKeyTextAfter(String in, String key,
			String endKey, String... afters) {

		if (in != null && !in.isEmpty()) {
			int pos = indexOfAfter(in, 0, afters);
			if (pos < 0) {
				return null;
			}

			in = in.substring(pos);
		}

		return getKeyText(in, key, null, endKey);
	}

	/**
	 * Return the first index after all the given "afters" have been found in
	 * the {@link String}, or -1 if it was not possible.
	 * 
	 * @param in
	 *            the input
	 * @param startAt
	 *            start at this position in the string
	 * @param afters
	 *            the sub-keys to find before checking for key/endKey
	 * 
	 * @return the text or NULL if not found
	 */
	static protected int indexOfAfter(String in, int startAt, String... afters) {
		int pos = -1;
		if (in != null && !in.isEmpty()) {
			pos = startAt;
			if (afters != null) {
				for (int i = 0; pos >= 0 && i < afters.length; i++) {
					String subKey = afters[i];
					if (!subKey.isEmpty()) {
						pos = in.indexOf(subKey, pos);
						if (pos >= 0) {
							pos += subKey.length();
						}
					}
				}
			}
		}

		return pos;
	}
}
