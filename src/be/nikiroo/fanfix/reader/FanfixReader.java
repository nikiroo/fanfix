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
public abstract class FanfixReader {
	private Story story;

	/**
	 * Return the current {@link Story}.
	 * 
	 * @return the {@link Story}
	 */
	public Story getStory() {
		return story;
	}

	/**
	 * Create a new {@link FanfixReader} for a {@link Story} in the
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
	 * Create a new {@link FanfixReader} for an external {@link Story}.
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
}
