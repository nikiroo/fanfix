package be.nikiroo.fanfix.reader;

import java.io.IOException;
import java.net.URL;

import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.utils.Progress;

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
				return pkg + "CliReader";
			case TUI:
				return pkg + "TuiReader";
			case GUI:
				return pkg + "GuiReader";
			}

			return null;
		}
	};

	/**
	 * Return the current {@link Story}.
	 * 
	 * @return the {@link Story}
	 */
	public Story getStory();

	/**
	 * The {@link LocalLibrary} to load the stories from (by default, takes the
	 * default {@link LocalLibrary}).
	 * 
	 * @return the {@link LocalLibrary}
	 */
	public BasicLibrary getLibrary();

	/**
	 * Change the {@link LocalLibrary} that will be managed by this
	 * {@link BasicReader}.
	 * 
	 * @param lib
	 *            the new {@link LocalLibrary}
	 */
	public void setLibrary(LocalLibrary lib);

	/**
	 * Create a new {@link BasicReader} for a {@link Story} in the
	 * {@link LocalLibrary}.
	 * 
	 * @param luid
	 *            the {@link Story} ID
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void setStory(String luid, Progress pg) throws IOException;

	/**
	 * Create a new {@link BasicReader} for an external {@link Story}.
	 * 
	 * @param source
	 *            the {@link Story} {@link URL}
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void setStory(URL source, Progress pg) throws IOException;

	/**
	 * Start the {@link Story} Reading.
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not
	 *             previously set
	 */
	public void read() throws IOException;

	/**
	 * Read the selected chapter (starting at 1).
	 * 
	 * @param chapter
	 *            the chapter
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not
	 *             previously set
	 */
	public void read(int chapter) throws IOException;

	/**
	 * Start the reader in browse mode for the given source (or pass NULL for
	 * all sources).
	 * 
	 * @param source
	 *            the type of {@link Story} to take into account, or NULL for
	 *            all
	 */
	public void browse(String source);

}
