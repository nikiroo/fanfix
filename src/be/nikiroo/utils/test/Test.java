package be.nikiroo.utils.test;

import be.nikiroo.utils.Cache;
import be.nikiroo.utils.Downloader;
import be.nikiroo.utils.main.StartImageUtils;
import be.nikiroo.utils.main.StartStringUtils;

/**
 * Tests for nikiroo-utils.
 * 
 * @author niki
 */
public class Test extends TestLauncher {
	/**
	 * Start the tests.
	 * 
	 * @param args
	 *            the arguments (which are passed as-is to the other test
	 *            classes)
	 */
	public Test(String[] args) {
		super("Nikiroo-utils", args);

		addSeries(new ProgressTest(args));
		addSeries(new BundleTest(args));
		addSeries(new IOUtilsTest(args));
		addSeries(new VersionTest(args));
		addSeries(new SerialTest(args));
		addSeries(new SerialServerTest(args));
		addSeries(new StringUtilsTest(args));
		addSeries(new TempFilesTest(args));

		// TODO: test cache and downloader
		Cache cache = null;
		Downloader downloader = null;

		// To include the sources:
		StartImageUtils siu;
		StartStringUtils ssu;
	}

	/**
	 * Main entry point of the program.
	 * 
	 * @param args
	 *            the arguments passed to the {@link TestLauncher}s.
	 */
	static public void main(String[] args) {
		System.exit(new Test(args).launch());
	}
}
