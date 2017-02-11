package be.nikiroo.fanfix.reader;

import java.io.IOException;
import java.net.URL;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.Library;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.BasicSupport.SupportType;

/**
 * Command line {@link Story} reader.
 * <p>
 * Will output stories to the console.
 * 
 * @author niki
 */
public abstract class BasicReader {
	public enum ReaderType {
		/** Simple reader that outputs everything on the console */
		CLI,
		/** Reader that starts local programs to handle the stories */
		LOCAL
	}

	private static ReaderType defaultType;
	private Story story;
	private ReaderType type;

	static {
		// TODO: default type from config
	}

	/**
	 * The type of this reader.
	 * 
	 * @return the type
	 */
	public ReaderType getType() {
		return type;
	}

	/**
	 * The type of this reader.
	 * 
	 * @param type
	 *            the new type
	 */
	protected BasicReader setType(ReaderType type) {
		this.type = type;
		return this;
	}

	/**
	 * Return the current {@link Story}.
	 * 
	 * @return the {@link Story}
	 */
	public Story getStory() {
		return story;
	}

	/**
	 * Create a new {@link BasicReader} for a {@link Story} in the
	 * {@link Library} .
	 * 
	 * @param luid
	 *            the {@link Story} ID
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void setStory(String luid) throws IOException {
		story = Instance.getLibrary().getStory(luid);
		if (story == null) {
			throw new IOException("Cannot retrieve story from library: " + luid);
		}
	}

	/**
	 * Create a new {@link BasicReader} for an external {@link Story}.
	 * 
	 * @param source
	 *            the {@link Story} {@link URL}
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void setStory(URL source) throws IOException {
		BasicSupport support = BasicSupport.getSupport(source);
		if (support == null) {
			throw new IOException("URL not supported: " + source.toString());
		}

		story = support.process(source);
		if (story == null) {
			throw new IOException(
					"Cannot retrieve story from external source: "
							+ source.toString());

		}
	}

	/**
	 * Start the {@link Story} Reading.
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not
	 *             previously set
	 */
	public abstract void read() throws IOException;

	/**
	 * Read the selected chapter (starting at 1).
	 * 
	 * @param chapter
	 *            the chapter
	 */
	public abstract void read(int chapter);

	/**
	 * Start the reader in browse mode for the given type (or pass NULL for all
	 * types).
	 * 
	 * @param type
	 *            the type of {@link Story} to take into account, or NULL for
	 *            all
	 */
	public abstract void start(SupportType type);

	/**
	 * Return a new {@link BasicReader} ready for use.
	 * 
	 * @return a {@link BasicReader}
	 */
	public static BasicReader getReader() {
		switch (defaultType) {
		// case LOCAL:
		// return new LocalReader().setType(ReaderType.LOCAL);
		case CLI:
			return new CliReader().setType(ReaderType.CLI);
		}

		return null;
	}

	/**
	 * The default {@link ReaderType} used when calling
	 * {@link BasicReader#getReader()}.
	 * 
	 * @return the default type
	 */
	public static ReaderType getDefaultReaderType() {
		return defaultType;
	}

	/**
	 * The default {@link ReaderType} used when calling
	 * {@link BasicReader#getReader()}.
	 * 
	 * @param defaultType
	 *            the new default type
	 */
	public static void setDefaultReaderType(ReaderType defaultType) {
		BasicReader.defaultType = defaultType;
	}
}
