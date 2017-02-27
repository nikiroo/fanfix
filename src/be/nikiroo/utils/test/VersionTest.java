package be.nikiroo.utils.test;

import be.nikiroo.utils.Version;

class VersionTest extends TestLauncher {
	public VersionTest(String[] args) {
		super("Version test", args);

		addTest(new TestCase("String <-> int") {
			@Override
			public void test() throws Exception {
				assertEquals("Cannot parse version 1.2.3 from int to String",
						"1.2.3", new Version(1, 2, 3).toString());
				assertEquals(
						"Cannot parse major version \"1.2.3\" from String to int",
						1, new Version("1.2.3").getMajor());
				assertEquals(
						"Cannot parse minor version \"1.2.3\" from String to int",
						2, new Version("1.2.3").getMinor());
				assertEquals(
						"Cannot parse patch version \"1.2.3\" from String to int",
						3, new Version("1.2.3").getPatch());
			}
		});

		addTest(new TestCase("Bad input") {
			@Override
			public void test() throws Exception {
				assertEquals(
						"Bad input should return an empty version",
						true,
						new Version(
								"Doors 98 SE Special Deluxe Edition Pro++ Not-Home")
								.isEmpty());

				assertEquals(
						"Bad input should return [unknown]",
						"[unknown]",
						new Version(
								"Doors 98 SE Special Deluxe Edition Pro++ Not-Home")
								.toString());
			}
		});

		addTest(new TestCase("Read current version") {
			@Override
			public void test() throws Exception {
				assertNotNull("The version should not be NULL (in any case!)",
						Version.getCurrentVersion());
				assertEquals("The current version should not be empty", false,
						Version.getCurrentVersion().isEmpty());
			}
		});
	}
}
