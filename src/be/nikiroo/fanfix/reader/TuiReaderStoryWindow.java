package be.nikiroo.fanfix.reader;

import jexer.*;
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
		// TODO: text.append("Resume:\n\n  "); -> to status bar
		
		// -2 because 0-based, 2 for borders, -1 to hide the HScroll
		textField = addText("", 0, 0, getWidth() - 2, getHeight() - 2);
		
		Chapter resume = getStory().getMeta().getResume();
		if (resume != null) {
			for (Paragraph para : resume) {
				// TODO: This is not efficient, should be changed
				for (String line : para.getContent().split("\n")) {
					textField.addLine(line);
				}
			}
		}
		
		statusBar = newStatusBar(desc(meta));
		statusBar.addShortcutKeypress(TKeypress.kbF10, TCommand.cmExit, "Exit");
		
		// -3 because 0-based and 2 for borders
		TButton first = addButton("<<", 0,  getHeight() - 3,
		    new TAction() {
			public void DO() {
			    // TODO
			}
		    }
		);
		addButton("<", 3,  getHeight() - 3, null);
		addButton(">", 5,  getHeight() - 3, null);
		addButton(">>", 7,  getHeight() - 3, null);
		// TODO: pad with space up to end of window
		// TODO: do not show "0/x: " for desc, only for other chapters
		addLabel(String.format(" %d/%d: %s", resume.getNumber(), getStory().getChapters().size(), resume.getName()), 11, getHeight() - 3);
	}

	@Override
	public void onResize(TResizeEvent resize) {
		super.onResize(resize);

		// Resize the text field
		textField.setWidth(resize.getWidth());
		textField.setHeight(resize.getHeight() - 2);
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
