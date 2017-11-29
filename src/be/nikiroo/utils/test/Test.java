package be.nikiroo.utils.test;

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
