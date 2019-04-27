package be.nikiroo.utils.test_code;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.streams.BufferedInputStream;
import be.nikiroo.utils.test.TestCase;
import be.nikiroo.utils.test.TestLauncher;

class BufferedInputStreamTest extends TestLauncher {
	public BufferedInputStreamTest(String[] args) {
		super("BufferedInputStream test", args);

		addTest(new TestCase("Simple InputStream reading") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127 };
				BufferedInputStream in = new BufferedInputStream(
						new ByteArrayInputStream(expected));
				checkArrays(this, "FIRST", in, expected);
			}
		});

		addTest(new TestCase("Simple byte array reading") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127 };
				BufferedInputStream in = new BufferedInputStream(expected);
				checkArrays(this, "FIRST", in, expected);
			}
		});
	}

	static void checkArrays(TestCase test, String prefix, InputStream in,
			byte[] expected) throws Exception {
		byte[] actual = IOUtils.toByteArray(in);
		test.assertEquals("The " + prefix
				+ " resulting array has not the correct number of items",
				expected.length, actual.length);
		for (int i = 0; i < actual.length; i++) {
			test.assertEquals(prefix + ": item " + i
					+ " (0-based) is not the same", expected[i], actual[i]);
		}
	}
}
