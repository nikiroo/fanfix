package be.nikiroo.utils.test_code;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.NextableInputStream;
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
	}
}
