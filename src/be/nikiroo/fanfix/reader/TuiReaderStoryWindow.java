package be.nikiroo.fanfix.reader;

import java.util.ArrayList;
import java.util.List;

import jexer.TAction;
import jexer.TApplication;
import jexer.TButton;
import jexer.TCommand;
import jexer.TKeypress;
import jexer.TLabel;
import jexer.TText;
import jexer.TWindow;
import jexer.event.TResizeEvent;
import be.nikiroo.fanfix.BasicLibrary;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;

class TuiReaderStoryWindow extends TWindow {
	private BasicLibrary lib;
	private MetaData meta;
	private Story story;
	private TText textField;
	private int chapter = -2;
	private List<TButton> navigationButtons;
	private TLabel chapterName;

	public TuiReaderStoryWindow(TApplication app, BasicLibrary lib,
			MetaData meta) {
		this(app, lib, meta, 0);
	}

	public TuiReaderStoryWindow(TApplication app, BasicLibrary lib,
			MetaData meta, int chapter) {
		super(app, desc(meta), 0, 0, 60, 18, CENTERED | RESIZABLE);

		this.lib = lib;
		this.meta = meta;

		// TODO: show all meta info before?

		textField = new TText(this, "", 0, 0, getWidth() - 2, getHeight() - 2);

		statusBar = newStatusBar(desc(meta));
		statusBar.addShortcutKeypress(TKeypress.kbF10, TCommand.cmExit, "Exit");

		navigationButtons = new ArrayList<TButton>(5);

		// -3 because 0-based and 2 for borders
		int row = getHeight() - 3;

		navigationButtons.add(addButton(" ", 0, row, null)); // for bg colour
																// when <<
																// button is
																// pressed
		navigationButtons.add(addButton("<<  ", 0, row, new TAction() {
			public void DO() {
				setChapter(0);
			}
		}));
		navigationButtons.add(addButton("<  ", 4, row, new TAction() {
			public void DO() {
				setChapter(TuiReaderStoryWindow.this.chapter - 1);
			}
		}));
		navigationButtons.add(addButton(">  ", 7, row, new TAction() {
			public void DO() {
				setChapter(TuiReaderStoryWindow.this.chapter + 1);
			}
		}));
		navigationButtons.add(addButton(">>  ", 10, row, new TAction() {
			public void DO() {
				setChapter(getStory().getChapters().size());
			}
		}));

		navigationButtons.get(0).setEnabled(false);
		navigationButtons.get(1).setEnabled(false);
		navigationButtons.get(2).setEnabled(false);

		chapterName = addLabel("", 14, row);
		chapterName.setWidth(getWidth() - 10);
		setChapter(chapter);
	}

	@Override
	public void onResize(TResizeEvent resize) {
		super.onResize(resize);

		// Resize the text field
		textField.setWidth(resize.getWidth() - 2);
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

			int max = getStory().getChapters().size();
			navigationButtons.get(0).setEnabled(chapter > 0);
			navigationButtons.get(1).setEnabled(chapter > 0);
			navigationButtons.get(2).setEnabled(chapter > 0);
			navigationButtons.get(3).setEnabled(chapter < max);
			navigationButtons.get(4).setEnabled(chapter < max);

			Chapter chap;
			String name;
			if (chapter == 0) {
				chap = getStory().getMeta().getResume();
				name = String.format(" %s", chap.getName());
			} else {
				chap = getStory().getChapters().get(chapter - 1);
				name = String
						.format(" %d/%d: %s", chapter, max, chap.getName());
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
			textField.toTop();
		}
	}

	private Story getStory() {
		if (story == null) {
			// TODO: progress bar?
			story = lib.getStory(meta.getLuid(), null);
		}
		return story;
	}

	private static String desc(MetaData meta) {
		return String.format("%s: %s", meta.getLuid(), meta.getTitle());
	}
}
