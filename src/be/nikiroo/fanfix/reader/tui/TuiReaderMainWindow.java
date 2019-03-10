package be.nikiroo.fanfix.reader.tui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jexer.TAction;
import jexer.TComboBox;
import jexer.TCommand;
import jexer.TField;
import jexer.TFileOpenBox.Type;
import jexer.TKeypress;
import jexer.TLabel;
import jexer.TList;
import jexer.TStatusBar;
import jexer.TWindow;
import jexer.event.TCommandEvent;
import jexer.event.TKeypressEvent;
import jexer.event.TMenuEvent;
import jexer.event.TResizeEvent;
import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.reader.Reader;

/**
 * The library window, that will list all the (filtered) stories available in
 * this {@link BasicLibrary}.
 * 
 * @author niki
 */
class TuiReaderMainWindow extends TWindow {
	public static final int MENU_SEARCH = 1100;
	public static final TCommand CMD_SEARCH = new TCommand(MENU_SEARCH) {
	};

	public enum Mode {
		SOURCE, AUTHOR,
	}

	private TList list;
	private List<MetaData> listKeys;
	private List<String> listItems;
	private Reader reader;

	private Mode mode = Mode.SOURCE;
	private String target = null;
	private String filter = "";

	private List<TSizeConstraint> sizeConstraints = new ArrayList<TSizeConstraint>();

	// TODO: because no way to find out the current index!!
	private TComboBox select;
	private TComboBox option;

	/**
	 * Create a new {@link TuiReaderMainWindow} without any stories in the list.
	 * 
	 * @param reader
	 *            the reader and main application
	 */
	public TuiReaderMainWindow(TuiReaderApplication reader) {
		// Construct a demo window. X and Y don't matter because it will be
		// centred on screen.
		super(reader, "Library", 0, 0, 60, 18, CENTERED | RESIZABLE);

		this.reader = reader;

		listKeys = new ArrayList<MetaData>();
		listItems = new ArrayList<String>();

		addSearch();
		addList();
		addSelect(); // TODO: last so it can draw over the rest

		TStatusBar statusBar = reader.setStatusBar(this, "Library");
		statusBar.addShortcutKeypress(TKeypress.kbCtrlF, CMD_SEARCH, "Search");

		TSizeConstraint.resize(sizeConstraints);

		// TODO: remove when not used anymore

		// addLabel("Label (1,1)", 1, 1);
		// addButton("&Button (35,1)", 35, 1, new TAction() {
		// public void DO() {
		// }
		// });
		// addCheckbox(1, 2, "Checky (1,2)", false);
		// addProgressBar(1, 3, 30, 42);
		// TRadioGroup groupy = addRadioGroup(1, 4, "Radio groupy");
		// groupy.addRadioButton("Fanfan");
		// groupy.addRadioButton("Tulipe");
		// addField(1, 10, 20, false, "text not fixed.");
		// addField(1, 11, 20, true, "text fixed.");
		// addText("20x4 Text in (12,20)", 1, 12, 20, 4);
		//
		// TTreeView tree = addTreeView(30, 5, 20, 5);
		// TTreeItem root = new TTreeItem(tree, "expended root", true);
		// tree.setSelected(root); // needed to allow arrow navigation without
		// // mouse-clicking before
		//
		// root.addChild("child");
		// root.addChild("child 2").addChild("sub child");
	}

	private void addSearch() {
		TLabel lblSearch = addLabel("Search: ", 0, 0);

		TField search = new TField(this, 0, 0, 1, true) {
			@Override
			public void onKeypress(TKeypressEvent keypress) {
				super.onKeypress(keypress);
				TKeypress key = keypress.getKey();
				if (key.isFnKey() && key.getKeyCode() == TKeypress.ENTER) {
					TuiReaderMainWindow.this.filter = getText();
					TuiReaderMainWindow.this.refreshStories();
				}
			}
		};

		TSizeConstraint.setSize(sizeConstraints, lblSearch, 5, 1, null, null);
		TSizeConstraint.setSize(sizeConstraints, search, 15, 1, -5, null);
	}

	private void addList() {
		list = addList(listItems, 0, 0, 10, 10, new TAction() {
			@Override
			public void DO() {
				MetaData meta = getSelectedMeta();
				if (meta != null) {
					readStory(meta);
				}
			}
		});

		TSizeConstraint.setSize(sizeConstraints, list, 0, 7, 0, 0);
	}

	private void addSelect() {
		// TODO: make a full list
		final List<String> options = new ArrayList<String>();
		options.add("(show all)");
		options.add("Sources");
		options.add("Author");

		// TODO
		final List<String> selects = new ArrayList<String>();
		selects.add("(show all)");
		for (String source : reader.getLibrary().getSources()) {
			selects.add(source);
		}

		TLabel lblSelect = addLabel("Select: ", 0, 0);

		// TODO: why must be last so to be able to draw over the rest
		// TODO: make it so we cannot add manual entries
		// TODO: how to select the item via keyboard? why double-click via
		// mouse?
		// TODO: how to change the values size on resize?
		// TODO: setWidth() does not impact the display width, only the control
		// and the down arrow on the right
		// TODO: width 1 +resize + click on down arrow = bad format exception
		select = addComboBox(0, 0, 10, selects, 0,
				Math.min(selects.size() + 1, getHeight() - 1 - 1),
				new TAction() {
					@Override
					public void DO() {
						// TODO: detect (show all)
						if (select.getText().equals("(show all)")) {
							setMode(mode, null);
						} else {
							setMode(mode, select.getText());
						}
					}
				});

		option = addComboBox(0, 0, 10, options, 0,
				Math.min(selects.size() + 1, getHeight() - 1 - 1),
				new TAction() {
					@Override
					public void DO() {
						// TODO clear not working!!

						String smode = option.getText();
						Mode mode;
						if (smode == null || smode.equals("(show all)")) {
							mode = null;
							select.setVisible(false);
							select.setEnabled(false);
						} else if (smode.equals("Sources")) {
							mode = Mode.SOURCE;
							select.setVisible(true);
							select.setEnabled(true);
							selects.clear();
							selects.add("(show all)");
							for (String source : reader.getLibrary()
									.getSources()) {
								selects.add(source);
							}
						} else {
							mode = Mode.AUTHOR;
							select.setVisible(true);
							select.setEnabled(true);
							selects.clear();
							selects.add("(show all)");
							for (String author : reader.getLibrary()
									.getAuthors()) {
								selects.add(author);
							}
						}

						// TODO: detect (show all)
						if (select.getText().equals("(show all)")) {
							setMode(mode, null);
						} else {
							setMode(mode, select.getText());
						}
					}
				});

		TSizeConstraint.setSize(sizeConstraints, lblSelect, 5, 3, null, null);
		TSizeConstraint.setSize(sizeConstraints, option, 15, 3, -5, null);
		TSizeConstraint.setSize(sizeConstraints, select, 15, 4, -5, null);
	}

	@Override
	public void onResize(TResizeEvent resize) {
		super.onResize(resize);
		TSizeConstraint.resize(sizeConstraints);
	}

	@Override
	public void onClose() {
		setVisible(false);
		super.onClose();
	}

	/**
	 * Refresh the list of stories displayed in this library.
	 * <p>
	 * Will take the current settings into account (filter, source...).
	 */
	public void refreshStories() {
		List<MetaData> metas;
		if (mode == Mode.SOURCE) {
			metas = reader.getLibrary().getListBySource(target);
		} else if (mode == Mode.AUTHOR) {
			metas = reader.getLibrary().getListByAuthor(target);
		} else {
			metas = reader.getLibrary().getList();
		}

		setMetas(metas);
	}

	/**
	 * Change the author/source filter and display all stories matching this
	 * target.
	 * 
	 * @param mode
	 *            the new mode or NULL for no sorting
	 * @param target
	 *            the actual target for the given mode, or NULL for all of them
	 */
	public void setMode(Mode mode, String target) {
		this.mode = mode;
		this.target = target;
		refreshStories();
	}

	/**
	 * Update the list of stories displayed in this {@link TWindow}.
	 * <p>
	 * If a filter is set, only the stories which pass the filter will be
	 * displayed.
	 * 
	 * @param metas
	 *            the new list of stories to display
	 */
	private void setMetas(List<MetaData> metas) {
		listKeys.clear();
		listItems.clear();

		if (metas != null) {
			for (MetaData meta : metas) {
				String desc = desc(meta);
				if (filter.isEmpty()
						|| desc.toLowerCase().contains(filter.toLowerCase())) {
					listKeys.add(meta);
					listItems.add(desc);
				}
			}
		}

		list.setList(listItems);
	}

	public MetaData getSelectedMeta() {
		if (list.getSelectedIndex() >= 0) {
			return listKeys.get(list.getSelectedIndex());
		}

		return null;
	}

	public void readStory(MetaData meta) {
		try {
			reader.setChapter(-1);
			reader.setMeta(meta);
			reader.read();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	private String desc(MetaData meta) {
		return String.format("%5s: %s", meta.getLuid(), meta.getTitle());
	}

	@Override
	public void onCommand(TCommandEvent command) {
		if (command.getCmd().equals(TuiReaderApplication.CMD_EXIT)) {
			TuiReaderApplication.close(this);
		} else {
			// Handle our own event if needed here
			super.onCommand(command);
		}
	}

	@Override
	public void onMenu(TMenuEvent menu) {
		MetaData meta = getSelectedMeta();
		if (meta != null) {
			switch (menu.getId()) {
			case TuiReaderApplication.MENU_OPEN:
				readStory(meta);

				return;
			case TuiReaderApplication.MENU_EXPORT:

				try {
					// TODO: choose type, pg, error
					OutputType outputType = OutputType.EPUB;
					String path = fileOpenBox(".", Type.SAVE);
					reader.getLibrary().export(meta.getLuid(), outputType,
							path, null);
				} catch (IOException e) {
					// TODO
					e.printStackTrace();
				}

				return;

			case -1:
				try {
					reader.getLibrary().delete(meta.getLuid());
				} catch (IOException e) {
					// TODO
				}

				return;
			}
		}

		super.onMenu(menu);
	}
}