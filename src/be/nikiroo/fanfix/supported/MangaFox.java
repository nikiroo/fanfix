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

import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

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
	protected MetaData getMeta() throws IOException {
		MetaData meta = new MetaData();

		meta.setTitle(getTitle());
		// No date anymore on mangafox
		// meta.setDate();
		meta.setAuthor(getAuthor());
		meta.setTags(getTags());
		meta.setSource(getType().getSourceName());
		meta.setUrl(getSource().toString());
		meta.setPublisher(getType().getSourceName());
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

		Element el = doc.getElementsByClass("detail-info-right-title-font").first();
		if (el != null) {
			return StringUtils.unhtml(el.text()).trim();
		}

		return null;
	}

	private String getAuthor() {
		StringBuilder builder = new StringBuilder();
		for (String author : getListA("detail-info-right-say")) {
			if (builder.length() > 0)
				builder.append(", ");
			builder.append(author);
		}

		return builder.toString();
	}

	private List<String> getTags() {
		return getListA("detail-info-right-tag-list");
	}

	private List<String> getListA(String uniqueClass) {
		List<String> list = new ArrayList<String>();

		Element doc = getSourceNode();
		Element el = doc.getElementsByClass(uniqueClass).first();
		if (el != null) {
			for (Element valueA : el.getElementsByTag("a")) {
				list.add(StringUtils.unhtml(valueA.text()).trim());
			}
		}

		return list;
	}

	@Override
	protected String getDesc() {
		Element doc = getSourceNode();
		Element title = doc.getElementsByClass("fullcontent").first();
		if (title != null) {
			return StringUtils.unhtml(title.text()).trim();
		}

		return null;
	}

	private Image getCover() {
		Element doc = getSourceNode();
		Element cover = doc.getElementsByClass("detail-info-cover-img").first();
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

		String prefix = getTitle(); // each chapter starts with this prefix, then a
		// chapter number (including "x.5"), then name

		// normally, only one list...
		Element doc = getSourceNode();
		for (Element list : doc.getElementsByClass("detail-main-list")) {
			for (Element el : list.getElementsByTag("a")) {
				String title = el.attr("title");
				if (title.startsWith(prefix)) {
					title = title.substring(prefix.length()).trim();
				}

				String url = el.absUrl("href");

				try {
					urls.add(new AbstractMap.SimpleEntry<String, URL>(title, new URL(url)));
				} catch (Exception e) {
					Instance.getTraceHandler().error(e);
				}
			}
		}

		// by default, the chapters are in reversed order
		Collections.reverse(urls);

		return urls;
	}

	@Override
	protected String getChapterContent(URL chapUrl, int number, Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		StringBuilder builder = new StringBuilder();

		Document chapDoc = DataUtil.load(Instance.getCache().open(chapUrl, this, false), "UTF-8", chapUrl.toString());

		// Example of what we want:
		// URL: http://fanfox.net/manga/solo_leveling/c110.5/1.html#ipg1
		// IMAGE, not working:
		// http://s.fanfox.net/store/manga/29037/110.5/compressed/s034.jpg?token=f630767b0c96f6cc793fc8f1fc177c0ae9342eb1&amp;ttl=1585929600
		// IMAGE, working:
		// http://s.fanfox.net/store/manga/29037/000.0/compressed/m2018110o_143554_925.jpg?token=7d74569986335d49651ef1040f7dcb9dbd559b1b&ttl=1585929600
		// NOTE: (c110.5 -> 110.5, c000 -> 000.0)
		// NOTE: image key: m2018110o_143554_925 can be found in the script, but not
		// sorted

		// 0. Get the javascript content
		StringBuilder javascript = new StringBuilder();
		for (Element script : chapDoc.getElementsByTag("script")) {
			javascript.append(script.html());
			javascript.append("\n");
		}

		// 1. Get the chapter url part
		String chap = chapUrl.getPath();
		chap = chap.split("#")[0];
		if (chap.endsWith("/1.html")) {
			chap = chap.substring(0, chap.length() - "/1.html".length());
		}
		int pos = chap.lastIndexOf("/");
		chap = chap.substring(pos + 1);
		if (!chap.contains(".")) {
			chap = chap + ".0";
		}
		if (chap.startsWith("c")) {
			chap = chap.substring(1);
		}

		// 2. Token:
		// <meta name="og:image"
		// content="http://fmcdn.fanfox.net/store/manga/29037/cover.jpg?token=4b2056d83973716c715f2404940822dff942a7b4&ttl=1585998000&v=1584582495"
		Element el = chapDoc.select("meta[name=\"og:image\"]").first();
		String token = el.attr("content").split("\\?")[1];

		// 3. Comic ID
		int comicId = getIntVar(javascript, "comicid");

		// 4. Get images
		List<String> chapKeys = getImageKeys(javascript);
		// http://s.fanfox.net/store/manga/29037/000.0/compressed/m2018110o_143554_925.jpg?token=7d74569986335d49651ef1040f7dcb9dbd559b1b&ttl=1585929600
		String base = "http://s.fanfox.net/store/manga/%s/%s/compressed/%s.jpg?%s";
		for (String key : chapKeys) {
			String img = String.format(base, comicId, chap, key, token);
			builder.append("[");
			builder.append(img);
			builder.append("]<br/>");
		}

		return builder.toString();
	}

	private int getIntVar(StringBuilder builder, String var) {
		var = "var " + var;

		int pos = builder.indexOf(var) + var.length();
		String value = builder.subSequence(pos, pos + 20).toString();
		value = value.split("=")[1].trim();
		value = value.split(";")[0].trim();

		return Integer.parseInt(value);
	}

	private List<String> getImageKeys(StringBuilder builder) {
		List<String> chapKeys = new ArrayList<String>();

		String start = "|compressed|";
		String stop = ">";
		int pos = builder.indexOf(start) + start.length();
		int pos2 = builder.indexOf(stop, pos) - stop.length();

		String data = builder.substring(pos, pos2);
		data = data.replace("|", "'");
		for (String key : data.split("'")) {
			if (key.startsWith("m") && !key.equals("manga")) {
				chapKeys.add(key);
			}
		}

		Collections.sort(chapKeys);
		return chapKeys;
	}

	/**
	 * Open the URL through the cache, but: retry a second time after 100ms if it
	 * fails, remove the query part of the {@link URL} before saving it to the cache
	 * (so it can be recalled later).
	 * 
	 * @param url the {@link URL}
	 * 
	 * @return the resource
	 * 
	 * @throws IOException in case of I/O error
	 */
	private InputStream openEx(String url) throws IOException {
		try {
			return Instance.getCache().open(new URL(url), withoutQuery(url), this, true);
		} catch (Exception e) {
			// second chance
			try {
				Thread.sleep(100);
			} catch (InterruptedException ee) {
			}

			return Instance.getCache().open(new URL(url), withoutQuery(url), this, true);
		}
	}

	/**
	 * Return the same input {@link URL} but without the query part.
	 * 
	 * @param url the inpiut {@link URL} as a {@link String}
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

	@Override
	protected boolean supports(URL url) {
		return "mangafox.me".equals(url.getHost()) || "www.mangafox.me".equals(url.getHost())
				|| "fanfox.net".equals(url.getHost()) || "www.fanfox.net".equals(url.getHost());
	}
}
