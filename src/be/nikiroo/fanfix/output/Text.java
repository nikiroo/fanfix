package be.nikiroo.fanfix.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;

class Text extends BasicOutput {
	protected FileWriter writer;
	protected File targetDir;

	@Override
	public File process(Story story, File targetDir, String targetName)
			throws IOException {
		String targetNameOrig = targetName;
		targetName += getDefaultExtension();

		this.targetDir = targetDir;

		File target = new File(targetDir, targetName);

		writer = new FileWriter(target);
		try {
			super.process(story, targetDir, targetNameOrig);
		} finally {
			writer.close();
			writer = null;
		}

		return target;
	}

	@Override
	public String getDefaultExtension() {
		return ".txt";
	}

	@Override
	protected void writeStoryHeader(Story story) throws IOException {
		String title = "";
		String author = null;
		String date = null;

		MetaData meta = story.getMeta();
		if (meta != null) {
			title = meta.getTitle() == null ? "" : meta.getTitle();
			author = meta.getAuthor();
			date = meta.getDate();
		}

		writer.write(title);
		writer.write("\n");
		if (author != null && !author.isEmpty()) {
			writer.write(Instance.getTrans().getString(StringId.BY) + " "
					+ author);
		}
		if (date != null && !date.isEmpty()) {
			writer.write(" (");
			writer.write(date);
			writer.write(")");
		}
		writer.write("\n");

		// resume:
		if (meta != null && meta.getResume() != null) {
			writeChapter(meta.getResume());
		}
	}

	@Override
	protected void writeChapterHeader(Chapter chap) throws IOException {
		String txt;
		if (chap.getName() != null && !chap.getName().isEmpty()) {
			txt = Instance.getTrans().getString(StringId.CHAPTER_NAMED,
					chap.getNumber(), chap.getName());
		} else {
			txt = Instance.getTrans().getString(StringId.CHAPTER_UNNAMED,
					chap.getNumber());
		}

		writer.write("\n" + txt + "\n");
		for (int i = 0; i < txt.length(); i++) {
			writer.write("—");
		}
		writer.write("\n\n");
	}

	@Override
	protected void writeParagraphFooter(Paragraph para) throws IOException {
		writer.write("\n");
	}

	@Override
	protected void writeParagraphHeader(Paragraph para) throws IOException {
		if (para.getType() == ParagraphType.IMAGE) {
			File file = new File(targetDir, getCurrentImageBestName(true));
			Instance.getCache().saveAsImage(new URL(para.getContent()), file);
		}
	}

	@Override
	protected void writeTextLine(ParagraphType type, String line)
			throws IOException {
		switch (type) {
		case BLANK:
			break;
		case BREAK:
			writer.write("\n* * *\n");
			break;
		case NORMAL:
		case QUOTE:
			writer.write(line);
			break;
		case IMAGE:
			writer.write("[" + getCurrentImageBestName(true) + "]");
			break;
		}
	}
}
