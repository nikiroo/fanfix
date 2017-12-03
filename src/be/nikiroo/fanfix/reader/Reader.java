package be.nikiroo.fanfix.reader;

import java.io.IOException;
import java.net.URL;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.utils.Progress;

/**
 * A {@link Reader} is a class that will handle {@link Story} reading and
 * browsing.
 * 
 * @author niki
 */
public interface Reader {
	/**
	 * A type of {@link BasicReader}.
	 * 
	 * @author niki
	 */
	public enum ReaderType {
		/** Simple reader that outputs everything on the console */
		CLI,
		/** Reader that starts local programs to handle the stories */
		GUI,
		/** A text (UTF-8) reader with menu and text windows */
		TUI,

		;

		/**
		 * Return the full class name of a type that implements said
		 * {@link ReaderType}.
		 * 
		 * @return the class name
		 */
		public String getTypeName() {
			String pkg = "be.nikiroo.fanfix.reader.";
			switch (this) {
			case CLI:
				return pkg + "cli.CliReader";
			case TUI:
				return pkg + "tui.TuiReader";
			case GUI:
				return pkg + "ui.GuiReader";
			}

			return null;
		}
	}

	/**
	 * Return the current target {@link MetaData}.
	 * 
	 * @return the meta
	 */
	public MetaData getMeta();

	/**
	 * Return the current {@link Story} as described by the current
	 * {@link MetaData}.
	 * 
	 * @param pg
	 *            the optional progress
	 * 
	 * @return the {@link Story}
	 */
	public Story getStory(Progress pg);

	/**
	 * The {@link BasicLibrary} to load the stories from (by default, takes the
	 * default {@link BasicLibrary}).
	 * 
	 * @return the {@link BasicLibrary}
	 */
	public BasicLibrary getLibrary();

	/**
	 * Change the {@link BasicLibrary} that will be managed by this
	 * {@link BasicReader}.
	 * 
	 * @param lib
	 *            the new {@link BasicLibrary}
	 */
	public void setLibrary(BasicLibrary lib);

	/**
	 * Set a {@link Story} from the current {@link BasicLibrary} into the
	 * {@link Reader}.
	 * 
	 * @param luid
	 *            the {@link Story} ID
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void setMeta(String luid) throws IOException;

	/**
	 * Set a {@link Story} from the current {@link BasicLibrary} into the
	 * {@link Reader}.
	 * 
	 * @param meta
	 *            the meta
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void setMeta(MetaData meta) throws IOException;

	/**
	 * Set an external {@link Story} into this {@link Reader}.
	 * 
	 * @param source
	 *            the {@link Story} {@link URL}
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void setMeta(URL source, Progress pg) throws IOException;

	/**
	 * Start the {@link Story} Reading.
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not
	 *             previously set
	 */
	public void read() throws IOException;

	/**
	 * The selected chapter to start reading at (starting at 1, 0 = description,
	 * -1 = none).
	 * 
	 * @return the chapter, or -1 for "no chapter"
	 */
	public int getChapter();

	/**
	 * The selected chapter to start reading at (starting at 1, 0 = description,
	 * -1 = none).
	 * 
	 * @param chapter
	 *            the chapter, or -1 for "no chapter"
	 */
	public void setChapter(int chapter);

	/**
	 * Start the reader in browse mode for the given source (or pass NULL for
	 * all sources).
	 * 
	 * @param source
	 *            the type of {@link Story} to take into account, or NULL for
	 *            all
	 */
	public void browse(String source);

	/**
	 * Open the {@link Story} with an external reader (the program will be
	 * passed the main file associated with this {@link Story}).
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to select the {@link Story} from
	 * @param luid
	 *            the {@link Story} LUID
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void openExternal(BasicLibrary lib, String luid) throws IOException;
}
