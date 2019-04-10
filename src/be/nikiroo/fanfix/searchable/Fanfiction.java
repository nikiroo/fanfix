package be.nikiroo.fanfix.searchable;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.supported.SupportType;

/**
 * A {@link BasicSearchable} for Fanfiction.NET.
 * 
 * @author niki
 */
class Fanfiction extends BasicSearchable {
	static private String BASE_URL = "http://fanfiction.net/";

	/**
	 * Create a new {@link Fanfiction}.
	 * 
	 * @param type
	 *            {@link SupportType#FANFICTION}
	 */
	public Fanfiction(SupportType type) {
		super(type);
	}

	@Override
	public List<SearchableTag> getTags() throws IOException {
		String storiesName = null;
		String crossoversName = null;
		Map<String, String> stories = new HashMap<String, String>();
		Map<String, String> crossovers = new HashMap<String, String>();

		Document mainPage = load(BASE_URL, true);
		Element menu = mainPage.getElementsByClass("dropdown").first();
		if (menu != null) {
			Element ul = menu.getElementsByClass("dropdown-menu").first();
			if (ul != null) {
				Map<String, String> currentList = null;
				for (Element li : ul.getElementsByTag("li")) {
					if (li.hasClass("disabled")) {
						if (storiesName == null) {
							storiesName = li.text();
							currentList = stories;
						} else {
							crossoversName = li.text();
							currentList = crossovers;
						}
					} else if (currentList != null) {
						Element a = li.getElementsByTag("a").first();
						if (a != null) {
							currentList.put(a.absUrl("href"), a.text());
						}
					}
				}
			}
		}

		List<SearchableTag> tags = new ArrayList<SearchableTag>();

		if (storiesName != null) {
			SearchableTag tag = new SearchableTag(null, storiesName, false);
			for (String id : stories.keySet()) {
				tag.add(new SearchableTag(id, stories.get(id), true, false));
			}
			tags.add(tag);
		}

		if (crossoversName != null) {
			SearchableTag tag = new SearchableTag(null, crossoversName, false);
			for (String id : crossovers.keySet()) {
				tag.add(new SearchableTag(id, crossovers.get(id), false, false));
			}
			tags.add(tag);
		}

		return tags;
	}

	@Override
	protected void fillTag(SearchableTag tag) throws IOException {
		if (tag.getId() == null || tag.isComplete()) {
			return;
		}

		boolean subtagIsLeaf = !tag.getId().contains("/crossovers/");

		Document doc = load(tag.getId(), false);
		Element list = doc.getElementById("list_output");
		if (list != null) {
			Element table = list.getElementsByTag("table").first();
			if (table != null) {
				for (Element div : table.getElementsByTag("div")) {
					Element a = div.getElementsByTag("a").first();
					Element span = div.getElementsByTag("span").first();

					if (a != null) {
						SearchableTag subtag = new SearchableTag(
								a.absUrl("href"), a.text(), subtagIsLeaf);
						tag.add(subtag);
						if (span != null) {
							String nr = span.text();
							if (nr.startsWith("(")) {
								nr = nr.substring(1);
							}
							if (nr.endsWith(")")) {
								nr = nr.substring(0, nr.length() - 1);
							}
							nr = nr.trim();

							long count = 0;
							try {
								if (nr.toLowerCase().endsWith("m")) {
									count = Long.parseLong(nr.substring(0,
											nr.length() - 1).trim());
									count *= 1000000;
								} else if (nr.toLowerCase().endsWith("k")) {
									count = Long.parseLong(nr.substring(0,
											nr.length() - 1).trim());
									count *= 1000;
								} else {
									count = Long.parseLong(nr);
								}
							} catch (NumberFormatException pe) {
							}

							subtag.setCount(count);
						}
					}
				}
			}
		}

		tag.setComplete(true);
	}

	@Override
	public List<MetaData> search(String search) throws IOException {
		// TODO /search/?reader=1&type=story&keywords=blablablab
		return null;
	}

	@Override
	public List<MetaData> search(SearchableTag tag) throws IOException {
		List<MetaData> metas = new ArrayList<MetaData>();

		if (tag.getId() != null) {
			Document doc = load(tag.getId(), false);

			Element center = doc.getElementsByTag("center").first();
			if (center != null) {
				int pages = -1;
				for (Element a : center.getElementsByTag("a")) {
					if (a.absUrl("href").contains("&p=")) {
						int thisLinkPages = -1;
						try {
							String[] tab = a.absUrl("href").split("=");
							tab = tab[tab.length - 1].split("&");
							thisLinkPages = Integer
									.parseInt(tab[tab.length - 1]);
						} catch (Exception e) {
						}

						pages = Math.max(pages, thisLinkPages);
					}
				}

				tag.setPages(pages);
			}

			for (Element story : doc.getElementsByClass("z-list")) {
				String title = "";
				String url = "";
				String coverUrl = "";

				Element stitle = story.getElementsByClass("stitle").first();
				if (stitle != null) {
					title = stitle.text();
					url = stitle.absUrl("href");
					Element cover = stitle.getElementsByTag("img").first();
					if (cover != null) {
						// note: see data-original if needed?
						coverUrl = cover.absUrl("src");
					}
				}

				String author = "";

				Elements as = story.getElementsByTag("a");
				if (as.size() > 1) {
					author = as.get(1).text();
				}

				String resume = "";
				String tags = "";

				Elements divs = story.getElementsByTag("div");
				if (divs.size() > 1 && divs.get(1).childNodeSize() > 0) {
					resume = divs.get(1).text();
					if (divs.size() > 2) {
						tags = divs.get(2).text();
						resume = resume.substring(0,
								resume.length() - tags.length()).trim();
					}
				}

				MetaData meta = new MetaData();
				meta.setAuthor(author);
				// meta.setCover(cover); //TODO ?
				meta.setImageDocument(false);
				meta.setResume(getSupport().makeChapter(new URL(tag.getId()),
						0, Instance.getTrans().getString(StringId.DESCRIPTION),
						resume));
				meta.setSource(getType().getSourceName());
				// TODO: remove tags to interpret them instead (lang, words..)
				meta.setTags(Arrays.asList(tags.split(" *- *")));
				meta.setTitle(title);
				meta.setUrl(url);

				metas.add(meta);
			}
		}

		return metas;
	}

	public static void main(String[] args) throws IOException {
		Fanfiction f = new Fanfiction(SupportType.FANFICTION);

		SearchableTag cartoons = f.getTags().get(0).getChildren().get(2);
		f.fillTag(cartoons);
		SearchableTag mlp = cartoons.getChildren().get(2);
		System.out.println(mlp);

		SearchableTag ccartoons = f.getTags().get(1).getChildren().get(0);
		f.fillTag(ccartoons);
		SearchableTag cmlp = ccartoons.getChildren().get(0);
		System.out.println(cmlp);

		f.fillTag(cmlp);
		System.out.println(cmlp);

		List<MetaData> metas = f.search(mlp);
		System.out.println(mlp.getPages());
		//System.out.println(metas);
	}
}
