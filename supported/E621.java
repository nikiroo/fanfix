package be.nikiroo.fanfix.supported;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;

/**
 * Support class for <a href="http://e621.net/">e621.net</a> and
 * <a href="http://e926.net/">e926.net</a>, a Furry website supporting comics,
 * including some of MLP.
 * <p>
 * <a href="http://e926.net/">e926.net</a> only shows the "clean" images and
 * comics, but it can be difficult to browse.
 * 
 * @author niki
 */
class E621 extends BasicSupport {
	@Override
	protected boolean supports(URL url) {
		String host = url.getHost();
		if (host.startsWith("www.")) {
			host = host.substring("www.".length());
		}

		return ("e621.net".equals(host) || "e926.net".equals(host)) && (isPool(url) || isSearchOrSet(url));
	}

	@Override
	protected boolean isHtml() {
		return true;
	}

	@Override
	protected MetaData getMeta() throws IOException {
		MetaData meta = new MetaData();

		meta.setTitle(getTitle());
		meta.setAuthor(getAuthor());
		meta.setDate("");
		meta.setTags(getTags());
		meta.setSource(getType().getSourceName());
		meta.setUrl(getSource().toString());
		meta.setPublisher(getType().getSourceName());
		meta.setUuid(getSource().toString());
		meta.setLuid("");
		meta.setLang("en");
		meta.setSubject("Furry");
		meta.setType(getType().toString());
		meta.setImageDocument(true);
		meta.setCover(getCover());
		meta.setFakeCover(true);

		return meta;
	}

	@Override
	protected String getDesc() throws IOException {
		if (isSearchOrSet(getSource())) {
			StringBuilder builder = new StringBuilder();
			builder.append("A collection of images from ").append(getSource().getHost()).append("\n") //
					.append("\tTime of creation: " + StringUtils.fromTime(new Date().getTime())).append("\n") //
					.append("\tTags: ");//
			for (String tag : getTags()) {
				builder.append("\t\t").append(tag);
			}

			return builder.toString();
		}

		if (isPool(getSource())) {
			Element el = getSourceNode().getElementById("description");
			if (el != null) {
				return el.text();
			}
		}

		return null;
	}

	@Override
	protected List<Entry<String, URL>> getChapters(Progress pg) throws IOException {
		List<Entry<String, URL>> chapters = new LinkedList<Entry<String, URL>>();
		
		if (isPool(getSource())) {
			String baseUrl = "https://e621.net/" + getSource().getPath() + "?page=";
			chapters = getChapters(getSource(), pg, baseUrl, "");
		} else if (isSearchOrSet(getSource())) {
			String baseUrl = "https://e621.net/posts/?page=";
			String search = "&tags=" + getTagsFromUrl(getSource());

			chapters = getChapters(getSource(), pg,
					baseUrl, search);
		}

		// sets and some pools are sorted in reverse order on the website
		if (getSource().getPath().startsWith("/posts")) {
			Collections.reverse(chapters);	
		}
		
		return chapters;
	}

	private List<Entry<String, URL>> getChapters(URL source, Progress pg, String baseUrl, String parameters)
			throws IOException {
		List<Entry<String, URL>> urls = new ArrayList<Entry<String, URL>>();

		if (source.getHost().contains("e926")) {
			baseUrl = baseUrl.replace("e621", "e926");
		}

		for (int i = 1; true; i++) {
			URL url = new URL(baseUrl + i + parameters);
			try {
				InputStream pageI = Instance.getInstance().getCache().open(url, this, false);
				try {
					if (IOUtils.readSmallStream(pageI).contains("Nobody here but us chickens!")) {
						break;
					}
					urls.add(new AbstractMap.SimpleEntry<String, URL>("Page " + Integer.toString(i), url));
				} finally {
					pageI.close();
				}
			} catch (Exception e) {
				break;
			}
		}

		return urls;
	}

	@Override
	protected String getChapterContent(URL chapUrl, int number, Progress pg) throws IOException {
		StringBuilder builder = new StringBuilder();
		Document chapterNode = loadDocument(chapUrl);
		
		Elements articles = chapterNode.getElementsByTag("article");
		
		// sets and some pools are sorted in reverse order on the website
		if (getSource().getPath().startsWith("/posts")) {
			Collections.reverse(articles);	
		}
		
		for (Element el : articles) {
			builder.append("[");
			builder.append(el.attr("data-file-url"));
			builder.append("]<br/>");
		}

		return builder.toString();
	}

	@Override
	protected URL getCanonicalUrl(URL source) {
		// Convert search-pools into proper pools
		if (source.getPath().equals("/posts") && source.getQuery() != null
				&& source.getQuery().startsWith("tags=pool%3A")) {
			String poolNumber = source.getQuery()
					.substring("tags=pool%3A".length());
			try {
				Integer.parseInt(poolNumber);
				String base = source.getProtocol() + "://" + source.getHost();
				if (source.getPort() != -1) {
					base = base + ":" + source.getPort();
				}
				source = new URL(base + "/pools/" + poolNumber);
			} catch (NumberFormatException e) {
				// Not a simple pool, skip
			} catch (MalformedURLException e) {
				// Cannot happen
			}
		}
		
		if (isSetOriginalUrl(source)) {
			try {
				Document doc = DataUtil.load(Instance.getInstance().getCache().open(source, this, false), "UTF-8", source.toString());
				for (Element shortname : doc.getElementsByClass("set-shortname")) {
					for (Element el : shortname.getElementsByTag("a")) {
						if (!el.attr("href").isEmpty())
							return new URL(el.absUrl("href"));
					}
				}
			} catch (IOException e) {
				Instance.getInstance().getTraceHandler().error(e);
			}
		}

		if (isPool(source)) {
			try {
				return new URL(source.toString().replace("/pool/show/", "/pools/"));
			} catch (MalformedURLException e) {
			}
		}

		return super.getCanonicalUrl(source);
	}

	// returns "xxx+ddd+ggg" if "tags=xxx+ddd+ggg" was present in the query
	private String getTagsFromUrl(URL url) {
		String tags = url == null ? "" : url.getQuery();
		int pos = tags.indexOf("tags=");

		if (pos >= 0) {
			tags = tags.substring(pos).substring("tags=".length());
		} else {
			return "";
		}

		pos = tags.indexOf('&');
		if (pos > 0) {
			tags = tags.substring(0, pos);
		}
		pos = tags.indexOf('/');
		if (pos > 0) {
			tags = tags.substring(0, pos);
		}

		return tags;
	}

	private String getTitle() {
		String title = "";

		Element el = getSourceNode().getElementsByTag("title").first();
		if (el != null) {
			title = el.text().trim();
		}

		for (String s : new String[] { "e621", "-", "e621", "Pool", "-" }) {
			if (title.startsWith(s)) {
				title = title.substring(s.length()).trim();
			}
			if (title.endsWith(s)) {
				title = title.substring(0, title.length() - s.length()).trim();
			}
		}

		if (isSearchOrSet(getSource())) {
			title = title.isEmpty() ? "e621" : "[e621] " + title;
		}
		
		return title;
	}

	private String getAuthor() throws IOException {
		StringBuilder builder = new StringBuilder();

		if (isSearchOrSet(getSource())) {
			for (Element el : getSourceNode().getElementsByClass("search-tag")) {
				if (el.attr("itemprop").equals("author")) {
					if (builder.length() > 0) {
						builder.append(", ");
					}
					builder.append(el.text().trim());
				}
			}
		}

		if (isPool(getSource())) {
			String desc = getDesc();
			String descL = desc.toLowerCase();

			if (descL.startsWith("by:") || descL.startsWith("by ")) {
				desc = desc.substring(3).trim();
				desc = desc.split("\n")[0];

				String tab[] = desc.split(" ");
				for (int i = 0; i < Math.min(tab.length, 5); i++) {
					if (tab[i].startsWith("http"))
						break;
					builder.append(" ").append(tab[i]);
				}
			}

			if (builder.length() == 0) {
				try {
					String poolNumber = getSource().getPath()
							.substring("/pools/".length());
					String url = "https://e621.net/posts" + "?tags=pool%3A"
							+ poolNumber;

					Document page1 = DataUtil.load(Instance.getInstance()
							.getCache().open(getSource(), null, false), "UTF-8",
							url);
					for (Element el : page1.getElementsByClass("search-tag")) {
						if (el.attr("itemprop").equals("author")) {
							if (builder.length() > 0) {
								builder.append(", ");
							}
							builder.append(el.text().trim());
						}
					}
				} catch (Exception e) {
				}
			}
		}

		return builder.toString();
	}

	// no tags for pools
	private List<String> getTags() {
		List<String> tags = new ArrayList<String>();
		if (isSearchOrSet(getSource())) {
			String str = getTagsFromUrl(getSource());
			for (String tag : str.split("\\+")) {
				try {
					tags.add(URLDecoder.decode(tag.trim(), "UTF-8").trim());
				} catch (UnsupportedEncodingException e) {
				}
			}
		}

		return tags;
	}

	private Image getCover() throws IOException {
		Image image = null;
		List<Entry<String, URL>> chapters = getChapters(null);
		if (!chapters.isEmpty()) {
			URL chap1Url = chapters.get(0).getValue();
			String imgsChap1 = getChapterContent(chap1Url, 1, null);
			if (!imgsChap1.isEmpty()) {
				imgsChap1 = imgsChap1.split("]")[0].substring(1).trim();
				image = bsImages.getImage(this, new URL(imgsChap1));
			}
		}

		return image;
	}

	// note: will be removed at getCanonicalUrl()
	private boolean isSetOriginalUrl(URL originalUrl) {
		return originalUrl.getPath().startsWith("/post_sets/");
	}

	private boolean isPool(URL url) {
		return url.getPath().startsWith("/pools/") || url.getPath().startsWith("/pool/show/");
	}

	// set will be renamed into search by canonical url
	private boolean isSearchOrSet(URL url) {
		return
		// search:
		(url.getPath().equals("/posts") && url.getQuery().contains("tags="))
				// or set:
				|| isSetOriginalUrl(url);
	}
}
