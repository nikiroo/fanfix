package be.nikiroo.fanfix.test;

import be.nikiroo.utils.test.TestLauncher;

/**
 * Tests for Fanfix.
 * 
 * @author niki
 */
public class Test extends TestLauncher {
	public Test(String[] args) {
		super("Fanfix", args);
		addSeries(new BasicSupportTest(args));
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
