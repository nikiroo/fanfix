package be.nikiroo.fanfix.supported;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;

/**
 * Support class for local stories encoded in textual format, with a few rules:
 * <ul>
 * <li>The title must be on the first line</li>
 * <li>The author (preceded by nothing, "by " or "©") must be on the second
 * line, possibly with the publication date in parenthesis (i.e., "
 * <tt>By Unknown (3rd October 1998)</tt>")</li>
 * <li>Chapters must be declared with "<tt>Chapter x</tt>" or "
 * <tt>Chapter x: NAME OF THE CHAPTER</tt>", where "<tt>x</tt>" is the chapter
 * number</li>
 * <li>A description of the story must be given as chapter number 0</li>
 * <li>A cover may be present, with the same filename but a PNG, JPEG or JPG
 * extension</li<
 * </ul>
 * 
 * @author niki
 */
class Text extends BasicSupport {
	@Override
	protected boolean isHtml() {
		return false;
	}

	@Override
	public String getSourceName() {
		return "text";
	}

	@Override
	protected MetaData getMeta(URL source, InputStream in) throws IOException {
		MetaData meta = new MetaData();

		meta.setTitle(getTitle(reset(in)));
		meta.setAuthor(getAuthor(reset(in)));
		meta.setDate(getDate(reset(in)));
		meta.setTags(new ArrayList<String>());
		meta.setSource(getSourceName());
		meta.setUrl(source.toString());
		meta.setPublisher("");
		meta.setUuid(source.toString());
		meta.setLuid("");
		meta.setLang(getLang(source, reset(in))); // default is EN
		meta.setSubject(getSubject(source));
		meta.setType(getType().toString());
		meta.setImageDocument(false);
		meta.setCover(getCover(source));

		return meta;
	}

	private String getSubject(URL source) throws IOException {
		try {
			File file = new File(source.toURI());
			return file.getParentFile().getName();
		} catch (URISyntaxException e) {
			throw new IOException("Cannot parse the URL to File: "
					+ source.toString(), e);
		}

	}

	private String getLang(URL source, InputStream in) throws IOException {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		scan.next(); // Title
		scan.next(); // Author (Date)
		String chapter0 = scan.next(); // empty or Chapter 0
		while (chapter0.isEmpty()) {
			chapter0 = scan.next();
		}

		String lang = detectChapter(chapter0, 0);
		if (lang == null) {
			// No description??
			lang = detectChapter(chapter0, 1);
		}

		if (lang == null) {
			lang = "EN";
		} else {
			lang = lang.toUpperCase();
		}

		return lang;
	}

	private String getTitle(InputStream in) throws IOException {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		return scan.next();
	}

	private String getAuthor(InputStream in) throws IOException {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		scan.next();
		String authorDate = scan.next();

		String author = authorDate;
		int pos = authorDate.indexOf('(');
		if (pos >= 0) {
			author = authorDate.substring(0, pos);
		}

		return fixAuthor(author);
	}

	private String getDate(InputStream in) throws IOException {
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		scan.next();
		String authorDate = scan.next();

		String date = "";
		int pos = authorDate.indexOf('(');
		if (pos >= 0) {
			date = authorDate.substring(pos + 1).trim();
			pos = date.lastIndexOf(')');
			if (pos >= 0) {
				date = date.substring(0, pos).trim();
			}
		}

		return date;
	}

	@Override
	protected String getDesc(URL source, InputStream in) throws IOException {
		return getChapterContent(source, in, 0);
	}

	private BufferedImage getCover(URL source) throws IOException {
		String path;
		try {
			path = new File(source.toURI()).getPath();
		} catch (URISyntaxException e) {
			Instance.syserr(e);
			path = null;
		}

		for (String ext : new String[] { ".txt", ".text", ".story" }) {
			if (path.endsWith(ext)) {
				path = path.substring(0, path.length() - ext.length());
			}
		}

		return getImage(this, source, path);
	}

	@Override
	protected List<Entry<String, URL>> getChapters(URL source, InputStream in)
			throws IOException {
		List<Entry<String, URL>> chaps = new ArrayList<Entry<String, URL>>();
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		boolean prevLineEmpty = false;
		while (scan.hasNext()) {
			String line = scan.next();
			if (prevLineEmpty && detectChapter(line, chaps.size() + 1) != null) {
				String chapName = Integer.toString(chaps.size() + 1);
				int pos = line.indexOf(':');
				if (pos >= 0 && pos + 1 < line.length()) {
					chapName = line.substring(pos + 1).trim();
				}
				final URL value = source;
				final String key = chapName;
				chaps.add(new Entry<String, URL>() {
					public URL setValue(URL value) {
						return null;
					}

					public URL getValue() {
						return value;
					}

					public String getKey() {
						return key;
					}
				});
			}

			prevLineEmpty = line.trim().isEmpty();
		}

		return chaps;
	}

	@Override
	protected String getChapterContent(URL source, InputStream in, int number)
			throws IOException {
		StringBuilder builder = new StringBuilder();
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		boolean inChap = false;
		while (scan.hasNext()) {
			String line = scan.next();
			if (detectChapter(line, number) != null) {
				inChap = true;
			} else if (inChap && detectChapter(line, number + 1) != null) {
				break;
			} else if (inChap) {
				builder.append(line);
				builder.append("\n");
			}
		}

		return builder.toString();
	}

	@Override
	protected boolean supports(URL url) {
		if ("file".equals(url.getProtocol())) {
			File file;
			try {
				file = new File(url.toURI());
				file = new File(file.getPath() + ".info");
			} catch (URISyntaxException e) {
				Instance.syserr(e);
				file = null;
			}

			return file == null || !file.exists();
		}

		return false;
	}

	/**
	 * Check if the given line looks like the given starting chapter in a
	 * supported language, and return the language if it does (or NULL if not).
	 * 
	 * @param line
	 *            the line to check
	 * 
	 * @return the language or NULL
	 */
	private String detectChapter(String line, int number) {
		line = line.toUpperCase();
		for (String lang : Instance.getConfig().getString(Config.CHAPTER)
				.split(",")) {
			String chapter = Instance.getConfig().getStringX(Config.CHAPTER,
					lang);
			if (chapter != null && !chapter.isEmpty()) {
				chapter = chapter.toUpperCase() + " ";
				if (line.startsWith(chapter)) {
					// We want "[CHAPTER] [number]: [name]", with ": [name]"
					// optional
					String test = line.substring(chapter.length()).trim();
					if (test.startsWith(Integer.toString(number))) {
						test = test
								.substring(Integer.toString(number).length())
								.trim();
						if (test.isEmpty() || test.startsWith(":")) {
							return lang;
						}
					}
				}
			}
		}

		return null;
	}
}
