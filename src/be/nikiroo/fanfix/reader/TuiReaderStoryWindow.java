package be.nikiroo.fanfix.reader;

import jexer.TApplication;
import jexer.TText;
import jexer.TWindow;
import jexer.event.TResizeEvent;
import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;

public class TuiReaderStoryWindow extends TWindow {
	private MetaData meta;
	private Story story;
	private TText textField;

	public TuiReaderStoryWindow(TApplication app, MetaData meta) {
		super(app, desc(meta), 0, 0, 60, 18, CENTERED | RESIZABLE);
		this.meta = meta;

		// /TODO: status bar with info, buttons to change chapter << < Chapter 0
		// : xxx.. > >> (max size for name = getWith() - X)

		// TODO: show all meta info before

		Chapter resume = getStory().getMeta().getResume();
		StringBuilder text = new StringBuilder();
		if (resume != null) {
			// TODO: why does \n not work but \n\n do? bug in jexer?
			text.append("Resume:\n\n  "); // -> to status bar
			for (Paragraph para : resume) {
				text.append(para.getContent()).append("\n\n  ");
			}
		}
		textField = addText(text.toString(), 0, 0, getWidth(), getHeight());
	}

	@Override
	public void onResize(TResizeEvent resize) {
		super.onResize(resize);

		// Resize the text field
		textField.setWidth(resize.getWidth());
		textField.setHeight(resize.getHeight());
		textField.reflow();
	}

	private Story getStory() {
		if (story == null) {
			// TODO: progress bar
			story = Instance.getLibrary().getStory(meta.getLuid(), null);
		}
		return story;
	}

	private static String desc(MetaData meta) {
		return String.format("%s: %s", meta.getLuid(), meta.getTitle());
	}
}
