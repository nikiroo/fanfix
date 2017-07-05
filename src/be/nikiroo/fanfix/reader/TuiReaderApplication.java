package be.nikiroo.fanfix.reader;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import jexer.TApplication;
import jexer.TMessageBox;
import jexer.TWindow;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.utils.Progress;

/**
 * Manages the TUI reader, links and manages the {@link TWindow}s, starts the
 * correct output mode.
 * 
 * @author niki
 */
class TuiReaderApplication extends TApplication implements Reader {
	private Reader reader;

	public TuiReaderApplication(MetaData meta, int chapter, Reader reader,
			BackendType backend) throws Exception {
		this(reader, backend);

		new TuiReaderMainWindow(this, meta, chapter);
	}

	public TuiReaderApplication(List<MetaData> stories, Reader reader,
			TApplication.BackendType backend) throws Exception {
		this(reader, backend);

		new TuiReaderMainWindow(this, stories);
	}

	private TuiReaderApplication(Reader reader, TApplication.BackendType backend)
			throws Exception {
		super(backend);

		this.reader = reader;

		// Add the menus
		addFileMenu();
		addEditMenu();
		addWindowMenu();
		addHelpMenu();

		getBackend().setTitle("Fanfix");
	}

	public void read() throws IOException {
		reader.read();
	}

	public void read(int chapter) throws IOException {
		reader.read(chapter);
	}

	public void open(MetaData meta) {
		open(meta, -1);
	}

	public void open(MetaData meta, int chapter) {
		// TODO: open in editor + external option
		if (!meta.isImageDocument()) {
			new TuiReaderStoryWindow(this, getLibrary(), meta, chapter);
		} else {
			try {
				BasicReader.openExternal(getLibrary(), meta.getLuid());
			} catch (IOException e) {
				messageBox("Error when trying to open the story",
						e.getMessage(), TMessageBox.Type.OK);
			}
		}
	}

	public Story getStory() {
		return reader.getStory();
	}

	public BasicLibrary getLibrary() {
		return reader.getLibrary();
	}

	public void setLibrary(LocalLibrary lib) {
		reader.setLibrary(lib);
	}

	public void setStory(String luid, Progress pg) throws IOException {
		reader.setStory(luid, pg);
	}

	public void setStory(URL source, Progress pg) throws IOException {
		reader.setStory(source, pg);
	}

	public void browse(String source) {
		reader.browse(source);
	}
}
