package be.nikiroo.fanfix.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import be.nikiroo.fanfix.bundles.ConfigBundle;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.resources.Bundles;
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
		addSeries(new LibraryTest(args));
	}

	/**
	 * Main entry point of the program.
	 * 
	 * @param args
	 *            the arguments passed to the {@link TestLauncher}s.
	 * @throws IOException
	 */
	static public void main(String[] args) throws IOException {
		File tmpConfig = File.createTempFile("fanfix-config_", ".test");
		File tmpCache = File.createTempFile("fanfix-cache_", ".test");
		tmpConfig.delete();
		tmpConfig.mkdir();
		tmpCache.delete();
		tmpCache.mkdir();

		FileOutputStream out = new FileOutputStream(new File(tmpConfig,
				"config.properties"));
		Properties props = new Properties();
		props.setProperty("CACHE_DIR", tmpCache.getAbsolutePath());
		props.store(out, null);
		out.close();

		ConfigBundle config = new ConfigBundle();
		Bundles.setDirectory(tmpConfig.getAbsolutePath());
		config.updateFile(tmpConfig.getPath());

		System.setProperty("CONFIG_DIR", tmpConfig.getAbsolutePath());

		int result = new Test(args).launch();

		IOUtils.deltree(tmpConfig);
		IOUtils.deltree(tmpCache);

		System.exit(result);
	}
}
