package be.nikiroo.utils.test_code;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.ReplaceInputStream;
import be.nikiroo.utils.test.TestCase;
import be.nikiroo.utils.test.TestLauncher;

public class ReplaceInputStreamTest extends TestLauncher {
	public ReplaceInputStreamTest(String[] args) {
		super("ReplaceInputStream test", args);

		addTest(new TestCase("Simple InputStream, empty replace") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(expected), new byte[0],
						new byte[0]);
				checkArrays(this, "FIRST", in, expected);
			}
		});

		addTest(new TestCase("Simple InputStream, simple replace") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(expected), new byte[] { 0 },
						new byte[] { 10 });

				checkArrays(this, "FIRST", in, new byte[] { 42, 12, 10, 127 });
			}
		});

		addTest(new TestCase("Simple byte array reading, 3/4 replace") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(expected), new byte[] { 12, 0,
								127 }, new byte[] { 10, 10, 10 });

				checkArrays(this, "FIRST", in, new byte[] { 42, 10, 10, 10 });
			}
		});

		addTest(new TestCase("Simple byte array reading, longer replace") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(expected), new byte[] { 0 },
						new byte[] { 10, 10, 10 });

				// TODO NOT OK!!
				System.out.println();
				for (int i = 0; i < expected.length; i++) {
					System.out.println("expected[" + i + "] = " + expected[i]);
				}
				byte[] actual = IOUtils.readSmallStream(in).getBytes("UTF-8");
				for (int i = 0; i < actual.length; i++) {
					System.out.println("actual[" + i + "] = " + actual[i]);
				}
				System.exit(1);
				//

				checkArrays(this, "FIRST", in, new byte[] { 42, 12, 10, 10, 10,
						127 });
			}
		});

		addTest(new TestCase("Simple byte array reading, shorter replace") {
			@Override
			public void test() throws Exception {
				byte[] expected = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(expected), new byte[] { 42,
								12, 0 }, new byte[] { 10 });
				checkArrays(this, "FIRST", in, new byte[] { 10, 127 });
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
			test.assertEquals("Item " + i + " (0-based) is not the same",
					expected[i], actual[i]);
		}
	}
}
