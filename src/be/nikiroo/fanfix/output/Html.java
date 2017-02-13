package be.nikiroo.fanfix.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.StringUtils;

class Html extends BasicOutput {
	protected FileWriter writer;
	private boolean inDialogue = false;
	private boolean inNormal = false;

	@Override
	public File process(Story story, File targetDir, String targetName)
			throws IOException {
		File target = new File(targetDir, targetName);
		target.mkdir();

		targetName = new File(targetName, "index").getPath();

		String targetNameOrig = targetName;
		targetName += getDefaultExtension();

		target = new File(targetDir, targetName);

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
		return ".html";
	}

	@Override
	protected void writeStoryHeader(Story story) throws IOException {
		String title = "";
		if (story.getMeta() != null) {
			title = story.getMeta().getTitle();
		}

		writer.write("<!DOCTYPE html>");
		writer.write("\n<html>");
		writer.write("\n<head>");
		writer.write("\n	<meta http-equiv='content-type' content='text/html; charset=utf-8'>");
		writer.write("\n	<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
		writer.write("\n	<title>" + StringUtils.xmlEscape(title) + "</title>");
		writer.write("\n</head>");
		writer.write("\n<body>\n");

		writer.write("<h1>" + StringUtils.xmlEscape(title) + "</h1>\n\n");
	}

	@Override
	protected void writeStoryFooter(Story story) throws IOException {
		writer.write("</body>\n");
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

		writer.write("<h1>" + StringUtils.xmlEscape(txt) + "</h1>\n\n");

		inDialogue = false;
		inNormal = false;
	}

	@Override
	protected void writeParagraphHeader(Paragraph para) throws IOException {
		if (para.getType() == ParagraphType.QUOTE && !inDialogue) {
			writer.write("		<div class='dialogues'>\n");
			inDialogue = true;
		} else if (para.getType() != ParagraphType.QUOTE && inDialogue) {
			writer.write("		</div>\n");
			inDialogue = false;
		}

		if (para.getType() == ParagraphType.NORMAL && !inNormal) {
			writer.write("		<div class='normals'>\n");
			inNormal = true;
		} else if (para.getType() != ParagraphType.NORMAL && inNormal) {
			writer.write("		</div>\n");
			inNormal = false;
		}

		switch (para.getType()) {
		case BLANK:
			writer.write("		<div class='blank'></div>");
			break;
		case BREAK:
			writer.write("		<hr/>");
			break;
		case NORMAL:
			writer.write("		<span class='normal'>");
			break;
		case QUOTE:
			writer.write("			<div class='dialogue'>&mdash; ");
			break;
		case IMAGE:
			// TODO
			writer.write("<a href='"
					+ StringUtils.xmlEscapeQuote(para.getContent()) + "'>"
					+ StringUtils.xmlEscape(para.getContent()) + "</a>");
			break;
		}
	}

	@Override
	protected void writeParagraphFooter(Paragraph para) throws IOException {
		switch (para.getType()) {
		case NORMAL:
			writer.write("</span>\n");
			break;
		case QUOTE:
			writer.write("</div>\n");
			break;
		default:
			writer.write("\n");
			break;
		}
	}

	@Override
	protected void writeTextLine(ParagraphType type, String line)
			throws IOException {
		switch (type) {
		case QUOTE:
		case NORMAL:
			writer.write(decorateText(StringUtils.xmlEscape(line)));
			break;
		default:
			break;
		}
	}

	@Override
	protected String enbold(String word) {
		return "<strong>" + word + "</strong>";
	}

	@Override
	protected String italize(String word) {
		return "<emph>" + word + "</emph>";
	}
}
