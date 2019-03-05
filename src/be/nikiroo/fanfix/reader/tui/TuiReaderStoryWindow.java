package be.nikiroo.fanfix.reader.tui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jexer.TAction;
import jexer.TButton;
import jexer.TLabel;
import jexer.TTable;
import jexer.TText;
import jexer.TWindow;
import jexer.event.TResizeEvent;
import jexer.event.TResizeEvent.Type;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Paragraph.ParagraphType;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;

/**
 * This window will contain the {@link Story} in a readable format, with a
 * chapter browser.
 * 
 * @author niki
 */
class TuiReaderStoryWindow extends TWindow {
	private BasicLibrary lib;
	private MetaData meta;
	private Story story;
	private TLabel titleField;
	private TText textField;
	private TTable table;
	private int chapter = -99; // invalid value
	private List<TButton> navigationButtons;
	private TLabel currentChapter;

	// chapter: -1 for "none" (0 is desc)
	public TuiReaderStoryWindow(TuiReaderApplication app, BasicLibrary lib,
			MetaData meta, int chapter) {
		super(app, desc(meta), 0, 0, 60, 18, CENTERED | RESIZABLE);

		this.lib = lib;
		this.meta = meta;

		app.setStatusBar(this, desc(meta));

		// last = use window background
		titleField = new TLabel(this, "    Title", 0, 1, "tlabel", false);
		textField = new TText(this, "", 0, 3, getWidth() - 2, getHeight() - 5);
		table = new TTable(this, 0, 3, getWidth(), getHeight() - 4, null, null,
				Arrays.asList("Key", "Value"), true);

		navigationButtons = new ArrayList<TButton>(5);

		// -3 because 0-based and 2 for borders
		int row = getHeight() - 3;

		// for bg colour when << button is pressed
		navigationButtons.add(addButton(" ", 0, row, null));
		navigationButtons.add(addButton("<<  ", 0, row, new TAction() {
			@Override
			public void DO() {
				setChapter(-1);
			}
		}));
		navigationButtons.add(addButton("<  ", 4, row, new TAction() {
			@Override
			public void DO() {
				setChapter(TuiReaderStoryWindow.this.chapter - 1);
			}
		}));
		navigationButtons.add(addButton(">  ", 7, row, new TAction() {
			@Override
			public void DO() {
				setChapter(TuiReaderStoryWindow.this.chapter + 1);
			}
		}));
		navigationButtons.add(addButton(">>  ", 10, row, new TAction() {
			@Override
			public void DO() {
				setChapter(getStory().getChapters().size());
			}
		}));

		navigationButtons.get(0).setEnabled(false);
		navigationButtons.get(1).setEnabled(false);
		navigationButtons.get(2).setEnabled(false);

		currentChapter = addLabel("", 14, row);
		currentChapter.setWidth(getWidth() - 10);

		setChapter(chapter);
	}

	@Override
	public void onResize(TResizeEvent resize) {
		super.onResize(resize);

		// Resize the text field TODO: why setW/setH/reflow not enough for the
		// scrollbars?
		textField.onResize(new TResizeEvent(Type.WIDGET, resize.getWidth() - 2,
				resize.getHeight() - 5));

		table.setWidth(getWidth());
		table.setHeight(getHeight() - 4);
		table.reflowData();

		// -3 because 0-based and 2 for borders
		int row = getHeight() - 3;

		String name = currentChapter.getLabel();
		while (name.length() < resize.getWidth() - currentChapter.getX()) {
			name += " ";
		}
		currentChapter.setLabel(name);
		currentChapter.setWidth(resize.getWidth() - 10);
		currentChapter.setY(row);

		for (TButton button : navigationButtons) {
			button.setY(row);
		}
	}

	/**
	 * Display the current chapter in the window, or the {@link Story} info
	 * page.
	 * 
	 * @param chapter
	 *            the chapter (including "0" which is the description) or "-1"
	 *            to display the info page instead
	 */
	private void setChapter(int chapter) {
		if (chapter > getStory().getChapters().size()) {
			chapter = getStory().getChapters().size();
		}

		if (chapter != this.chapter) {
			this.chapter = chapter;

			int max = getStory().getChapters().size();
			navigationButtons.get(0).setEnabled(chapter > -1);
			navigationButtons.get(1).setEnabled(chapter > -1);
			navigationButtons.get(2).setEnabled(chapter > -1);
			navigationButtons.get(3).setEnabled(chapter < max);
			navigationButtons.get(4).setEnabled(chapter < max);

			if (chapter < 0) {
				displayInfoPage();
			} else {
				displayChapterPage();
			}
		}

		setCurrentChapterText();
	}

	/**
	 * Append the info page about the current {@link Story}.
	 * 
	 * @param builder
	 *            the builder to append to
	 */
	private void displayInfoPage() {
		textField.setVisible(false);
		table.setVisible(true);

		MetaData meta = getStory().getMeta();

		setCurrentTitle(meta.getTitle());

		table.setRowData(new String[][] { //
				new String[] { "Author", meta.getAuthor() }, //
				new String[] { "Publication date", meta.getDate() },
				new String[] { "Word count", Long.toString(meta.getWords()) },
				new String[] { "Source", meta.getSource() } //
		});
		table.setHeaders(Arrays.asList("key", "value"), false);
		table.toTop();
	}

	/**
	 * Append the current chapter.
	 * 
	 * @param builder
	 *            the builder to append to
	 */
	private void displayChapterPage() {
		table.setVisible(false);
		textField.setVisible(true);

		StringBuilder builder = new StringBuilder();

		Chapter chap = null;
		if (chapter == 0) {
			chap = getStory().getMeta().getResume();
		} else if (chapter > 0) {
			chap = getStory().getChapters().get(chapter - 1);
		}

		// TODO: i18n
		String chapName = chap == null ? "[No RESUME]" : chap.getName();
		setCurrentTitle(String.format("Chapter %d: %s", chapter, chapName));

		if (chap != null) {
			for (Paragraph para : chap) {
				if (para.getType() == ParagraphType.BREAK) {
					builder.append("\n");
				}
				builder.append(para.getContent()).append("\n");
				if (para.getType() == ParagraphType.BREAK) {
					builder.append("\n");
				}
			}
		}

		setText(builder.toString());
	}

	private Story getStory() {
		if (story == null) {
			// TODO: progress bar?
			story = lib.getStory(meta.getLuid(), null);
		}
		return story;
	}

	/**
	 * Display the given text on the window.
	 * 
	 * @param text
	 *            the text to display
	 */
	private void setText(String text) {
		textField.setText(text);
		textField.reflowData();
		textField.toTop();
	}

	/**
	 * Set the current chapter area to the correct value.
	 */
	private void setCurrentChapterText() {
		String name;
		if (chapter < 0) {
			name = " " + getStory().getMeta().getTitle();
		} else if (chapter == 0) {
			Chapter resume = getStory().getMeta().getResume();
			if (resume != null) {
				name = String.format(" %s", resume.getName());
			} else {
				// TODO: i18n
				name = "[No RESUME]";
			}
		} else {
			int max = getStory().getChapters().size();
			Chapter chap = getStory().getChapters().get(chapter - 1);
			name = String.format(" %d/%d: %s", chapter, max, chap.getName());
		}

		int width = getWidth() - currentChapter.getX();
		while (name.length() < width) {
			name += " ";
		}

		if (name.length() > width) {
			name = name.substring(0, width);
		}

		currentChapter.setLabel(name);
	}

	/**
	 * Set the current title in-window.
	 * 
	 * @param title
	 *            the new title
	 */
	private void setCurrentTitle(String title) {
		titleField.setWidth(title.length());
		titleField.setLabel(title);
	}

	private static String desc(MetaData meta) {
		return String.format("%s: %s", meta.getLuid(), meta.getTitle());
	}
}
