package be.nikiroo.fanfix.reader;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.Library;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.BasicSupport.SupportType;

/**
 * Command line {@link Story} reader.
 * <p>
 * Will output stories to the console.
 * 
 * @author niki
 */
public class CliReader {
	private Story story;

	/**
	 * Create a new {@link CliReader} for a {@link Story} in the {@link Library}
	 * .
	 * 
	 * @param luid
	 *            the {@link Story} ID
	 * @throws IOException
	 *             in case of I/O error
	 */
	public CliReader(String luid) throws IOException {
		story = Instance.getLibrary().getStory(luid);
		if (story == null) {
			throw new IOException("Cannot retrieve story from library: " + luid);
		}
	}

	/**
	 * Create a new {@link CliReader} for an external {@link Story}.
	 * 
	 * @param source
	 *            the {@link Story} {@link URL}
	 * @throws IOException
	 *             in case of I/O error
	 */
	public CliReader(URL source) throws IOException {
		BasicSupport support = BasicSupport.getSupport(source);
		if (support == null) {
			throw new IOException("URL not supported: " + source.toString());
		}

		story = support.process(source);
		if (story == null) {
			throw new IOException(
					"Cannot retrieve story from external source: "
							+ source.toString());

		}
	}

	/**
	 * Read the information about the {@link Story}.
	 */
	public void read() {
		String title = "";
		String author = "";

		MetaData meta = story.getMeta();
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

		for (Chapter chap : story) {
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
		if (chapter > story.getChapters().size()) {
			System.err.println("Chapter " + chapter + ": no such chapter");
		} else {
			Chapter chap = story.getChapters().get(chapter - 1);
			System.out.println("Chapter " + chap.getNumber() + ": "
					+ chap.getName());

			for (Paragraph para : chap) {
				System.out.println(para.getContent());
				System.out.println("");
			}
		}
	}

	/**
	 * List all the stories available in the {@link Library} by
	 * {@link OutputType} (or all of them if the given type is NULL)
	 * 
	 * @param type
	 *            the {@link OutputType} or NULL for all stories
	 */
	public static void list(SupportType type) {
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
