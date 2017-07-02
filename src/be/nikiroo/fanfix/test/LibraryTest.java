package be.nikiroo.fanfix.test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import be.nikiroo.fanfix.BasicLibrary;
import be.nikiroo.fanfix.LocalLibrary;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.test.TestCase;
import be.nikiroo.utils.test.TestLauncher;

public class LibraryTest extends TestLauncher {
	private BasicLibrary lib;
	private File tmp;

	public LibraryTest(String[] args) {
		super("Library", args);

		final String luid1 = "001"; // A
		final String luid2 = "002"; // B
		final String luid3 = "003"; // B then A, then B
		final String source1 = "Source A";
		final String source2 = "Source B";
		final String author1 = "Unknown author";
		final String author2 = "Another other otter author";

		addSeries(new TestLauncher("Local", args) {
			{
				addTest(new TestCase("getList") {
					@Override
					public void test() throws Exception {
						List<MetaData> metas = lib.getList();
						assertEquals(0, metas.size());
					}
				});

				addTest(new TestCase("save") {
					@Override
					public void test() throws Exception {
						lib.save(story(luid1, "My story 1", source1, author1),
								luid1, null);

						List<MetaData> metas = lib.getList();
						assertEquals(1, metas.size());
					}
				});

				addTest(new TestCase("save more") {
					@Override
					public void test() throws Exception {
						List<MetaData> metas = null;

						lib.save(story(luid2, "My story 2", source2, author1),
								luid2, null);

						metas = lib.getList();
						assertEquals(2, metas.size());

						lib.save(story(luid3, "My story 3", source2, author1),
								luid3, null);

						metas = lib.getList();
						assertEquals(3, metas.size());
					}
				});

				addTest(new TestCase("save override luid (change author)") {
					@Override
					public void test() throws Exception {
						// same luid as a previous one
						lib.save(
								story(luid3, "My story 3 [edited]", source2,
										author2), luid3, null);

						List<MetaData> metas = lib.getList();
						assertEquals(3, metas.size());
					}
				});

				addTest(new TestCase("getList with results") {
					@Override
					public void test() throws Exception {
						List<MetaData> metas = lib.getList();
						assertEquals(3, metas.size());
					}
				});

				addTest(new TestCase("getList by source") {
					@Override
					public void test() throws Exception {
						List<MetaData> metas = null;

						metas = lib.getListBySource(source1);
						assertEquals(1, metas.size());

						metas = lib.getListBySource(source2);
						assertEquals(2, metas.size());

						metas = lib.getListBySource(null);
						assertEquals(3, metas.size());
					}
				});

				addTest(new TestCase("getList by author") {
					@Override
					public void test() throws Exception {
						List<MetaData> metas = null;

						metas = lib.getListByAuthor(author1);
						assertEquals(2, metas.size());

						metas = lib.getListByAuthor(author2);
						assertEquals(1, metas.size());

						metas = lib.getListByAuthor(null);
						assertEquals(3, metas.size());
					}
				});

				addTest(new TestCase("changeType") {
					@Override
					public void test() throws Exception {
						List<MetaData> metas = null;

						lib.changeSource(luid3, source1, null);

						metas = lib.getListBySource(source1);
						assertEquals(2, metas.size());

						metas = lib.getListBySource(source2);
						assertEquals(1, metas.size());

						metas = lib.getListBySource(null);
						assertEquals(3, metas.size());
					}
				});

				addTest(new TestCase("save override luid (change source)") {
					@Override
					public void test() throws Exception {
						List<MetaData> metas = null;

						// same luid as a previous one
						lib.save(story(luid3, "My story 3", source2, author2),
								luid3, null);

						metas = lib.getListBySource(source1);
						assertEquals(1, metas.size());

						metas = lib.getListBySource(source2);
						assertEquals(2, metas.size());

						metas = lib.getListBySource(null);
						assertEquals(3, metas.size());
					}
				});
			}
		});
	}

	@Override
	protected void start() throws Exception {
		tmp = File.createTempFile(".test-fanfix", ".library");
		tmp.delete();
		tmp.mkdir();

		lib = new LocalLibrary(tmp, OutputType.INFO_TEXT, OutputType.CBZ);
	}

	@Override
	protected void stop() throws Exception {
		IOUtils.deltree(tmp);
	}

	private Story story(String luid, String title, String source, String author) {
		Story story = new Story();

		MetaData meta = new MetaData();
		meta.setLuid(luid);
		meta.setTitle(title);
		meta.setSource(source);
		meta.setAuthor(author);
		story.setMeta(meta);

		Chapter resume = chapter(0, "Resume");
		meta.setResume(resume);

		List<Chapter> chapters = new ArrayList<Chapter>();
		chapters.add(chapter(1, "Chap 1"));
		chapters.add(chapter(2, "Chap 2"));
		story.setChapters(chapters);

		long words = 0;
		for (Chapter chap : story.getChapters()) {
			words += chap.getWords();
		}
		meta.setWords(words);

		return story;
	}

	private Chapter chapter(int number, String name) {
		Chapter chapter = new Chapter(number, name);

		List<Paragraph> paragraphs = new ArrayList<Paragraph>();
		paragraphs.add(new Paragraph(Paragraph.ParagraphType.NORMAL,
				"some words in this paragraph please thank you", 8));

		chapter.setParagraphs(paragraphs);

		long words = 0;
		for (Paragraph para : chapter.getParagraphs()) {
			words += para.getWords();
		}
		chapter.setWords(words);

		return chapter;
	}
}
