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

/**
 * Command line {@link Story} reader.
 * <p>
 * Will output stories to the console.
 * 
 * @author niki
 */
class CliReader extends BasicReader {
	@Override
	public void read() throws IOException {
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
}
