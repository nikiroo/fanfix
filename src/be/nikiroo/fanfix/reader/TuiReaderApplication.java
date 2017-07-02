package be.nikiroo.fanfix.reader;

import java.io.IOException;
import java.util.List;

import jexer.TApplication;
import jexer.TMessageBox;
import be.nikiroo.fanfix.data.MetaData;

class TuiReaderApplication extends TApplication {
	private BasicReader reader;

	private static BackendType guessBackendType() {
		// Swing is the default backend on Windows unless explicitly
		// overridden by jexer.Swing.
		TApplication.BackendType backendType = TApplication.BackendType.XTERM;
		if (System.getProperty("os.name").startsWith("Windows")) {
			backendType = TApplication.BackendType.SWING;
		}
		if (System.getProperty("os.name").startsWith("Mac")) {
			backendType = TApplication.BackendType.SWING;
		}
		if (System.getProperty("jexer.Swing") != null) {
			if (System.getProperty("jexer.Swing", "false").equals("true")) {
				backendType = TApplication.BackendType.SWING;
			} else {
				backendType = TApplication.BackendType.XTERM;
			}
		}
		return backendType;
	}

	public TuiReaderApplication(List<MetaData> stories, BasicReader reader)
			throws Exception {
		this(stories, reader, guessBackendType());
	}

	public TuiReaderApplication(List<MetaData> stories, BasicReader reader,
			TApplication.BackendType backend) throws Exception {
		super(backend);

		this.reader = reader;

		// Add the menus
		addFileMenu();
		addEditMenu();
		addWindowMenu();
		addHelpMenu();

		getBackend().setTitle("Fanfix");

		new TuiReaderMainWindow(this, stories);
	}

	public void open(MetaData meta) {
		// TODO: open in editor + external option
		if (!meta.isImageDocument()) {
			new TuiReaderStoryWindow(this, reader.getLibrary(), meta);
		} else {
			try {
				BasicReader.open(reader.getLibrary(), meta.getLuid());
			} catch (IOException e) {
				messageBox("Error when trying to open the story",
						e.getMessage(), TMessageBox.Type.OK);
			}
		}
	}
}
