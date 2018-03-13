package be.nikiroo.utils.test;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.StringUtils.Alignment;

class StringUtilsTest extends TestLauncher {
	public StringUtilsTest(String[] args) {
		super("StringUtils test", args);

		addTest(new TestCase("Time serialisation") {
			@Override
			public void test() throws Exception {
				for (long fullTime : new Long[] { 0l, 123456l, 123456000l,
						new Date().getTime() }) {
					// precise to the second, no more
					long time = (fullTime / 1000) * 1000;

					String displayTime = StringUtils.fromTime(time);
					assertNotNull("The stringified time for " + time
							+ " should not be null", displayTime);
					assertEquals("The stringified time for " + time
							+ " should not be empty", false, displayTime.trim()
							.isEmpty());

					assertEquals("The time " + time
							+ " should be loop-convertable", time,
							StringUtils.toTime(displayTime));

					assertEquals("The time " + displayTime
							+ " should be loop-convertable", displayTime,
							StringUtils.fromTime(StringUtils
									.toTime(displayTime)));
				}
			}
		});

		addTest(new TestCase("MD5") {
			@Override
			public void test() throws Exception {
				String mess = "The String we got is not what 'md5sum' said it should heve been";
				assertEquals(mess, "34ded48fcff4221d644be9a37e1cb1d9",
						StringUtils.getMd5Hash("fanfan la tulipe"));
				assertEquals(mess, "7691b0cb74ed0f94b4d8cd858abe1165",
						StringUtils.getMd5Hash("je te do-o-o-o-o-o-nne"));
			}
		});

		addTest(new TestCase("Padding") {
			@Override
			public void test() throws Exception {
				for (String data : new String[] { "fanfan", "la tulipe",
						"1234567890", "12345678901234567890", "1", "" }) {
					String result = StringUtils.padString(data, -1);
					assertEquals("A size of -1 is expected to produce a noop",
							true, data.equals(result));
					for (int size : new Integer[] { 0, 1, 5, 10, 40 }) {
						result = StringUtils.padString(data, size);
						assertEquals(
								"Padding a String at a certain size should give a String of the given size",
								size, result.length());
						assertEquals(
								"Padding a String should not change the content",
								true, data.trim().startsWith(result.trim()));

						result = StringUtils.padString(data, size, false, null);
						assertEquals(
								"Padding a String without cutting should not shorten the String",
								true, data.length() <= result.length());
						assertEquals(
								"Padding a String without cutting should keep the whole content",
								true, data.trim().equals(result.trim()));

						result = StringUtils.padString(data, size, false,
								Alignment.RIGHT);
						if (size > data.length()) {
							assertEquals(
									"Padding a String to the end should work as expected",
									true, result.endsWith(data));
						}

						result = StringUtils.padString(data, size, false,
								Alignment.JUSTIFY);
						if (size > data.length()) {
							String unspacedData = data.trim();
							String unspacedResult = result.trim();
							for (int i = 0; i < size; i++) {
								unspacedData = unspacedData.replace("  ", " ");
								unspacedResult = unspacedResult.replace("  ",
										" ");
							}

							assertEquals(
									"Justified text trimmed with all spaces collapsed "
											+ "sould be identical to original text "
											+ "trimmed with all spaces collapsed",
									unspacedData, unspacedResult);
						}

						result = StringUtils.padString(data, size, false,
								Alignment.CENTER);
						if (size > data.length()) {
							int before = 0;
							for (int i = 0; i < result.length()
									&& result.charAt(i) == ' '; i++) {
								before++;
							}

							int after = 0;
							for (int i = result.length() - 1; i >= 0
									&& result.charAt(i) == ' '; i--) {
								after++;
							}

							if (result.trim().isEmpty()) {
								after = before / 2;
								if (before > (2 * after)) {
									before = after + 1;
								} else {
									before = after;
								}
							}

							assertEquals(
									"Padding a String on center should work as expected",
									result.length(), before + data.length()
											+ after);
							assertEquals(
									"Padding a String on center should not uncenter the content",
									true, Math.abs(before - after) <= 1);
						}
					}
				}
			}
		});

		addTest(new TestCase("Justifying") {
			@Override
			public void test() throws Exception {
				for (String data : new String[] { "test",
						"let's test some words", "" }) {
					int total = 0;
					for (String word : data.split((" "))) {
						total += word.replace("-", "").replace(" ", "")
								.length();
					}
					List<String> result = StringUtils.justifyText(data, 5,
							StringUtils.Alignment.LEFT);
					
					System.out.println("["+data+"] -> [");

					int totalResult = 0;
					for (String resultLine : result) {
						System.out.println(resultLine);
						for (String word : resultLine.split((" "))) {
							totalResult += word.replace("-", "")
									.replace(" ", "").length();
						}
					}
					System.out.println("]");

					assertEquals(
							"The number of letters ('-' not included) should be identical before and after",
							total, totalResult);
				}
			}
		});

		addTest(new TestCase("unhtml") {
			@Override
			public void test() throws Exception {
				Map<String, String> data = new HashMap<String, String>();
				data.put("aa", "aa");
				data.put("test with spaces ", "test with spaces ");
				data.put("<a href='truc://target/'>link</a>", "link");
				data.put("<html>Digimon</html>", "Digimon");
				data.put("", "");
				data.put(" ", " ");

				for (Entry<String, String> entry : data.entrySet()) {
					String result = StringUtils.unhtml(entry.getKey());
					assertEquals("Result is not what we expected",
							entry.getValue(), result);
				}
			}
		});

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
