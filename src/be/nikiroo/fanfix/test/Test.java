package be.nikiroo.fanfix.test;

import java.io.File;
import java.io.IOException;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
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
	//
	// 3 files can control the test:
	// - test/VERBOSE: enable verbose mode
	// - test/OFFLINE: to forbid any downloading
	// - test/FORCE_REFRESH: to force a clear of the cache
	//
	// The test files will be:
	// - test/*.url: URL to download in text format, content = URL
	// - test/*.story: text mode story, content = story
	//

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
	 * 
	 * @throws IOException
	 */
	public Test(String[] args) throws IOException {
		super("Fanfix", args);
		Instance.setTraceHandler(null);
		addSeries(new BasicSupportUtilitiesTest(args));
		addSeries(new BasicSupportDeprecatedTest(args));
		addSeries(new LibraryTest(args));
		
		File sources = new File("test/");
		if (sources.isDirectory()) {
			for (File file : sources.listFiles()) {
				if (file.isDirectory()) {
					continue;
				}

				String expectedDir = new File(file.getParentFile(), "expected_"
						+ file.getName()).getAbsolutePath();
				String resultDir = new File(file.getParentFile(), "result_"
						+ file.getName()).getAbsolutePath();

				String uri;
				if (file.getName().endsWith(".url")) {
					uri = IOUtils.readSmallFile(file).trim();
				} else if (file.getName().endsWith(".story")) {
					uri = file.getAbsolutePath();
				} else {
					continue;
				}

				addSeries(new ConversionTest(file.getName(), uri, expectedDir,
						resultDir, args));
			}
		}
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

		// Verbose mode:
		boolean verbose = new File("test/VERBOSE").exists();

		// Can force refresh
		boolean forceRefresh = new File("test/FORCE_REFRESH").exists();

		// Only download files if allowed:
		boolean offline = new File("test/OFFLINE").exists();
		Instance.getCache().setOffline(offline);

		int result = 0;
		tempFiles = new TempFiles("fanfix-test");
		try {
			File tmpConfig = tempFiles.createTempDir("fanfix-config");
			File localCache = new File("test/CACHE");
			if (forceRefresh) {
				IOUtils.deltree(localCache);
			}
			localCache.mkdirs();

			ConfigBundle config = new ConfigBundle();
			Bundles.setDirectory(tmpConfig.getAbsolutePath());
			config.setString(Config.CACHE_DIR, localCache.getAbsolutePath());
			config.setInteger(Config.CACHE_MAX_TIME_STABLE, -1);
			config.setInteger(Config.CACHE_MAX_TIME_CHANGING, -1);
			config.updateFile(tmpConfig.getPath());
			System.setProperty("CONFIG_DIR", tmpConfig.getAbsolutePath());

			Instance.init(true);
			Instance.getCache().setOffline(offline);

			TestLauncher tests = new Test(args);
			tests.setDetails(verbose);

			result = tests.launch();

			IOUtils.deltree(tmpConfig);
		} finally {
			// Test temp files
			tempFiles.close();

			// This is usually done in Fanfix.Main:
			Instance.getTempFiles().close();
		}

		System.exit(result);
	}
}
