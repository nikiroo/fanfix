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
import jexer.TWidget;
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
	public static final int MENU_LIBRARY = 1029;
	public static final int MENU_EXIT = 1030;

	public static final TCommand CMD_EXIT = new TCommand(MENU_EXIT) {
	};

	private Reader reader;
	private TuiReaderMainWindow main;

	private MetaData meta;
	private String source;
	private boolean useMeta;

	// start reading if meta present
	public TuiReaderApplication(Reader reader, BackendType backend)
			throws Exception {
		super(backend);
		init(reader);

		showMain(getMeta(), null, true);
	}

	public TuiReaderApplication(Reader reader, String source,
			TApplication.BackendType backend) throws Exception {
		super(backend);
		init(reader);

		showMain(null, source, false);
	}

	@Override
	public void read() throws IOException {
		MetaData meta = getMeta();

		if (meta == null) {
			throw new IOException("No story to read");
		}

		// TODO: open in editor + external option
		if (!meta.isImageDocument()) {
			TWindow window = new TuiReaderStoryWindow(this, getLibrary(), meta,
					getChapter());
			window.maximize();
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

	/**
	 * Set the default status bar when this window appear.
	 * <p>
	 * Some shortcuts are always visible, and will be put here.
	 * <p>
	 * Note that shortcuts placed this way on menu won't work unless the menu
	 * also implement them.
	 * 
	 * @param window
	 *            the new window or menu on screen
	 * @param description
	 *            the description to show on the status ba
	 */
	public TStatusBar setStatusBar(TWindow window, String description) {
		TStatusBar statusBar = window.newStatusBar(description);
		statusBar.addShortcutKeypress(TKeypress.kbF10, CMD_EXIT, "Exit");
		return statusBar;

	}

	private void showMain(MetaData meta, String source, boolean useMeta)
			throws IOException {
		// TODO: thread-safety
		this.meta = meta;
		this.source = source;
		this.useMeta = useMeta;

		if (main != null && main.isVisible()) {
			main.activate();
		} else {
			if (main != null) {
				main.close();
			}
			main = new TuiReaderMainWindow(this);
			if (useMeta) {
				main.setMeta(meta);
				if (meta != null) {
					read();
				}
			} else {
				main.setSource(source);
			}
		}
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
		fileMenu.addItem(MENU_LIBRARY, "Lib&rary");
		fileMenu.addSeparator();
		fileMenu.addItem(MENU_EXIT, "E&xit");

		setStatusBar(fileMenu, "File-management "
				+ "commands (Open, Save, Print, etc.)");

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
			close(this);
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
		case MENU_LIBRARY:
			try {
				showMain(meta, source, useMeta);
			} catch (IOException e) {
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

	/**
	 * Ask the user and, if confirmed, close the {@link TApplication} this
	 * {@link TWidget} is running on.
	 * <p>
	 * This should result in the program terminating.
	 * 
	 * @param widget
	 *            the {@link TWidget}
	 */
	static public void close(TWidget widget) {
		close(widget.getApplication());
	}

	/**
	 * Ask the user and, if confirmed, close the {@link TApplication}.
	 * <p>
	 * This should result in the program terminating.
	 * 
	 * @param app
	 *            the {@link TApplication}
	 */
	static void close(TApplication app) {
		// TODO: i18n
		if (app.messageBox("Confirmation", "(TODO: i18n) Exit application?",
				TMessageBox.Type.YESNO).getResult() == TMessageBox.Result.YES) {
			app.exit();
		}
	}
}
