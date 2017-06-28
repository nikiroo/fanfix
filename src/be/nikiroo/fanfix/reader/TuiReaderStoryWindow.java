package be.nikiroo.fanfix.reader;

import java.util.ArrayList;
import java.util.List;

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
	private int chapter = -2;
	private List<TButton> navigationButtons;
	private TLabel chapterName;

	public TuiReaderStoryWindow(TApplication app, MetaData meta) {
		this(app, meta, 0);
	}

	public TuiReaderStoryWindow(TApplication app, MetaData meta, int chapter) {
		super(app, desc(meta), 0, 0, 60, 18, CENTERED | RESIZABLE);
		this.meta = meta;

		// TODO: show all meta info before?

		// -2 because 0-based, 2 for borders, -1 to hide the HScroll
		textField = addText("", 0, 0, getWidth() - 2, getHeight() - 2);

		statusBar = newStatusBar(desc(meta));
		statusBar.addShortcutKeypress(TKeypress.kbF10, TCommand.cmExit, "Exit");

		navigationButtons = new ArrayList<TButton>(4);

		// -3 because 0-based and 2 for borders
		int row = getHeight() - 3;

		navigationButtons.add(addButton("<<", 0, row, new TAction() {
			public void DO() {
				setChapter(0);
			}
		}));
		navigationButtons.add(addButton("<", 3, row, new TAction() {
			public void DO() {
				setChapter(TuiReaderStoryWindow.this.chapter - 1);
			}
		}));
		navigationButtons.add(addButton(">", 5, row, new TAction() {
			public void DO() {
				setChapter(TuiReaderStoryWindow.this.chapter + 1);
			}
		}));
		navigationButtons.add(addButton(">>", 7, row, new TAction() {
			public void DO() {
				setChapter(getStory().getChapters().size());
			}
		}));

		chapterName = addLabel("", 11, row);
		chapterName.setWidth(getWidth() - 10);
		setChapter(chapter);
	}

	@Override
	public void onResize(TResizeEvent resize) {
		super.onResize(resize);

		// Resize the text field
		textField.setWidth(resize.getWidth());
		textField.setHeight(resize.getHeight() - 2);
		textField.reflow();

		// -3 because 0-based and 2 for borders
		int row = getHeight() - 3;

		String name = chapterName.getLabel();
		while (name.length() < resize.getWidth() - chapterName.getX()) {
			name += " ";
		}
		chapterName.setLabel(name);
		chapterName.setWidth(resize.getWidth() - 10);
		chapterName.setY(row);

		for (TButton button : navigationButtons) {
			button.setY(row);
		}
	}

	private void setChapter(int chapter) {
		if (chapter < 0) {
			chapter = 0;
		}

		if (chapter > getStory().getChapters().size()) {
			chapter = getStory().getChapters().size();
		}

		if (chapter != this.chapter) {
			this.chapter = chapter;
			Chapter chap;
			String name;
			if (chapter == 0) {
				chap = getStory().getMeta().getResume();
				name = String.format(" %s", chap.getName());
			} else {
				chap = getStory().getChapters().get(chapter - 1);
				name = String.format(" %d/%d: %s", chapter, getStory()
						.getChapters().size(), chap.getName());
			}

			while (name.length() < getWidth() - chapterName.getX()) {
				name += " ";
			}

			chapterName.setLabel(name);

			StringBuilder builder = new StringBuilder();
			// TODO: i18n
			String c = String.format("Chapter %d: %s", chapter, chap.getName());
			builder.append(c).append("\n");
			for (int i = 0; i < c.length(); i++) {
				builder.append("═");
			}
			builder.append("\n\n");
			for (Paragraph para : chap) {
				builder.append(para.getContent()).append("\n\n");
			}
			textField.setText(builder.toString());
			textField.reflow();
		}
	}

	private Story getStory() {
		if (story == null) {
			// TODO: progress bar?
			story = Instance.getLibrary().getStory(meta.getLuid(), null);
		}
		return story;
	}

	private static String desc(MetaData meta) {
		return String.format("%s: %s", meta.getLuid(), meta.getTitle());
	}
}
