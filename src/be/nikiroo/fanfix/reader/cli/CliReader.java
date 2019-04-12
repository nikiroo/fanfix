package be.nikiroo.fanfix.reader.cli;

import java.io.IOException;
import java.util.ArrayList;
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
	public void search(SupportType searchOn, String keywords, int page, int item)
			throws IOException {

	}

	@Override
	public void searchTag(SupportType searchOn, int page, int item,
			String... tags) throws IOException {
		BasicSearchable search = BasicSearchable.getSearchable(searchOn);
		List<SearchableTag> stags = search.getTags();

		SearchableTag stag = null;
		for (String tag : tags) {
			stag = null;
			for (int i = 0; i < stags.size(); i++) {
				if (stags.get(i).getName().equalsIgnoreCase(tag)) {
					stag = stags.get(i);
					break;
				}
			}

			if (stag != null) {
				search.fillTag(stag);
				stags = stag.getChildren();
			} else {
				stags = new ArrayList<SearchableTag>();
				break;
			}
		}

		if (stag != null) {
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
							System.out.println(page + "/" + item + ": "
									+ meta.getTitle());
							System.out.println();
							System.out.println(meta.getUrl());
							System.out.println();
							System.out.println("Tags: " + meta.getTags());
							System.out.println();
							for (Paragraph para : meta.getResume()) {
								System.out.println(para.getContent());
								System.out.println("");
							}
						} else {
							SearchableTag subtag = subtags.get(item - 1);

							String sp = "";
							if (subtag.getParent() != null) {
								List<String> parents = new ArrayList<String>();
								for (SearchableTag parent = subtag.getParent(); parent != null; parent = parent
										.getParent()) {
									parents.add(parent.getName());
								}
								for (String parent : parents) {
									if (!sp.isEmpty()) {
										sp += " / ";
									}
									sp += parent;
								}
							}

							// TODO: i18n
							if (sp.isEmpty()) {
								System.out.println(String.format(
										"%d/%d: %s, %d %s", page, item,
										subtag.getName(), subtag.getCount(),
										"stories"));
							} else {
								System.out.println(String.format(
										"%d/%d: %s (%s), %d %s", page, item,
										subtag.getName(), sp,
										subtag.getCount(), "stories"));
							}
						}
					} else {
						System.out.println("Invalid item: only " + count
								+ " items found");
					}
				} else {
					if (metas != null) {
						int i = 0;
						for (MetaData meta : metas) {
							System.out
									.println((i + 1) + ": " + meta.getTitle());
							i++;
						}
					} else {
						int i = 1;
						for (SearchableTag subtag : subtags) {
							String total = "";
							if (subtag.getCount() > 0) {
								// TODO: use StringUtils fromNumber
								total = " (" + subtag.getCount() + ")";
							}
							System.out.println(i + ": " + subtag.getName()
									+ total);
							i++;
						}
					}
				}
			}
		} else {
			for (SearchableTag s : stags) {
				System.out.println(s.getName());
			}
		}
	}
}
