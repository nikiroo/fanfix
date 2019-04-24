package be.nikiroo.utils.test_code;

import java.io.ByteArrayInputStream;

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
				NextableInputStream in = new NextableInputStream(
						new ByteArrayInputStream(expected), null);
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
				NextableInputStream in = new NextableInputStream(
						new ByteArrayInputStream(expected),
						new NextableInputStreamStep(12));

				checkNext(this, false, "FIRST", in, new byte[] { 42 });
			}
		});

		addTest(new TestCase("Stop at 12, resume, stop again, resume") {
			@Override
			public void test() throws Exception {
				byte[] data = new byte[] { 42, 12, 0, 127, 12, 51, 11, 12 };
				NextableInputStream in = new NextableInputStream(
						new ByteArrayInputStream(data),
						new NextableInputStreamStep(12));

				checkNext(this, false, "FIRST", in, new byte[] { 42 });
				checkNext(this, true, "SECOND", in, new byte[] { 0, 127 });
				checkNext(this, true, "THIRD", in, new byte[] { 51, 11 });
			}
		});

		addTest(new TestCase("Encapsulation") {
			@Override
			public void test() throws Exception {
				byte[] data = new byte[] { 42, 12, 0, 4, 127, 12, 5 };
				NextableInputStream in4 = new NextableInputStream(
						new ByteArrayInputStream(data),
						new NextableInputStreamStep(4));
				NextableInputStream subIn12 = new NextableInputStream(in4,
						new NextableInputStreamStep(12));

				checkNext(this, false, "SUB FIRST", subIn12, new byte[] { 42 });
				checkNext(this, true, "SUB SECOND", subIn12, new byte[] { 0 });

				assertEquals("The subIn still has some data", false,
						subIn12.next());

				checkNext(this, true, "MAIN LAST", in4,
						new byte[] { 127, 12, 5 });
			}
		});

		addTest(new TestCase("UTF-8 text lines test") {
			@Override
			public void test() throws Exception {
				String ln1 = "Ligne première";
				String ln2 = "Ligne la deuxième du nom";
				byte[] data = (ln1 + "\n" + ln2).getBytes("UTF-8");
				NextableInputStream in = new NextableInputStream(
						new ByteArrayInputStream(data),
						new NextableInputStreamStep('\n'));

				checkNext(this, false, "FIRST", in, ln1.getBytes("UTF-8"));
				checkNext(this, true, "SECOND", in, ln2.getBytes("UTF-8"));
			}
		});
	}

	static void checkNext(TestCase test, boolean callNext, String prefix,
			NextableInputStream in, byte[] expected) throws Exception {
		if (callNext) {
			test.assertEquals("Cannot get " + prefix + " entry", true,
					in.next());
		}
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
