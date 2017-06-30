package be.nikiroo.fanfix.reader;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.Library;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.ui.UIUtils;
import be.nikiroo.utils.serial.SerialUtils;

/**
 * The class that handles the different {@link Story} readers you can use.
 * <p>
 * All the readers should be accessed via {@link BasicReader#getReader()}.
 * 
 * @author niki
 */
public abstract class BasicReader {
	public enum ReaderType {
		/** Simple reader that outputs everything on the console */
		CLI,
		/** Reader that starts local programs to handle the stories */
		GUI,
		/** A text (UTF-8) reader with menu and text windows */
		TUI,
		
		;
		
		public String getTypeName() {
			String pkg = "be.nikiroo.fanfix.reader.";
			switch (this) {
				case CLI: return pkg + "CliReader";
				case TUI: return pkg + "TuiReader";
				case GUI: return pkg + "LocalReader";
			}
			
			return null;
		}
	}

	private static ReaderType defaultType = ReaderType.GUI;
	private Story story;
	private ReaderType type;

	/**
	 * Take the default reader type configuration from the config file.
	 */
	static {
		String typeString = Instance.getConfig().getString(Config.READER_TYPE);
		if (typeString != null && !typeString.isEmpty()) {
			try {
				ReaderType type = ReaderType.valueOf(typeString.toUpperCase());
				defaultType = type;
			} catch (IllegalArgumentException e) {
				// Do nothing
			}
		}
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
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void setStory(String luid, Progress pg) throws IOException {
		story = Instance.getLibrary().getStory(luid, pg);
		if (story == null) {
			throw new IOException("Cannot retrieve story from library: " + luid);
		}
	}

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
	public void setStory(URL source, Progress pg) throws IOException {
		BasicSupport support = BasicSupport.getSupport(source);
		if (support == null) {
			throw new IOException("URL not supported: " + source.toString());
		}

		story = support.process(source, pg);
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
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not
	 *             previously set
	 */
	public abstract void read(int chapter) throws IOException;

	/**
	 * Start the reader in browse mode for the given type (or pass NULL for all
	 * types).
	 * 
	 * @param type
	 *            the type of {@link Story} to take into account, or NULL for
	 *            all
	 */
	public abstract void start(String type);

	/**
	 * Return a new {@link BasicReader} ready for use if one is configured.
	 * <p>
	 * Can return NULL if none are configured.
	 * 
	 * @return a {@link BasicReader}, or NULL if none configured
	 */
	public static BasicReader getReader() {
		try {
			if (defaultType != null) {
				return ((BasicReader)SerialUtils.createObject
					(defaultType.getTypeName())).setType(defaultType);
			}
		} catch (Exception e) {
			Instance.syserr(new Exception("Cannot create a reader of type: "
					+ defaultType + " (Not compiled in?)", e));
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

	/**
	 * Return an {@link URL} from this {@link String}, be it a file path or an
	 * actual {@link URL}.
	 * 
	 * @param sourceString
	 *            the source
	 * 
	 * @return the corresponding {@link URL}
	 * 
	 * @throws MalformedURLException
	 *             if this is neither a file nor a conventional {@link URL}
	 */
	public static URL getUrl(String sourceString) throws MalformedURLException {
		if (sourceString == null || sourceString.isEmpty()) {
			throw new MalformedURLException("Empty url");
		}

		URL source = null;
		try {
			source = new URL(sourceString);
		} catch (MalformedURLException e) {
			File sourceFile = new File(sourceString);
			source = sourceFile.toURI().toURL();
		}

		return source;
	}

	// open with external player the related file
	public static void open(String luid) throws IOException {
		MetaData meta = Instance.getLibrary().getInfo(luid);
		File target = Instance.getLibrary().getFile(luid);

		open(meta, target);
	}

	// open with external player the related file
	protected static void open(MetaData meta, File target) throws IOException {
		String program = null;
		if (meta.isImageDocument()) {
			program = Instance.getUiConfig().getString(
					UiConfig.IMAGES_DOCUMENT_READER);
		} else {
			program = Instance.getUiConfig().getString(
					UiConfig.NON_IMAGES_DOCUMENT_READER);
		}

		if (program != null && program.trim().isEmpty()) {
			program = null;
		}

		if (program == null) {
			try {
				Desktop.getDesktop().browse(target.toURI());
			} catch (UnsupportedOperationException e) {
				Runtime.getRuntime().exec(
						new String[] { "xdg-open", target.getAbsolutePath() });

			}
		} else {
			Runtime.getRuntime().exec(
					new String[] { program, target.getAbsolutePath() });
		}
	}
}
