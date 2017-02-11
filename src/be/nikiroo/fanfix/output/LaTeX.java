package be.nikiroo.fanfix.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;

class LaTeX extends BasicOutput {
	protected FileWriter writer;
	private boolean lastWasQuote = false;

	// quote chars
	private char openQuote = Instance.getTrans().getChar(
			StringId.OPEN_SINGLE_QUOTE);
	private char closeQuote = Instance.getTrans().getChar(
			StringId.CLOSE_SINGLE_QUOTE);
	private char openDoubleQuote = Instance.getTrans().getChar(
			StringId.OPEN_DOUBLE_QUOTE);
	private char closeDoubleQuote = Instance.getTrans().getChar(
			StringId.CLOSE_DOUBLE_QUOTE);

	@Override
	public File process(Story story, File targetDir, String targetName)
			throws IOException {
		String targetNameOrig = targetName;
		targetName += getDefaultExtension();

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
	protected String getDefaultExtension() {
		return ".tex";
	}

	@Override
	protected void writeStoryHeader(Story story) throws IOException {
		String date = "";
		String author = "";
		String title = "\\title{}";
		String lang = "";
		if (story.getMeta() != null) {
			MetaData meta = story.getMeta();
			title = "\\title{" + latexEncode(meta.getTitle()) + "}";
			date = "\\date{" + latexEncode(meta.getDate()) + "}";
			author = "\\author{" + latexEncode(meta.getAuthor()) + "}";
			lang = meta.getLang().toLowerCase();
			if (lang != null && !lang.isEmpty()) {
				lang = Instance.getConfig().getStringX(Config.LATEX_LANG, lang);
				if (lang == null) {
					System.err.println(Instance.getTrans().getString(
							StringId.LATEX_LANG_UNKNOWN, lang));
				}
			}
		}

		writer.append("%\n");
		writer.append("% This LaTeX document was auto-generated by Fanfic Reader, created by Niki.\n");
		writer.append("%\n\n");
		writer.append("\\documentclass[a4paper]{book}\n");
		if (lang != null && !lang.isEmpty()) {
			writer.append("\\usepackage[" + lang + "]{babel}\n");
		}
		writer.append("\\usepackage[utf8]{inputenc}\n");
		writer.append("\\usepackage[T1]{fontenc}\n");
		writer.append("\\usepackage{lmodern}\n");
		writer.append("\\newcommand{\\br}{\\vspace{10 mm}}\n");
		writer.append("\\newcommand{\\say}{--- \\noindent\\emph}\n");
		writer.append("\\hyphenpenalty=1000\n");
		writer.append("\\tolerance=5000\n");
		writer.append("\\begin{document}\n");
		if (story.getMeta() != null && story.getMeta().getDate() != null)
			writer.append(date + "\n");
		writer.append(title + "\n");
		writer.append(author + "\n");
		writer.append("\\maketitle\n");
		writer.append("\n");

		// TODO: cover
	}

	@Override
	protected void writeStoryFooter(Story story) throws IOException {
		writer.append("\\end{document}\n");
	}

	@Override
	protected void writeChapterHeader(Chapter chap) throws IOException {
		writer.append("\n\n\\chapter{" + latexEncode(chap.getName()) + "}"
				+ "\n");
	}

	@Override
	protected void writeChapterFooter(Chapter chap) throws IOException {
		writer.write("\n");
	}

	@Override
	protected String enbold(String word) {
		return "\\textsc{" + word + "}";
	}

	@Override
	protected String italize(String word) {
		return "\\emph{" + word + "}";
	}

	@Override
	protected void writeTextLine(ParagraphType type, String line)
			throws IOException {

		line = decorateText(latexEncode(line));

		switch (type) {
		case BLANK:
			writer.write("\n");
			lastWasQuote = false;
			break;
		case BREAK:
			writer.write("\n\\br");
			writer.write("\n");
			lastWasQuote = false;
			break;
		case NORMAL:
			writer.write(line);
			writer.write("\n");
			lastWasQuote = false;
			break;
		case QUOTE:
			writer.write("\n\\say{" + line + "}\n");
			if (lastWasQuote) {
				writer.write("\n\\noindent{}");
			}
			lastWasQuote = true;
			break;
		case IMAGE:
			// TODO
			break;
		}
	}

	private String latexEncode(String input) {
		StringBuilder builder = new StringBuilder();
		for (char car : input.toCharArray()) {
			// TODO: check restricted chars?
			if (car == '^' || car == '$' || car == '\\' || car == '#'
					|| car == '%') {
				builder.append('\\');
				builder.append(car);
			} else if (car == openQuote) {
				builder.append('`');
			} else if (car == closeQuote) {
				builder.append('\'');
			} else if (car == openDoubleQuote) {
				builder.append("``");
			} else if (car == closeDoubleQuote) {
				builder.append("''");
			} else {
				builder.append(car);
			}
		}

		return builder.toString();
	}
}
