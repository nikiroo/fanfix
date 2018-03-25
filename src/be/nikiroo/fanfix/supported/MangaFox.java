package be.nikiroo.fanfix.supported;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;

class MangaFox extends BasicSupport {
	@Override
	protected boolean isHtml() {
		return true;
	}

	@Override
	public String getSourceName() {
		return "MangaFox.me";
	}

	@Override
	protected MetaData getMeta() throws IOException {
		MetaData meta = new MetaData();
		Element doc = getSourceNode();

		Element title = doc.getElementById("title");
		Elements table = null;
		if (title != null) {
			table = title.getElementsByTag("table");
		}
		if (table != null) {
			// Rows: header, data
			Elements rows = table.first().getElementsByTag("tr");
			if (rows.size() > 1) {
				table = rows.get(1).getElementsByTag("td");
				// Columns: Realeased, Authors, Artists, Genres
				if (table.size() < 4) {
					table = null;
				}
			}
		}

		meta.setTitle(getTitle());
		if (table != null) {
			meta.setAuthor(getAuthors(table.get(1).text() + ","
					+ table.get(2).text()));

			meta.setDate(StringUtils.unhtml(table.get(0).text()).trim());
			meta.setTags(explode(table.get(3).text()));
		}
		meta.setSource(getSourceName());
		meta.setUrl(getSource().toString());
		meta.setPublisher(getSourceName());
		meta.setUuid(getSource().toString());
		meta.setLuid("");
		meta.setLang("en");
		meta.setSubject("manga");
		meta.setType(getType().toString());
		meta.setImageDocument(true);
		meta.setCover(getCover());

		return meta;
	}

	private String getTitle() {
		Element doc = getSourceNode();

		Element title = doc.getElementById("title");
		Element h1 = title.getElementsByTag("h1").first();
		if (h1 != null) {
			return StringUtils.unhtml(h1.text()).trim();
		}

		return null;
	}

	private String getAuthors(String authorList) {
		String author = "";
		for (String auth : explode(authorList)) {
			if (!author.isEmpty()) {
				author = author + ", ";
			}
			author += auth;
		}

		return author;
	}

	@Override
	protected String getDesc() {
		Element doc = getSourceNode();
		Element title = doc.getElementsByClass("summary").first();
		if (title != null) {
			StringUtils.unhtml(title.text()).trim();
		}

		return null;
	}

	private Image getCover() {
		Element doc = getSourceNode();
		Element cover = doc.getElementsByClass("cover").first();
		if (cover != null) {
			cover = cover.getElementsByTag("img").first();
		}

		if (cover != null) {
			String coverUrl = cover.absUrl("src");

			InputStream coverIn;
			try {
				coverIn = openEx(coverUrl);
				try {
					return new Image(coverIn);
				} finally {
					coverIn.close();
				}
			} catch (IOException e) {
				Instance.getTraceHandler().error(e);
			}
		}

		return null;
	}

	@Override
	protected List<Entry<String, URL>> getChapters(Progress pg) {
		List<Entry<String, URL>> urls = new ArrayList<Entry<String, URL>>();

		String prefix = null; // each chapter starts with this prefix, then a
								// chapter number (including "x.5"), then name

		Element doc = getSourceNode();
		for (Element li : doc.getElementsByTag("li")) {
			Element el = li.getElementsByTag("h4").first();
			if (el == null) {
				el = li.getElementsByTag("h3").first();
			}
			if (el != null) {
				Element a = el.getElementsByTag("a").first();
				if (a != null) {
					String title = StringUtils.unhtml(el.text()).trim();
					try {
						String url = a.absUrl("href");
						if (url.endsWith("1.html")) {
							url = url.substring(0,
									url.length() - "1.html".length());
						}
						if (!url.endsWith("/")) {
							url += "/";
						}

						if (prefix == null || !prefix.isEmpty()) {
							StringBuilder possiblePrefix = new StringBuilder(
									StringUtils.unhtml(a.text()).trim());
							while (possiblePrefix.length() > 0) {
								char car = possiblePrefix.charAt(possiblePrefix
										.length() - 1);
								boolean punctuation = (car == '.' || car == ' ');
								boolean digit = (car >= '0' && car <= '9');
								if (!punctuation && !digit) {
									break;
								}

								possiblePrefix.setLength(possiblePrefix
										.length() - 1);
							}

							if (prefix == null) {
								prefix = possiblePrefix.toString();
							}

							if (!prefix.equalsIgnoreCase(possiblePrefix
									.toString())) {
								prefix = ""; // prefix not ok
							}
						}

						urls.add(new AbstractMap.SimpleEntry<String, URL>(
								title, new URL(url)));
					} catch (Exception e) {
						Instance.getTraceHandler().error(e);
					}
				}
			}
		}

		if (prefix != null && !prefix.isEmpty()) {
			try {
				// We found a prefix, so everything should be sortable
				SortedMap<Double, Entry<String, URL>> map = new TreeMap<Double, Entry<String, URL>>();
				for (Entry<String, URL> entry : urls) {
					String num = entry.getKey().substring(prefix.length() + 1)
							.trim();
					String name = "";
					int pos = num.indexOf(' ');
					if (pos >= 0) {
						name = num.substring(pos).trim();
						num = num.substring(0, pos).trim();
					}

					if (!name.isEmpty()) {
						name = "Tome " + num + ": " + name;
					} else {
						name = "Tome " + num;
					}

					double key = Double.parseDouble(num);

					map.put(key, new AbstractMap.SimpleEntry<String, URL>(name,
							entry.getValue()));
				}
				urls = new ArrayList<Entry<String, URL>>(map.values());
			} catch (NumberFormatException e) {
				Instance.getTraceHandler()
						.error(new IOException(
								"Cannot find a tome number, revert to default sorting",
								e));
				// by default, the chapters are in reversed order
				Collections.reverse(urls);
			}
		} else {
			// by default, the chapters are in reversed order
			Collections.reverse(urls);
		}

		return urls;
	}

	@Override
	protected String getChapterContent(URL chapUrl, int number, Progress pg)
			throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		StringBuilder builder = new StringBuilder();

		String url = chapUrl.toString();
		InputStream imageIn = null;
		Element imageDoc = null;

		// 1. find out how many images there are
		int size;
		try {
			// note: when used, the base URL can be an ad-page
			imageIn = openEx(url + "1.html");
			imageDoc = DataUtil.load(imageIn, "UTF-8", url + "1.html");
		} catch (IOException e) {
			Instance.getTraceHandler().error(
					new IOException("Cannot get image " + 1 + " of manga", e));
		} finally {
			if (imageIn != null) {
				imageIn.close();
			}
		}
		Element select = imageDoc.getElementsByClass("m").first();
		Elements options = select.getElementsByTag("option");
		size = options.size() - 1; // last is "Comments"

		pg.setMinMax(0, size);

		// 2. list them
		for (int i = 1; i <= size; i++) {
			if (i > 1) { // because first one was opened for size
				try {
					imageIn = openEx(url + i + ".html");
					imageDoc = DataUtil.load(imageIn, "UTF-8", url + i
							+ ".html");

					String linkImage = imageDoc.getElementById("image").absUrl(
							"src");
					if (linkImage != null) {
						builder.append("[");
						// to help with the retry and the originalUrl, part 1
						builder.append(withoutQuery(linkImage));
						builder.append("]<br/>");
					}

					// to help with the retry and the originalUrl, part 2
					refresh(linkImage);
				} catch (IOException e) {
					Instance.getTraceHandler().error(
							new IOException("Cannot get image " + i
									+ " of manga", e));
				} finally {
					if (imageIn != null) {
						imageIn.close();
					}
				}
			}
		}

		return builder.toString();
	}

	/**
	 * Refresh the {@link URL} by calling {@link MangaFox#openEx(String)}.
	 * 
	 * @param url
	 *            the URL to refresh
	 * 
	 * @return TRUE if it was refreshed
	 */
	private boolean refresh(String url) {
		try {
			openEx(url).close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Open the URL through the cache, but: retry a second time after 100ms if
	 * it fails, remove the query part of the {@link URL} before saving it to
	 * the cache (so it can be recalled later).
	 * 
	 * @param url
	 *            the {@link URL}
	 * 
	 * @return the resource
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private InputStream openEx(String url) throws IOException {
		try {
			return Instance.getCache().open(new URL(url), this, true,
					withoutQuery(url));
		} catch (Exception e) {
			// second chance
			try {
				Thread.sleep(100);
			} catch (InterruptedException ee) {
			}

			return Instance.getCache().open(new URL(url), this, true,
					withoutQuery(url));
		}
	}

	/**
	 * Return the same input {@link URL} but without the query part.
	 * 
	 * @param url
	 *            the inpiut {@link URL} as a {@link String}
	 * 
	 * @return the input {@link URL} without query
	 */
	private URL withoutQuery(String url) {
		URL o = null;
		try {
			// Remove the query from o (originalUrl), so it can be cached
			// correctly
			o = new URL(url);
			o = new URL(o.getProtocol() + "://" + o.getHost() + o.getPath());

			return o;
		} catch (MalformedURLException e) {
			return null;
		}
	}

	/**
	 * Explode an HTML comma-separated list of values into a non-duplicate text
	 * {@link List} .
	 * 
	 * @param values
	 *            the comma-separated values in HTML format
	 * 
	 * @return the full list with no duplicate in text format
	 */
	private List<String> explode(String values) {
		List<String> list = new ArrayList<String>();
		if (values != null && !values.isEmpty()) {
			for (String auth : values.split(",")) {
				String a = StringUtils.unhtml(auth).trim();
				if (!a.isEmpty() && !list.contains(a.trim())) {
					list.add(a);
				}
			}
		}

		return list;
	}

	@Override
	protected boolean supports(URL url) {
		return "mangafox.me".equals(url.getHost())
				|| "www.mangafox.me".equals(url.getHost())
				|| "fanfox.net".equals(url.getHost())
				|| "www.fanfox.net".equals(url.getHost());
	}
}
