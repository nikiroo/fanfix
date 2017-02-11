package be.nikiroo.fanfix.reader.cli;

import java.io.IOException;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.reader.FanfixReader;
import be.nikiroo.fanfix.supported.BasicSupport.SupportType;

/**
 * Command line {@link Story} reader.
 * <p>
 * Will output stories to the console.
 * 
 * @author niki
 */
public class CliReader extends FanfixReader {
	/**
	 * Start the {@link Story} Reading.
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not
	 *             previously set
	 */
	public void read() throws IOException {
		if (getStory() == null) {
			throw new IOException("No story to read");
		}

		String title = "";
		String author = "";

		MetaData meta = getStory().getMeta();
		if (meta != null) {
			if (meta.getTitle() != null) {
				title = meta.getTitle();
			}

			if (meta.getAuthor() != null) {
				author = "©" + meta.getAuthor();
				if (meta.getDate() != null && !meta.getDate().isEmpty()) {
					author = author + " (" + meta.getDate() + ")";
				}
			}
		}

		System.out.println(title);
		System.out.println(author);
		System.out.println("");

		for (Chapter chap : getStory()) {
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

	/**
	 * Read the selected chapter (starting at 1).
	 * 
	 * @param chapter
	 *            the chapter
	 */
	public void read(int chapter) {
		if (chapter > getStory().getChapters().size()) {
			System.err.println("Chapter " + chapter + ": no such chapter");
		} else {
			Chapter chap = getStory().getChapters().get(chapter - 1);
			System.out.println("Chapter " + chap.getNumber() + ": "
					+ chap.getName());

			for (Paragraph para : chap) {
				System.out.println(para.getContent());
				System.out.println("");
			}
		}
	}

	@Override
	public void start(SupportType type) {
		List<MetaData> stories;
		stories = Instance.getLibrary().getList(type);

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
