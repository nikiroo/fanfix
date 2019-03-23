package be.nikiroo.fanfix.reader.ui;

import java.io.IOException;
import java.util.Arrays;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput;

/**
 * This class can export a chapter into HTML3 code ready for Java Swing support.
 * 
 * @author niki
 */
public class GuiReaderTextViewerOutput {
	private StringBuilder builder;
	private BasicOutput output;
	private Story fakeStory;

	/**
	 * Create a new {@link GuiReaderTextViewerOutput} that will convert a
	 * {@link Chapter} into HTML3 suited for Java Swing.
	 */
	public GuiReaderTextViewerOutput() {
		builder = new StringBuilder();
		fakeStory = new Story();

		output = new BasicOutput() {
			private boolean paraInQuote;

			@Override
			protected void writeChapterHeader(Chapter chap) throws IOException {
				builder.append("<HTML>");

				builder.append("<H1>");
				builder.append("Chapter ");
				builder.append(chap.getNumber());
				builder.append(": ");
				builder.append(chap.getName());
				builder.append("</H1>");

				builder.append("<JUSTIFY>");
				for (Paragraph para : chap) {
					writeParagraph(para);
				}
			}

			@Override
			protected void writeChapterFooter(Chapter chap) throws IOException {
				if (paraInQuote) {
					builder.append("</DIV>");
				}
				paraInQuote = false;

				builder.append("</JUSTIFY>");
				builder.append("</HTML>");
			}

			@Override
			protected void writeParagraph(Paragraph para) throws IOException {
				switch (para.getType()) {
				case NORMAL:
					builder.append(decorateText(para.getContent()));
					builder.append("<BR>");
					break;
				case BLANK:
					builder.append("<BR>");
					break;
				case BREAK:
					builder.append("<BR>* * *<BR><BR>");
					break;
				case QUOTE:
					if (!paraInQuote) {
						builder.append("<DIV>");
					} else {
						builder.append("</DIV>");
					}
					paraInQuote = !paraInQuote;

					builder.append("<DIV>");
					builder.append("&ndash;&nbsp;&nbsp;");
					builder.append(decorateText(para.getContent()));
					builder.append("</DIV>");

					break;
				case IMAGE:
				}
			}

			@Override
			protected String enbold(String word) {
				return "<B COLOR='BLUE'>" + word + "</B>";
			}

			@Override
			protected String italize(String word) {
				return "<I COLOR='GRAY'>" + word + "</I>";
			}
		};
	}

	/**
	 * Convert the chapter into HTML3 code.
	 * 
	 * @param chap
	 *            the {@link Chapter} to convert.
	 * 
	 * @return HTML3 code tested with Java Swing
	 */
	public String convert(Chapter chap) {
		builder.setLength(0);
		try {
			fakeStory.setChapters(Arrays.asList(chap));
			output.process(fakeStory, null, null);
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
		return builder.toString();
	}
}
