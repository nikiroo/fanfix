package be.nikiroo.utils.test;

import java.io.InputStream;

import be.nikiroo.utils.IOUtils;

public class IOUtilsTest extends TestLauncher {

	public IOUtilsTest(String[] args) {
		super("IOUtils test", args);

		addTest(new TestCase("openResource") {
			@Override
			public void test() throws Exception {
				InputStream in = IOUtils.openResource("VERSION");
				assertNotNull(
						"The VERSION file is supposed to be present in the binaries",
						in);
				in.close();
			}
		});

		addTest(new TestCase("getVersion") {
			@Override
			public void test() throws Exception {
				assertNotNull("The VERSION is not defined",
						IOUtils.getVersion());
			}
		});
	}
}
