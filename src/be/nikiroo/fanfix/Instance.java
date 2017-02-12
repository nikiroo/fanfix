package be.nikiroo.fanfix;

import java.io.File;
import java.io.IOException;

import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.ConfigBundle;
import be.nikiroo.fanfix.bundles.StringIdBundle;
import be.nikiroo.utils.resources.Bundles;

/**
 * Global state for the program (services and singletons).
 * 
 * @author niki
 */
public class Instance {
	private static ConfigBundle config;
	private static StringIdBundle trans;
	private static Cache cache;
	private static Library lib;
	private static boolean debug;
	private static File coverDir;
	private static File readerTmp;

	static {
		// Most of the rest is dependant upon this:
		config = new ConfigBundle();

		trans = new StringIdBundle(getLang());
		lib = new Library(getFile(Config.LIBRARY_DIR));
		debug = Instance.getConfig().getBoolean(Config.DEBUG_ERR, false);
		coverDir = getFile(Config.DEFAULT_COVERS_DIR);
		File tmp = getFile(Config.CACHE_DIR);
		readerTmp = getFile(Config.CACHE_DIR_LOCAL_READER);

		if (checkEnv("NOUTF")) {
			trans.setUnicode(false);
		}

		if (checkEnv("DEBUG")) {
			debug = true;
		}

		if (tmp == null || readerTmp == null) {
			String tmpDir = System.getProperty("java.io.tmpdir");
			if (tmpDir != null) {
				if (tmp == null) {
					tmp = new File(tmpDir, "fanfic-tmp");
				}
				if (readerTmp == null) {
					readerTmp = new File(tmpDir, "fanfic-reader");
				}
			} else {
				syserr(new IOException(
						"The system does not have a default temporary directory"));
			}
		}

		if (coverDir != null && !coverDir.exists()) {
			syserr(new IOException(
					"The 'default covers' directory does not exists: "
							+ coverDir));
			coverDir = null;
		}

		String configDir = System.getenv("CONFIG_DIR");
		if (configDir != null) {
			if (new File(configDir).isDirectory()) {
				Bundles.setDirectory(configDir);
				try {
					config = new ConfigBundle();
					config.updateFile(configDir);
				} catch (IOException e) {
					syserr(e);
				}
				try {
					trans = new StringIdBundle(getLang());
					trans.updateFile(configDir);
				} catch (IOException e) {
					syserr(e);
				}
			} else {
				syserr(new IOException("Configuration directory not found: "
						+ configDir));
			}
		}

		try {
			String ua = config.getString(Config.USER_AGENT);
			int hours = config.getInteger(Config.CACHE_MAX_TIME_CHANGING, -1);
			int hoursLarge = config
					.getInteger(Config.CACHE_MAX_TIME_STABLE, -1);

			cache = new Cache(tmp, ua, hours, hoursLarge);
		} catch (IOException e) {
			syserr(new IOException(
					"Cannot create cache (will continue without cache)", e));
		}
	}

	/**
	 * Get the (unique) configuration service for the program.
	 * 
	 * @return the configuration service
	 */
	public static ConfigBundle getConfig() {
		return config;
	}

	/**
	 * Get the (unique) {@link Cache} for the program.
	 * 
	 * @return the {@link Cache}
	 */
	public static Cache getCache() {
		return cache;
	}

	/**
	 * Get the (unique) {link StringIdBundle} for the program.
	 *
	 * @return the {link StringIdBundle}
	 */
	public static StringIdBundle getTrans() {
		return trans;
	}

	/**
	 * Get the (unique) {@link Library} for the program.
	 * 
	 * @return the {@link Library}
	 */
	public static Library getLibrary() {
		return lib;
	}

	/**
	 * Return the directory where to look for default cover pages.
	 * 
	 * @return the default covers directory
	 */
	public static File getCoverDir() {
		return coverDir;
	}

	/**
	 * Return the directory where to store temporary files for the local reader.
	 * 
	 * @return the directory
	 */
	public static File getReaderDir() {
		return readerTmp;
	}

	/**
	 * Report an error to the user
	 * 
	 * @param e
	 *            the {@link Exception} to report
	 */
	public static void syserr(Exception e) {
		if (debug) {
			e.printStackTrace();
		} else {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Return a path, but support the special $HOME variable.
	 * 
	 * @return the path
	 */
	private static File getFile(Config id) {
		File file = null;
		String path = config.getString(id);
		if (path != null && !path.isEmpty()) {
			path = path.replace('/', File.separatorChar);
			if (path.contains("$HOME")) {
				path = path.replace("$HOME",
						"" + System.getProperty("user.home"));
			}

			file = new File(path);
		}

		return file;
	}

	/**
	 * The language to use for the application (NULL = default system language).
	 * 
	 * @return the language
	 */
	private static String getLang() {
		String lang = config.getString(Config.LANG);

		if (System.getenv("LANG") != null && !System.getenv("LANG").isEmpty()) {
			lang = System.getenv("LANG");
		}

		if (lang != null && lang.isEmpty()) {
			lang = null;
		}

		return lang;
	}

	/**
	 * Check that the given environment variable is "enabled".
	 * 
	 * @param key
	 *            the variable to check
	 * 
	 * @return TRUE if it is
	 */
	private static boolean checkEnv(String key) {
		String value = System.getenv(key);
		if (value != null) {
			value = value.trim().toLowerCase();
			if ("yes".equals(value) || "true".equals(value)
					|| "on".equals(value) || "1".equals(value)
					|| "y".equals(value)) {
				return true;
			}
		}

		return false;
	}
}
