package be.nikiroo.fanfix.reader.cli;

import java.io.IOException;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.searchable.BasicSearchable;
import be.nikiroo.fanfix.searchable.SearchableTag;
import be.nikiroo.fanfix.supported.SupportType;
import be.nikiroo.utils.StringUtils;

/**
 * Command line {@link Story} reader.
 * <p>
 * Will output stories to the console.
 * 
 * @author niki
 */
class CliReader extends BasicReader {
	@Override
	public void read(boolean sync) throws IOException {
		MetaData meta = getMeta();

		if (meta == null) {
			throw new IOException("No story to read");
		}

		String title = "";
		String author = "";

		if (meta.getTitle() != null) {
			title = meta.getTitle();
		}

		if (meta.getAuthor() != null) {
			author = "©" + meta.getAuthor();
			if (meta.getDate() != null && !meta.getDate().isEmpty()) {
				author = author + " (" + meta.getDate() + ")";
			}
		}

		System.out.println(title);
		System.out.println(author);
		System.out.println("");

		// TODO: progress?
		for (Chapter chap : getStory(null)) {
			if (chap.getName() != null && !chap.getName().isEmpty()) {
				System.out.println(Instance.getTrans().getString(
						StringId.CHAPTER_NAMED, chap.getNumber(),
						chap.getName()));
			} else {
				System.out.println(Instance.getTrans().getString(
						StringId.CHAPTER_UNNAMED, chap.getNumber()));
			}
		}
	}

	public void read(int chapter) throws IOException {
		MetaData meta = getMeta();

		if (meta == null) {
			throw new IOException("No story to read");
		}

		// TODO: progress?
		if (chapter > getStory(null).getChapters().size()) {
			System.err.println("Chapter " + chapter + ": no such chapter");
		} else {
			Chapter chap = getStory(null).getChapters().get(chapter - 1);
			System.out.println("Chapter " + chap.getNumber() + ": "
					+ chap.getName());

			for (Paragraph para : chap) {
				System.out.println(para.getContent());
				System.out.println("");
			}
		}
	}

	@Override
	public void browse(String source) {
		List<MetaData> stories;
		stories = getLibrary().getListBySource(source);

		for (MetaData story : stories) {
			String author = "";
			if (story.getAuthor() != null && !story.getAuthor().isEmpty()) {
				author = " (" + story.getAuthor() + ")";
			}

			System.out.println(story.getLuid() + ": " + story.getTitle()
					+ author);
		}
	}

	@Override
	public void search(boolean sync) throws IOException {
		for (SupportType type : SupportType.values()) {
			if (BasicSearchable.getSearchable(type) != null) {
				System.out.println(type);
			}
		}
	}

	@Override
	public void search(SupportType searchOn, String keywords, int page,
			int item, boolean sync) throws IOException {
		BasicSearchable search = BasicSearchable.getSearchable(searchOn);

		if (page == 0) {
			System.out.println(search.searchPages(keywords));
		} else {
			List<MetaData> metas = search.search(keywords, page);

			if (item == 0) {
				System.out.println("Page " + page + " of stories for: "
						+ keywords);
				displayStories(metas);
			} else {
				// ! 1-based index !
				if (item <= 0 || item > metas.size()) {
					throw new IOException("Index out of bounds: " + item);
				}

				MetaData meta = metas.get(item - 1);
				displayStory(meta);
			}
		}
	}

	@Override
	public void searchTag(SupportType searchOn, int page, int item,
			boolean sync, Integer... tags) throws IOException {

		BasicSearchable search = BasicSearchable.getSearchable(searchOn);
		SearchableTag stag = search.getTag(tags);

		if (stag == null) {
			// TODO i18n
			System.out.println("Known tags: ");
			int i = 1;
			for (SearchableTag s : search.getTags()) {
				System.out.println(String.format("%d: %s", i, s.getName()));
				i++;
			}
		} else {
			if (page <= 0) {
				if (stag.isLeaf()) {
					search.search(stag, 1);
					System.out.println(stag.getPages());
				} else {
					System.out.println(stag.getCount());
				}
			} else {
				List<MetaData> metas = null;
				List<SearchableTag> subtags = null;
				int count;

				if (stag.isLeaf()) {
					metas = search.search(stag, page);
					count = metas.size();
				} else {
					subtags = stag.getChildren();
					count = subtags.size();
				}

				if (item > 0) {
					if (item <= count) {
						if (metas != null) {
							MetaData meta = metas.get(item - 1);
							displayStory(meta);
						} else {
							SearchableTag subtag = subtags.get(item - 1);
							displayTag(subtag);
						}
					} else {
						System.out.println("Invalid item: only " + count
								+ " items found");
					}
				} else {
					if (metas != null) {
						// TODO i18n
						System.out.println(String.format("Content of %s: ",
								stag.getFqName()));
						displayStories(metas);
					} else {
						// TODO i18n
						System.out.println(String.format("Subtags of %s: ",
								stag.getFqName()));
						displayTags(subtags);
					}
				}
			}
		}
	}

	private void displayTag(SearchableTag subtag) {
		// TODO: i18n
		String stories = "stories";
		String num = StringUtils.formatNumber(subtag.getCount());
		System.out.println(String.format("%s (%s), %s %s", subtag.getName(),
				subtag.getFqName(), num, stories));
	}

	private void displayStory(MetaData meta) {
		System.out.println(meta.getTitle());
		System.out.println();
		System.out.println(meta.getUrl());
		System.out.println();
		System.out.println("Tags: " + meta.getTags());
		System.out.println();
		for (Paragraph para : meta.getResume()) {
			System.out.println(para.getContent());
			System.out.println("");
		}
	}

	private void displayTags(List<SearchableTag> subtags) {
		int i = 1;
		for (SearchableTag subtag : subtags) {
			String total = "";
			if (subtag.getCount() > 0) {
				total = StringUtils.formatNumber(subtag.getCount());
			}

			if (total.isEmpty()) {
				System.out
						.println(String.format("%d: %s", i, subtag.getName()));
			} else {
				System.out.println(String.format("%d: %s (%s)", i,
						subtag.getName(), total));
			}

			i++;
		}
	}

	private void displayStories(List<MetaData> metas) {
		int i = 1;
		for (MetaData meta : metas) {
			System.out.println(i + ": " + meta.getTitle());
			i++;
		}
	}
}
