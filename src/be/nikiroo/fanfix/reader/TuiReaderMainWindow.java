package be.nikiroo.fanfix.reader;

import java.util.ArrayList;
import java.util.List;

import jexer.TAction;
import jexer.TCommand;
import jexer.TKeypress;
import jexer.TList;
import jexer.TRadioGroup;
import jexer.TTreeItem;
import jexer.TTreeView;
import jexer.TWindow;
import be.nikiroo.fanfix.data.MetaData;

/**
 * The library window, that will list all the (filtered) stories available in
 * this {@link Library}.
 * 
 * @author niki
 */
class TuiReaderMainWindow extends TWindow {
	private TList list;
	private List<MetaData> listKeys;
	private List<String> listItems;
	private TuiReaderApplication reader;

	/**
	 * Create a new {@link TuiReaderMainWindow} with the given stories in the
	 * list.
	 * 
	 * @param reader
	 *            the reader and main application
	 * @param metas
	 *            the stories to display
	 */
	public TuiReaderMainWindow(TuiReaderApplication reader, List<MetaData> metas) {
		this(reader);
		setMetas(metas);
	}

	/**
	 * Create a new {@link TuiReaderMainWindow} with only the given
	 * {@link MetaData} in the list, and open this {@link MetaData} at the given
	 * chapter.
	 * 
	 * @param reader
	 *            the reader and main application
	 * @param meta
	 *            the story to display
	 * @param chapter
	 *            the chapter to open
	 */
	public TuiReaderMainWindow(TuiReaderApplication reader, MetaData meta,
			int chapter) {
		this(reader);

		List<MetaData> metas = new ArrayList<MetaData>();
		metas.add(meta);
		setMetas(metas);

		reader.open(meta, chapter);
	}

	/**
	 * Create a new {@link TuiReaderMainWindow} without any stories in the list.
	 * 
	 * @param reader
	 *            the reader and main application
	 */
	public TuiReaderMainWindow(TuiReaderApplication reader) {
		// Construct a demo window. X and Y don't matter because it will be
		// centered on screen.
		super(reader, "Library", 0, 0, 60, 18, CENTERED | RESIZABLE
				| UNCLOSABLE);

		this.reader = reader;

		maximize();

		listKeys = new ArrayList<MetaData>();
		listItems = new ArrayList<String>();
		list = addList(listItems, 0, 0, getWidth(), getHeight(), new TAction() {
			@Override
			public void DO() {
				if (list.getSelectedIndex() >= 0) {
					enterOnStory(listKeys.get(list.getSelectedIndex()));
				}
			}
		});

		// TODO: add the current "source/type" or filter
		statusBar = newStatusBar("Library");
		statusBar.addShortcutKeypress(TKeypress.kbF10, TCommand.cmExit, "Exit");

		if (false) {
			addLabel("Label (1,1)", 1, 1);
			addButton("&Button (35,1)", 35, 1, new TAction() {
				public void DO() {
				}
			});
			addCheckbox(1, 2, "Checky (1,2)", false);
			addProgressBar(1, 3, 30, 42);
			TRadioGroup groupy = addRadioGroup(1, 4, "Radio groupy");
			groupy.addRadioButton("Fanfan");
			groupy.addRadioButton("Tulipe");
			addField(1, 10, 20, false, "text not fixed.");
			addField(1, 11, 20, true, "text fixed.");
			addText("20x4 Text in (12,20)", 1, 12, 20, 4);

			TTreeView tree = addTreeView(30, 5, 20, 5);
			TTreeItem root = new TTreeItem(tree, "expended root", true);
			tree.setSelected(root); // needed to allow arrow navigation without
									// mouse-clicking before

			root.addChild("child");
			root.addChild("child 2").addChild("sub child");

		}
	}

	/**
	 * Update the list of stories displayed in this {@link TWindow}.
	 * 
	 * @param metas
	 *            the new list of stories to display
	 */
	public void setMetas(List<MetaData> metas) {
		listKeys.clear();
		listItems.clear();

		if (metas != null) {
			for (MetaData meta : metas) {
				listKeys.add(meta);
				listItems.add(desc(meta));
			}
		}

		list.setList(listItems);
	}

	private void enterOnStory(MetaData meta) {
		reader.open(meta);
	}

	private String desc(MetaData meta) {
		return String.format("%5s: %s", meta.getLuid(), meta.getTitle());
	}
}
