package be.nikiroo.utils.test_code;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.NextableInputStream;
import be.nikiroo.utils.NextableInputStreamStep;
import be.nikiroo.utils.test.TestCase;
import be.nikiroo.utils.test.TestLauncher;

public class NextableInputStreamTest extends TestLauncher {
	public NextableInputStreamTest(String[] args) {
		super("NextableInputStream test", args);

		addTest(new TestCase("Simple byte array reading") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127 };
				InputStream bin = new ByteArrayInputStream(expected);
				NextableInputStream in = new NextableInputStream(bin);
				byte[] actual = IOUtils.toByteArray(in);

				assertEquals(
						"The resulting array has not the same number of items",
						expected.length, actual.length);
				for (int i = 0; i < expected.length; i++) {
					assertEquals("Item " + i + " (0-based) is not the same",
							expected[i], actual[i]);
				}
			}
		});

		addTest(new TestCase("Stop at 12") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127 };
				InputStream bin = new ByteArrayInputStream(expected);
				NextableInputStream in = new NextableInputStream(bin);
				in.addStep(new NextableInputStreamStep(12));
				byte[] actual = IOUtils.toByteArray(in);

				assertEquals(
						"The resulting array has not the correct number of items",
						1, actual.length);
				for (int i = 0; i < actual.length; i++) {
					assertEquals("Item " + i + " (0-based) is not the same",
							expected[i], actual[i]);
				}
			}
		});

		addTest(new TestCase("Stop at 12, resume, stop again, resume") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127, 12, 51, 11, 12 };
				NextableInputStream in = new NextableInputStream(
						new ByteArrayInputStream(expected));
				in.addStep(new NextableInputStreamStep(12));

				byte[] actual1 = IOUtils.toByteArray(in);
				byte[] expected1 = new byte[] { 42 };
				assertEquals(
						"The FIRST resulting array has not the correct number of items",
						expected1.length, actual1.length);
				for (int i = 0; i < actual1.length; i++) {
					assertEquals("Item " + i + " (0-based) is not the same",
							expected1[i], actual1[i]);
				}

				assertEquals("Cannot get SECOND entry", true, in.next());
				byte[] actual2 = IOUtils.toByteArray(in);
				byte[] expected2 = new byte[] { 0, 127 };
				assertEquals(
						"The SECOND resulting array has not the correct number of items",
						expected2.length, actual2.length);
				for (int i = 0; i < actual2.length; i++) {
					assertEquals("Item " + i + " (0-based) is not the same",
							expected2[i], actual2[i]);
				}

				assertEquals("Cannot get next THIRD entry", true, in.next());
				byte[] actual3 = IOUtils.toByteArray(in);
				byte[] expected3 = new byte[] { 51, 11 };
				assertEquals(
						"The THIRD resulting array has not the correct number of items",
						expected3.length, actual3.length);
				for (int i = 0; i < actual3.length; i++) {
					assertEquals("Item " + i + " (0-based) is not the same",
							expected3[i], actual3[i]);
				}
			}
		});
	}
}
