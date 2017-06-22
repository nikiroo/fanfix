package be.nikiroo.utils.test;

import be.nikiroo.utils.StringUtils;

class StringUtilsTest extends TestLauncher {
	public StringUtilsTest(String[] args) {
		super("StringUtils test", args);

		addTest(new TestCase("zip64") {
			@Override
			public void test() throws Exception {
				String orig = "test";
				String zipped = StringUtils.zip64(orig);
				String unzipped = StringUtils.unzip64(zipped);
				assertEquals(orig, unzipped);
			}
		});
	}
}
