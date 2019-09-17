package be.nikiroo.fanfix.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.ConfigBundle;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.TempFiles;
import be.nikiroo.utils.resources.Bundles;
import be.nikiroo.utils.test.TestLauncher;

/**
 * Tests for Fanfix.
 * 
 * @author niki
 */
public class Test extends TestLauncher {
	/**
	 * The temporary files handler.
	 */
	static TempFiles tempFiles;

	/**
	 * Create the Fanfix {@link TestLauncher}.
	 * 
	 * @param args
	 *            the arguments to configure the number of columns and the ok/ko
	 *            {@link String}s
	 */
	public Test(String[] args) {
		super("Fanfix", args);
		Instance.setTraceHandler(null);
		addSeries(new BasicSupportUtilitiesTest(args));
		addSeries(new BasicSupportDeprecatedTest(args));
		addSeries(new LibraryTest(args));
		addSeries(new ConversionTest(args));
	}

	/**
	 * Main entry point of the program.
	 * 
	 * @param args
	 *            the arguments passed to the {@link TestLauncher}s.
	 * @throws IOException
	 *             in case of I/O error
	 */
	static public void main(String[] args) throws IOException {
		Instance.init();
		//Instance.getCache().setOffline(true);
		
		int result = 0;
		tempFiles = new TempFiles("fanfix-test");
		try {
			File tmpConfig = tempFiles.createTempDir("fanfix-config");
			File tmpCache = tempFiles.createTempDir("fanfix-cache");

			FileOutputStream out = null;
			try {
				out = new FileOutputStream(new File(tmpConfig,
						"config.properties"));
				Properties props = new Properties();
				props.setProperty("CACHE_DIR", tmpCache.getAbsolutePath());
				props.store(out, null);
			} finally {
				if (out != null) {
					out.close();
				}
			}

			ConfigBundle config = new ConfigBundle();
			Bundles.setDirectory(tmpConfig.getAbsolutePath());
			config.updateFile(tmpConfig.getPath());

			System.setProperty("CONFIG_DIR", tmpConfig.getAbsolutePath());

			result = new Test(args).launch();

			IOUtils.deltree(tmpConfig);
			IOUtils.deltree(tmpCache);
		} finally {
			// Test temp files
			tempFiles.close();

			// This is usually done in Fanfix.Main:
			Instance.getTempFiles().close();
		}

		System.exit(result);
	}
}
