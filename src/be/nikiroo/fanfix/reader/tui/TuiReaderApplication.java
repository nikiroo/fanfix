package be.nikiroo.fanfix.reader.tui;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.IOException;
import java.net.URL;

import jexer.TApplication;
import jexer.TCommand;
import jexer.TKeypress;
import jexer.TMessageBox;
import jexer.TStatusBar;
import jexer.TWindow;
import jexer.event.TMenuEvent;
import jexer.menu.TMenu;
import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.Reader;
import be.nikiroo.utils.Progress;

/**
 * Manages the TUI general mode and links and manages the {@link TWindow}s.
 * <p>
 * It will also enclose a {@link Reader} and simply handle the reading part
 * differently (it will create the required sub-windows and display them).
 * 
 * @author niki
 */
class TuiReaderApplication extends TApplication implements Reader {
	public static final int MENU_OPEN = 1025;
	public static final int MENU_IMPORT_URL = 1026;
	public static final int MENU_IMPORT_FILE = 1027;
	public static final int MENU_EXPORT = 1028;
	public static final int MENU_EXIT = 1029;

	private Reader reader;
	private TuiReaderMainWindow main;

	// start reading if meta present
	public TuiReaderApplication(Reader reader, BackendType backend)
			throws Exception {
		super(backend);
		init(reader);

		MetaData meta = getMeta();

		main = new TuiReaderMainWindow(this);
		main.setMeta(meta);
		if (meta != null) {
			read();
		}
	}

	public TuiReaderApplication(Reader reader, String source,
			TApplication.BackendType backend) throws Exception {
		super(backend);

		init(reader);

		main = new TuiReaderMainWindow(this);
		main.setSource(source);
	}

	@Override
	public void read() throws IOException {
		MetaData meta = getMeta();

		if (meta == null) {
			throw new IOException("No story to read");
		}

		// TODO: open in editor + external option
		if (!meta.isImageDocument()) {
			@SuppressWarnings("unused")
			Object discard = new TuiReaderStoryWindow(this, getLibrary(), meta,
					getChapter());
		} else {
			try {
				openExternal(getLibrary(), meta.getLuid());
			} catch (IOException e) {
				messageBox("Error when trying to open the story",
						e.getMessage(), TMessageBox.Type.OK);
			}
		}
	}

	@Override
	public MetaData getMeta() {
		return reader.getMeta();
	}

	@Override
	public Story getStory(Progress pg) {
		return reader.getStory(pg);
	}

	@Override
	public BasicLibrary getLibrary() {
		return reader.getLibrary();
	}

	@Override
	public void setLibrary(BasicLibrary lib) {
		reader.setLibrary(lib);
	}

	@Override
	public void setMeta(MetaData meta) throws IOException {
		reader.setMeta(meta);
	}

	@Override
	public void setMeta(String luid) throws IOException {
		reader.setMeta(luid);
	}

	@Override
	public void setMeta(URL source, Progress pg) throws IOException {
		reader.setMeta(source, pg);
	}

	@Override
	public void browse(String source) {
		reader.browse(source);
	}

	@Override
	public int getChapter() {
		return reader.getChapter();
	}

	@Override
	public void setChapter(int chapter) {
		reader.setChapter(chapter);
	}

	private void init(Reader reader) {
		this.reader = reader;

		// TODO: traces/errors?
		Instance.setTraceHandler(null);

		// Add the menus TODO: i18n
		TMenu fileMenu = addMenu("&File");
		fileMenu.addItem(MENU_OPEN, "&Open");
		fileMenu.addItem(MENU_EXPORT, "&Save as...");
		// TODO: Move to...
		fileMenu.addSeparator();
		fileMenu.addItem(MENU_IMPORT_URL, "Import &URL...");
		fileMenu.addItem(MENU_IMPORT_FILE, "Import &file...");
		fileMenu.addSeparator();
		fileMenu.addItem(MENU_EXIT, "E&xit");

		TStatusBar statusBar = fileMenu.newStatusBar("File-management "
				+ "commands (Open, Save, Print, etc.)");
		// TODO: doesn't actually work:
		statusBar.addShortcutKeypress(TKeypress.kbF10, TCommand.cmExit, "Exit");

		// TODO: Edit: re-download, delete

		//

		addWindowMenu();

		getBackend().setTitle("Fanfix");
	}

	@Override
	protected boolean onMenu(TMenuEvent menu) {
		// TODO: i18n
		switch (menu.getId()) {
		case MENU_EXIT:
			if (messageBox("Confirmation", "(TODO: i18n) Exit application?",
					TMessageBox.Type.YESNO).getResult() == TMessageBox.Result.YES) {
				// exit(false);
			}

			return true;
		case MENU_IMPORT_URL:
			String clipboard = "";
			try {
				clipboard = ("" + Toolkit.getDefaultToolkit()
						.getSystemClipboard().getData(DataFlavor.stringFlavor))
						.trim();
			} catch (Exception e) {
				// No data will be handled
			}

			if (clipboard == null || !clipboard.startsWith("http")) {
				clipboard = "";
			}

			String url = inputBox("Import story", "URL to import", clipboard)
					.getText();

			if (!imprt(url)) {
				// TODO: bad import
			}

			return true;
		case MENU_IMPORT_FILE:
			try {
				String filename = fileOpenBox(".");
				if (!imprt(filename)) {
					// TODO: bad import
				}
			} catch (IOException e) {
				// TODO: bad file
				e.printStackTrace();
			}

			return true;
		}

		return super.onMenu(menu);
	}

	private boolean imprt(String url) {
		try {
			reader.getLibrary().imprt(BasicReader.getUrl(url), null);
			main.refreshStories();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public void openExternal(BasicLibrary lib, String luid) throws IOException {
		reader.openExternal(lib, luid);
	}
}
