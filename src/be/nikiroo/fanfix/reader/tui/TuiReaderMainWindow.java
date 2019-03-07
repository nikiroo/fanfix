package be.nikiroo.fanfix.reader.tui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jexer.TAction;
import jexer.TFileOpenBox.Type;
import jexer.TList;
import jexer.TWindow;
import jexer.event.TCommandEvent;
import jexer.event.TMenuEvent;
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
	private TList list;
	private List<MetaData> listKeys;
	private List<String> listItems;
	private Reader reader;
	private String source;

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

		// TODO size + onResize

		

		addLabel("Search: ", 5, 3);
		addField(15, 3, 5, true);
		
		addLabel("Sort by: ", 5, 1);
		// -1 = no default index (0 means first,...) 1=height when visible, null
		// = action
		List<String> data = Arrays.asList("(show all)", "Source", "Name", "Author");
		// must be last so to be able to draw over the rest
		// TODO: make it so we cannot add manual entries
		// TODO: how to select the item via keyboard? why double-click via mouse?
		addComboBox(15, 1, 12,
				data, 0, Math.min(data.size()+1,getHeight()-1-1),
				null);

		list = addList(listItems, 0, 7, getWidth(), getHeight(), new TAction() {
			@Override
			public void DO() {
				MetaData meta = getSelectedMeta();
				if (meta != null) {
					readStory(meta);
				}
			}
		});

		// TODO: add the current "source/type" or filter
		reader.setStatusBar(this, "Library");

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

	@Override
	public void onClose() {
		setVisible(false);
		super.onClose();
	}

	/**
	 * Change the source filter and display all stories matching this source.
	 * 
	 * @param source
	 *            the new source or NULL for all sources
	 */
	public void setSource(String source) {
		this.source = source;
		refreshStories();
	}

	public void refreshStories() {
		List<MetaData> metas = reader.getLibrary().getListBySource(source);
		setMetas(metas);
	}

	/**
	 * Update the list of stories displayed in this {@link TWindow}.
	 * 
	 * @param meta
	 *            the new (unique) story to display
	 */
	public void setMeta(MetaData meta) {
		List<MetaData> metas = new ArrayList<MetaData>();
		if (meta != null) {
			metas.add(meta);
		}

		setMetas(metas);
	}

	/**
	 * Update the list of stories displayed in this {@link TWindow}.
	 * 
	 * @param metas
	 *            the new list of stories to display
	 */
	private void setMetas(List<MetaData> metas) {
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