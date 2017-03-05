package be.nikiroo.fanfix.supported;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;

/**
 * Support class for <a href="http://e621.net/">e621.net</a> and <a
 * href="http://e926.net/">e926.net</a>, a Furry website supporting comics,
 * including some of MLP.
 * <p>
 * <a href="http://e926.net/">e926.net</a> only shows the "clean" images and
 * comics, but it can be difficult to browse.
 * 
 * @author niki
 */
class E621 extends BasicSupport {
	@Override
	public String getSourceName() {
		return "e621.net";
	}

	@Override
	protected MetaData getMeta(URL source, InputStream in) throws IOException {
		MetaData meta = new MetaData();

		meta.setTitle(getTitle(reset(in)));
		meta.setAuthor(getAuthor(source, reset(in)));
		meta.setDate("");
		meta.setTags(new ArrayList<String>()); // TODDO ???
		meta.setSource(getSourceName());
		meta.setUrl(source.toString());
		meta.setPublisher(getSourceName());
		meta.setUuid(source.toString());
		meta.setLuid("");
		meta.setLang("EN");
		meta.setSubject("Furry");
		meta.setType(getType().toString());
		meta.setImageDocument(true);
		meta.setCover(getCover(source));
		meta.setFakeCover(true);

		return meta;
	}

	@Override
	public Story process(URL url, Progress pg) throws IOException {
		// There is no chapters on e621, just pagination...
		Story story = super.process(url, pg);

		Chapter only = new Chapter(1, null);
		for (Chapter chap : story) {
			only.getParagraphs().addAll(chap.getParagraphs());
		}

		story.getChapters().clear();
		story.getChapters().add(only);

		return story;
	}

	@Override
	protected boolean supports(URL url) {
		String host = url.getHost();
		if (host.startsWith("www.")) {
			host = host.substring("www.".length());
		}

		return ("e621.net".equals(host) || "e926.net".equals(host))
				&& url.getPath().startsWith("/pool/");
	}

	@Override
	protected boolean isHtml() {
		return true;
	}

	private BufferedImage getCover(URL source) throws IOException {
		InputStream in = Instance.getCache().open(source, this, true);
		String images = getChapterContent(new URL(source.toString() + "?page="
				+ 1), in, 1);
		if (!images.isEmpty()) {
			int pos = images.indexOf("<br/>");
			if (pos >= 0) {
				images = images.substring(1, pos - 1);
				return getImage(this, null, images);
			}
		}

		return null;
	}

	private String getAuthor(URL source, InputStream in) throws IOException {
		String author = getLine(in, "href=\"/post/show/", 0);
		if (author != null) {
			String key = "href=\"";
			int pos = author.indexOf(key);
			if (pos >= 0) {
				author = author.substring(pos + key.length());
				pos = author.indexOf("\"");
				if (pos >= 0) {
					author = author.substring(0, pos - 1);
					String page = source.getProtocol() + "://"
							+ source.getHost() + author;
					try {
						InputStream pageIn = Instance.getCache().open(
								new URL(page), this, false);
						try {
							key = "class=\"tag-type-artist\"";
							author = getLine(pageIn, key, 0);
							if (author != null) {
								pos = author.indexOf("<a href=\"");
								if (pos >= 0) {
									author = author.substring(pos);
									pos = author.indexOf("</a>");
									if (pos >= 0) {
										author = author.substring(0, pos);
										return StringUtils.unhtml(author);
									}
								}
							}
						} finally {
							pageIn.close();
						}
					} catch (Exception e) {
						// No author found
					}
				}
			}
		}

		return null;
	}

	private String getTitle(InputStream in) throws IOException {
		String title = getLine(in, "<title>", 0);
		if (title != null) {
			int pos = title.indexOf('>');
			if (pos >= 0) {
				title = title.substring(pos + 1);
				pos = title.indexOf('<');
				if (pos >= 0) {
					title = title.substring(0, pos);
				}
			}

			if (title.startsWith("Pool:")) {
				title = title.substring("Pool:".length());
			}

			title = StringUtils.unhtml(title).trim();
		}

		return title;
	}

	@Override
	protected String getDesc(URL source, InputStream in) throws IOException {
		String desc = getLine(in, "margin-bottom: 2em;", 0);

		if (desc != null) {
			StringBuilder builder = new StringBuilder();

			boolean inTags = false;
			for (char car : desc.toCharArray()) {
				if ((inTags && car == '>') || (!inTags && car == '<')) {
					inTags = !inTags;
				}

				if (inTags) {
					builder.append(car);
				}
			}

			return builder.toString().trim();
		}

		return null;
	}

	@Override
	protected List<Entry<String, URL>> getChapters(URL source, InputStream in)
			throws IOException {
		List<Entry<String, URL>> urls = new ArrayList<Entry<String, URL>>();
		int last = 1; // no pool/show when only one page

		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		while (scan.hasNext()) {
			String line = scan.next();
			for (int pos = line.indexOf(source.getPath()); pos >= 0; pos = line
					.indexOf(source.getPath(), pos + source.getPath().length())) {
				int equalPos = line.indexOf("=", pos);
				int quotePos = line.indexOf("\"", pos);
				if (equalPos >= 0 && quotePos > equalPos) {
					String snum = line.substring(equalPos + 1, quotePos);
					try {
						int num = Integer.parseInt(snum);
						if (num > last) {
							last = num;
						}
					} catch (NumberFormatException e) {
					}
				}
			}
		}

		for (int i = 1; i <= last; i++) {
			final String key = Integer.toString(i);
			final URL value = new URL(source.toString() + "?page=" + i);
			urls.add(new Entry<String, URL>() {
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

		return urls;
	}

	@Override
	protected String getChapterContent(URL source, InputStream in, int number)
			throws IOException {
		StringBuilder builder = new StringBuilder();
		String staticSite = "https://static1.e621.net";
		if (source.getHost().contains("e926")) {
			staticSite = staticSite.replace("e621", "e926");
		}

		String key = staticSite + "/data/preview/";

		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		while (scan.hasNext()) {
			String line = scan.next();
			if (line.contains("class=\"preview")) {
				for (int pos = line.indexOf(key); pos >= 0; pos = line.indexOf(
						key, pos + key.length())) {
					int endPos = line.indexOf("\"", pos);
					if (endPos >= 0) {
						String id = line.substring(pos + key.length(), endPos);
						id = staticSite + "/data/" + id;

						int dotPos = id.lastIndexOf(".");
						if (dotPos >= 0) {
							id = id.substring(0, dotPos);
							builder.append("[");
							builder.append(id);
							builder.append("]<br/>");
						}
					}
				}
			}
		}

		return builder.toString();
	}
}
