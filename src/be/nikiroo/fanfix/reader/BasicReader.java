package be.nikiroo.fanfix.reader;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.serial.SerialUtils;

/**
 * The class that handles the different {@link Story} readers you can use.
 * <p>
 * All the readers should be accessed via {@link BasicReader#getReader()}.
 * 
 * @author niki
 */
public abstract class BasicReader implements Reader {
	private static BasicLibrary defaultLibrary = Instance.getLibrary();
	private static ReaderType defaultType = ReaderType.GUI;

	private BasicLibrary lib;
	private Story story;

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

	public Story getStory() {
		return story;
	}

	public BasicLibrary getLibrary() {
		if (lib == null) {
			lib = defaultLibrary;
		}

		return lib;
	}

	public void setLibrary(LocalLibrary lib) {
		this.lib = lib;
	}

	public void setStory(String luid, Progress pg) throws IOException {
		story = getLibrary().getStory(luid, pg);
		if (story == null) {
			throw new IOException("Cannot retrieve story from library: " + luid);
		}
	}

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

	public void read() throws IOException {
		read(-1);
	}

	/**
	 * Return a new {@link BasicReader} ready for use if one is configured.
	 * <p>
	 * Can return NULL if none are configured.
	 * 
	 * @return a {@link BasicReader}, or NULL if none configured
	 */
	public static Reader getReader() {
		try {
			if (defaultType != null) {
				return (Reader) SerialUtils.createObject(defaultType
						.getTypeName());
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
	 * Change the default {@link LocalLibrary} to open with the
	 * {@link BasicReader}s.
	 * 
	 * @param lib
	 *            the new {@link LocalLibrary}
	 */
	public static void setDefaultLibrary(BasicLibrary lib) {
		BasicReader.defaultLibrary = lib;
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
	public static void openExternal(BasicLibrary lib, String luid)
			throws IOException {
		MetaData meta = lib.getInfo(luid);
		File target = lib.getFile(luid);

		openExternal(meta, target);
	}

	/**
	 * Open the {@link Story} with an external reader (the program will be
	 * passed the given target file).
	 * 
	 * @param meta
	 *            the {@link Story} to load
	 * @param target
	 *            the target {@link File}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected static void openExternal(MetaData meta, File target)
			throws IOException {
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
