package be.nikiroo.fanfix.supported;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
		/** CBZ files */
		CBZ;

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
		 * Call {@link SupportType#valueOf(String.toUpperCase())}.
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
		 * Call {@link SupportType#valueOf(String.toUpperCase())} but return
		 * NULL for NULL instead of raising exception.
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
		 * Call {@link SupportType#valueOf(String.toUpperCase())} but return
		 * NULL in case of error instead of raising an exception.
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

	/** Only used by {@link BasicSupport#getInput()} just so it is always reset. */
	private InputStream in;
	private SupportType type;
	private URL currentReferer; // with on 'r', as in 'HTTP'...

	// quote chars
	private char openQuote = Instance.getTrans().getChar(
			StringId.OPEN_SINGLE_QUOTE);
	private char closeQuote = Instance.getTrans().getChar(
			StringId.CLOSE_SINGLE_QUOTE);
	private char openDoubleQuote = Instance.getTrans().getChar(
			StringId.OPEN_DOUBLE_QUOTE);
	private char closeDoubleQuote = Instance.getTrans().getChar(
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
	 * Return the story title.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the title
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract String getTitle(URL source, InputStream in)
			throws IOException;

	/**
	 * Return the story author.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the author
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract String getAuthor(URL source, InputStream in)
			throws IOException;

	/**
	 * Return the story publication date.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the date
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract String getDate(URL source, InputStream in)
			throws IOException;

	/**
	 * Return the subject of the story (for instance, if it is a fanfiction,
	 * what is the original work; if it is a technical text, what is the
	 * technical subject...).
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the subject
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract String getSubject(URL source, InputStream in)
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
	 * Return the story cover resource if any, or NULL if none.
	 * <p>
	 * The default cover should not be checked for here.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the cover or NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract URL getCover(URL source, InputStream in)
			throws IOException;

	/**
	 * Return the list of chapters (name and resource).
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the chapters
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract List<Entry<String, URL>> getChapters(URL source,
			InputStream in) throws IOException;

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
	 * 
	 * @return the content
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract String getChapterContent(URL source, InputStream in,
			int number) throws IOException;

	/**
	 * Check if this {@link BasicSupport} is mainly catered to image files.
	 * 
	 * @return TRUE if it is
	 */
	public boolean isImageDocument(URL source, InputStream in)
			throws IOException {
		return false;
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
		return processMeta(url, true, false);
	}

	/**
	 * Process the given story resource into a partially filled {@link Story}
	 * object containing the name and metadata.
	 * 
	 * @param url
	 *            the story resource
	 * 
	 * @param close
	 *            close "this" and "in" when done
	 * 
	 * @return the {@link Story}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected Story processMeta(URL url, boolean close, boolean getDesc)
			throws IOException {
		in = Instance.getCache().open(url, this, false);
		if (in == null) {
			return null;
		}

		try {
			preprocess(getInput());

			Story story = new Story();
			story.setMeta(new MetaData());
			story.getMeta().setTitle(ifUnhtml(getTitle(url, getInput())));
			story.getMeta().setAuthor(
					fixAuthor(ifUnhtml(getAuthor(url, getInput()))));
			story.getMeta().setDate(ifUnhtml(getDate(url, getInput())));
			story.getMeta().setTags(getTags(url, getInput()));
			story.getMeta().setSource(getSourceName());
			story.getMeta().setPublisher(
					ifUnhtml(getPublisher(url, getInput())));
			story.getMeta().setUuid(getUuid(url, getInput()));
			story.getMeta().setLuid(getLuid(url, getInput()));
			story.getMeta().setLang(getLang(url, getInput()));
			story.getMeta().setSubject(ifUnhtml(getSubject(url, getInput())));
			story.getMeta().setImageDocument(isImageDocument(url, getInput()));

			if (getDesc) {
				String descChapterName = Instance.getTrans().getString(
						StringId.DESCRIPTION);
				story.getMeta().setResume(
						makeChapter(url, 0, descChapterName,
								getDesc(url, getInput())));
			}

			return story;
		} finally {
			if (close) {
				try {
					close();
				} catch (IOException e) {
					Instance.syserr(e);
				}

				if (in != null) {
					in.close();
				}
			}
		}
	}

	/**
	 * Process the given story resource into a fully filled {@link Story}
	 * object.
	 * 
	 * @param url
	 *            the story resource
	 * 
	 * @return the {@link Story}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Story process(URL url) throws IOException {
		setCurrentReferer(url);

		try {
			Story story = processMeta(url, false, true);
			if (story == null) {
				return null;
			}

			story.setChapters(new ArrayList<Chapter>());

			URL cover = getCover(url, getInput());
			if (cover == null) {
				String subject = story.getMeta() == null ? null : story
						.getMeta().getSubject();
				if (subject != null && !subject.isEmpty()
						&& Instance.getCoverDir() != null) {
					File fileCover = new File(Instance.getCoverDir(), subject);
					cover = getImage(fileCover.toURI().toURL(), subject);
				}
			}

			if (cover != null) {
				InputStream coverIn = null;
				try {
					coverIn = Instance.getCache().open(cover, this, true);
					story.getMeta().setCover(StringUtils.toImage(coverIn));
				} catch (IOException e) {
					Instance.syserr(new IOException(Instance.getTrans()
							.getString(StringId.ERR_BS_NO_COVER, cover), e));
				} finally {
					if (coverIn != null)
						coverIn.close();
				}
			}

			List<Entry<String, URL>> chapters = getChapters(url, getInput());
			int i = 1;
			if (chapters != null) {
				for (Entry<String, URL> chap : chapters) {
					setCurrentReferer(chap.getValue());
					InputStream chapIn = Instance.getCache().open(
							chap.getValue(), this, true);
					try {
						story.getChapters().add(
								makeChapter(url, i, chap.getKey(),
										getChapterContent(url, chapIn, i)));
					} finally {
						chapIn.close();
					}
					i++;
				}
			}

			return story;

		} finally {
			try {
				close();
			} catch (IOException e) {
				Instance.syserr(e);
			}

			if (in != null) {
				in.close();
			}

			currentReferer = null;
		}
	}

	/**
	 * The support type.$
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
	 * Return the story publisher (by default,
	 * {@link BasicSupport#getSourceName()}).
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the publisher
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected String getPublisher(URL source, InputStream in)
			throws IOException {
		return getSourceName();
	}

	/**
	 * Return the story UUID, a unique value representing the story (it is often
	 * an URL).
	 * <p>
	 * By default, this is the {@link URL} of the resource.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the uuid
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected String getUuid(URL source, InputStream in) throws IOException {
		return source.toString();
	}

	/**
	 * Return the story Library UID, a unique value representing the story (it
	 * is often a number) in the local library.
	 * <p>
	 * By default, this is empty.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the id
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected String getLuid(URL source, InputStream in) throws IOException {
		return "";
	}

	/**
	 * Return the 2-letter language code of this story.
	 * <p>
	 * By default, this is 'EN'.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the language
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected String getLang(URL source, InputStream in) throws IOException {
		return "EN";
	}

	/**
	 * Return the list of tags for this story.
	 * 
	 * @param source
	 *            the source of the story
	 * @param in
	 *            the input (the main resource)
	 * 
	 * @return the tags
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected List<String> getTags(URL source, InputStream in)
			throws IOException {
		return new ArrayList<String>();
	}

	/**
	 * Return the first line from the given input which correspond to the given
	 * selectors.
	 * <p>
	 * Do not reset the input, which will be pointing at the line just after the
	 * result (input will be spent if no result is found).
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
	protected String getLine(InputStream in, String needle, int relativeLine) {
		return getLine(in, needle, relativeLine, true);
	}

	/**
	 * Return a line from the given input which correspond to the given
	 * selectors.
	 * <p>
	 * Do not reset the input, which will be pointing at the line just after the
	 * result (input will be spent if no result is found) when first is TRUE,
	 * and will always be spent if first is FALSE.
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
	protected String getLine(InputStream in, String needle, int relativeLine,
			boolean first) {
		String rep = null;

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
	 * Prepare the support if needed before processing.
	 * 
	 * @throws IOException
	 *             on I/O error
	 */
	protected void preprocess(InputStream in) throws IOException {
	}

	/**
	 * Now that we have processed the {@link Story}, close the resources if any.
	 * 
	 * @throws IOException
	 *             on I/O error
	 */
	protected void close() throws IOException {
	}

	/**
	 * Create a {@link Chapter} object from the given information, formatting
	 * the content as it should be.
	 * 
	 * @param number
	 *            the chapter number
	 * @param name
	 *            the chapter name
	 * @param content
	 *            the chapter content
	 * 
	 * @return the {@link Chapter}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected Chapter makeChapter(URL source, int number, String name,
			String content) throws IOException {

		// Chapter name: process it correctly, then remove the possible
		// redundant "Chapter x: " in front of it
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

		if (chapterName.startsWith(":")) {
			chapterName = chapterName.substring(1).trim();
		}
		//

		Chapter chap = new Chapter(number, chapterName);

		if (content == null) {
			return chap;
		}

		if (isHtml()) {
			// Special <HR> processing:
			content = content.replaceAll("(<hr [^>]*>)|(<hr/>)|(<hr>)",
					"\n* * *\n");
		}

		InputStream in = new ByteArrayInputStream(content.getBytes("UTF-8"));
		try {
			@SuppressWarnings("resource")
			Scanner scan = new Scanner(in, "UTF-8");
			scan.useDelimiter("(\\n|</p>)"); // \n for test, </p> for html

			List<Paragraph> paras = new ArrayList<Paragraph>();
			while (scan.hasNext()) {
				String line = scan.next().trim();
				boolean image = false;
				if (line.startsWith("[") && line.endsWith("]")) {
					URL url = getImage(source,
							line.substring(1, line.length() - 1).trim());
					if (url != null) {
						paras.add(new Paragraph(url));
						image = true;
					}
				}

				if (!image) {
					paras.add(processPara(line));
				}
			}

			// Check quotes for "bad" format
			List<Paragraph> newParas = new ArrayList<Paragraph>();
			for (Paragraph para : paras) {
				newParas.addAll(requotify(para));
			}
			paras = newParas;

			// Remove double blanks/brks
			boolean space = false;
			boolean brk = true;
			for (int i = 0; i < paras.size(); i++) {
				Paragraph para = paras.get(i);
				boolean thisSpace = para.getType() == ParagraphType.BLANK;
				boolean thisBrk = para.getType() == ParagraphType.BREAK;

				if (space && thisBrk) {
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
					&& (paras.get(0).getType() == ParagraphType.BLANK || paras
							.get(0).getType() == ParagraphType.BREAK)) {
				paras.remove(0);
			}

			// Remove blank/brk at end
			int last = paras.size() - 1;
			if (paras.size() > 0
					&& (paras.get(last).getType() == ParagraphType.BLANK || paras
							.get(last).getType() == ParagraphType.BREAK)) {
				paras.remove(last);
			}

			chap.setParagraphs(paras);

			return chap;
		} finally {
			in.close();
		}
	}

	/**
	 * Return the list of supported image extensions.
	 * 
	 * @return the extensions
	 */
	protected String[] getImageExt(boolean emptyAllowed) {
		if (emptyAllowed) {
			return new String[] { "", ".png", ".jpg", ".jpeg", ".gif", ".bmp" };
		} else {
			return new String[] { ".png", ".jpg", ".jpeg", ".gif", ".bmp" };
		}
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
	protected URL getImage(URL source, String line) {
		String path = new File(source.getFile()).getParent();
		URL url = null;

		// try for files
		try {
			String urlBase = new File(new File(path), line.trim()).toURI()
					.toURL().toString();
			for (String ext : getImageExt(true)) {
				if (new File(urlBase + ext).exists()) {
					url = new File(urlBase + ext).toURI().toURL();
				}
			}
		} catch (Exception e) {
			// Nothing to do here
		}

		if (url == null) {
			// try for URLs
			try {
				for (String ext : getImageExt(true)) {
					if (Instance.getCache().check(new URL(line + ext))) {
						url = new URL(line + ext);
					}
				}

				// try out of cache
				if (url == null) {
					for (String ext : getImageExt(true)) {
						try {
							url = new URL(line + ext);
							Instance.getCache().refresh(url, this, true);
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
				Instance.getCache().refresh(url, this, true);
			} catch (IOException e) {
				// woops, broken image
				url = null;
			}
		}

		return url;
	}

	/**
	 * Reset then return {@link BasicSupport#in}.
	 * 
	 * @return {@link BasicSupport#in}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected InputStream getInput() throws IOException {
		in.reset();
		return in;
	}

	/**
	 * Fix the author name if it is prefixed with some "by" {@link String}.
	 * 
	 * @param author
	 *            the author with a possible prefix
	 * 
	 * @return the author without prefixes
	 */
	private String fixAuthor(String author) {
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
	 *            the paragraph to requotify (not necessaraly a quote)
	 * 
	 * @return the correctly (or so we hope) quotified paragraphs
	 */
	private List<Paragraph> requotify(Paragraph para) {
		List<Paragraph> newParas = new ArrayList<Paragraph>();

		if (para.getType() == ParagraphType.QUOTE) {
			String line = para.getContent();
			boolean singleQ = line.startsWith("" + openQuote);
			boolean doubleQ = line.startsWith("" + openDoubleQuote);

			if (!singleQ && !doubleQ) {
				line = openDoubleQuote + line + closeDoubleQuote;
				newParas.add(new Paragraph(ParagraphType.QUOTE, line));
			} else {
				char close = singleQ ? closeQuote : closeDoubleQuote;
				int posClose = line.indexOf(close);
				int posDot = line.indexOf(".");
				while (posDot >= 0 && posDot < posClose) {
					posDot = line.indexOf(".", posDot + 1);
				}

				if (posDot >= 0) {
					String rest = line.substring(posDot + 1).trim();
					line = line.substring(0, posDot + 1).trim();
					newParas.add(new Paragraph(ParagraphType.QUOTE, line));
					newParas.addAll(requotify(processPara(rest)));
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
	private Paragraph processPara(String line) {
		line = ifUnhtml(line).trim();

		boolean space = true;
		boolean brk = true;
		boolean quote = false;
		boolean tentativeCloseQuote = false;
		char prev = '\0';
		int dashCount = 0;

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
				if ((car >= 'a' && car <= 'z') || (car >= 'A' && car <= 'Z')
						|| (car >= '0' && car <= '9')) {
					builder.append("'");
				} else {
					builder.append(closeQuote);
				}
			}

			switch (car) {
			case ' ': // note: unbreakable space
			case ' ':
			case '\t':
			case '\n': // just in case
			case '\r': // just in case
				builder.append(' ');
				break;

			case '\'':
				if (space || (brk && quote)) {
					quote = true;
					builder.append(openQuote);
				} else if (prev == ' ') {
					builder.append(openQuote);
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
					builder.append(openQuote);
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
				builder.append(closeQuote);
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

		return new Paragraph(type, line);
	}

	/**
	 * Remove the HTML from the inpit <b>if</b> {@link BasicSupport#isHtml()} is
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

		for (SupportType type : new SupportType[] { SupportType.TEXT,
				SupportType.INFO_TEXT }) {
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
			return new Fimfiction().setType(type);
		case FANFICTION:
			return new Fanfiction().setType(type);
		case TEXT:
			return new Text().setType(type);
		case MANGAFOX:
			return new MangaFox().setType(type);
		case E621:
			return new E621().setType(type);
		case CBZ:
			return new Cbz().setType(type);
		}

		return null;
	}
}
