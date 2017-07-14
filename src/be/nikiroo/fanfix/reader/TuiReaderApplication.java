package be.nikiroo.fanfix.reader;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import jexer.TApplication;
import jexer.TMessageBox;
import jexer.TWindow;
import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.Instance.SyserrHandler;
import be.nikiroo.fanfix.Instance.TraceHandler;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
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
	private Reader reader;

	// start reading if meta present
	public TuiReaderApplication(Reader reader, BackendType backend)
			throws Exception {
		super(backend);
		init(reader);

		MetaData meta = getMeta();

		new TuiReaderMainWindow(this).setMeta(meta);

		if (meta != null) {
			read();
		}
	}

	public TuiReaderApplication(List<MetaData> stories, Reader reader,
			TApplication.BackendType backend) throws Exception {
		super(backend);
		init(reader);

		new TuiReaderMainWindow(this).setMetas(stories);
	}

	@SuppressWarnings("unused")
	@Override
	public void read() throws IOException {
		MetaData meta = getMeta();

		if (meta == null) {
			throw new IOException("No story to read");
		}

		// TODO: open in editor + external option
		if (!meta.isImageDocument()) {
			new TuiReaderStoryWindow(this, getLibrary(), meta, getChapter());
		} else {
			try {
				BasicReader.openExternal(getLibrary(), meta.getLuid());
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

		// Do not allow traces/debug to pollute the screen:
		Instance.setSyserrHandler(new SyserrHandler() {
			@Override
			public void notify(Exception e, boolean showDetails) {
				// TODO
			}
		});

		Instance.setTraceHandler(new TraceHandler() {
			@Override
			public void trace(String message) {
				// TODO
			}
		});
		//

		// Add the menus
		addFileMenu();
		addEditMenu();
		addWindowMenu();
		addHelpMenu();

		getBackend().setTitle("Fanfix");
	}
}
