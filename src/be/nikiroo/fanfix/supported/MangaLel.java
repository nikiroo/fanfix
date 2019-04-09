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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;

class MangaLel extends BasicSupport {
	@Override
	protected boolean isHtml() {
		return true;
	}

	@Override
	protected MetaData getMeta() throws IOException {
		MetaData meta = new MetaData();

		String[] authorDateTag = getAuthorDateTag();

		meta.setTitle(getTitle());
		meta.setAuthor(authorDateTag[0]);
		meta.setDate(authorDateTag[1]);
		meta.setTags(explode(authorDateTag[2]));
		meta.setSource(getType().getSourceName());
		meta.setUrl(getSource().toString());
		meta.setPublisher(getType().getSourceName());
		meta.setUuid(getSource().toString());
		meta.setLuid("");
		meta.setLang("fr");
		meta.setSubject("manga");
		meta.setType(getType().toString());
		meta.setImageDocument(true);
		meta.setCover(getCover());

		return meta;
	}

	private String getTitle() {
		Element doc = getSourceNode();
		Element h2 = doc.getElementsByClass("widget-title").first();
		if (h2 != null) {
			return StringUtils.unhtml(h2.text()).trim();
		}

		return null;
	}

	// 0 = author
	// 1 = date
	// 2 = tags
	private String[] getAuthorDateTag() {
		String[] tab = new String[3];

		Element doc = getSourceNode();
		Element tabEls = doc.getElementsByClass("dl-horizontal").first();
		int prevOk = 0;
		for (Element tabEl : tabEls.children()) {
			String txt = tabEl.text().trim();
			if (prevOk > 0) {
				if (tab[prevOk - 1] == null) {
					tab[prevOk - 1] = "";
				} else {
					tab[prevOk - 1] += ", ";
				}

				tab[prevOk - 1] += txt;
				prevOk = 0;
			} else {
				if (txt.equals("Auteur(s)") || txt.equals("Artist(s)")) {
					prevOk = 1;
				} else if (txt.equals("Date de sortie")) {
					prevOk = 2;
				} else if (txt.equals("Type") || txt.equals("Catégories")) {
					prevOk = 3;
				} else {
					prevOk = 0;
				}
			}
		}

		for (int i = 0; i < 3; i++) {
			String list = "";
			for (String item : explode(tab[i])) {
				if (!list.isEmpty()) {
					list = list + ", ";
				}
				list += item;
			}
			tab[i] = list;
		}

		return tab;
	}

	@Override
	protected String getDesc() {
		String desc = null;

		Element doc = getSourceNode();
		Element title = doc.getElementsByClass("well").first();
		if (title != null) {
			desc = StringUtils.unhtml(title.text()).trim();
			if (desc.startsWith("Résumé")) {
				desc = desc.substring("Résumé".length()).trim();
			}
		}

		return desc;
	}

	private Image getCover() {
		Element doc = getSourceNode();
		Element cover = doc.getElementsByClass("img-responsive").first();

		if (cover != null) {
			String coverUrl = cover.absUrl("src");

			InputStream coverIn;
			try {
				coverIn = Instance.getCache().open(new URL(coverUrl), this,
						true);
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

		int i = 0;
		Element doc = getSourceNode();
		Elements chapEls = doc.getElementsByClass("chapters").first()
				.getElementsByTag("li");
		for (Element chapEl : chapEls) {
			Element titleEl = chapEl.getElementsByTag("h5").first();
			String title = StringUtils.unhtml(titleEl.text()).trim();

			// because Atril does not support strange file names
			title = Integer.toString(chapEls.size() - i);

			Element linkEl = chapEl.getElementsByTag("h5").first()
					.getElementsByTag("a").first();
			String link = linkEl.absUrl("href");

			try {
				urls.add(new AbstractMap.SimpleEntry<String, URL>(title,
						new URL(link)));
			} catch (MalformedURLException e) {
				Instance.getTraceHandler().error(e);
			}

			i++;
		}

		Collections.reverse(urls);
		return urls;
	}

	@Override
	protected String getChapterContent(URL chapUrl, int number, Progress pg)
			throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		StringBuilder builder = new StringBuilder();

		InputStream in = Instance.getCache().open(chapUrl, this, false);
		try {
			Element pageDoc = DataUtil.load(in, "UTF-8", chapUrl.toString());
			Elements linkEls = pageDoc.getElementsByClass("img-responsive");
			for (Element linkEl : linkEls) {
				if (linkEl.hasAttr("data-src")) {
					builder.append("[");
					builder.append(linkEl.absUrl("data-src").trim());
					builder.append("]<br/>");
				}
			}

		} finally {
			in.close();
		}

		return builder.toString();
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
		return "manga-lel.com".equals(url.getHost())
				|| "www.manga-lel.com".equals(url.getHost());
	}
}
