package be.nikiroo.utils.test_code;

import java.io.ByteArrayOutputStream;

import be.nikiroo.utils.streams.BufferedOutputStream;
import be.nikiroo.utils.test.TestCase;
import be.nikiroo.utils.test.TestLauncher;

class BufferedOutputStreamTest extends TestLauncher {
	public BufferedOutputStreamTest(String[] args) {
		super("BufferedOutputStream test", args);

		addTest(new TestCase("Single write") {
			@Override
			public void test() throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				BufferedOutputStream out = new BufferedOutputStream(bout);

				byte[] data = new byte[] { 42, 12, 0, 127 };

				out.write(data);
				out.close();

				checkArrays(this, "FIRST", bout, data);
			}
		});

		addTest(new TestCase("Multiple writes") {
			@Override
			public void test() throws Exception {
				ByteArrayOutputStream bout = new ByteArrayOutputStream();
				BufferedOutputStream out = new BufferedOutputStream(bout);

				byte[] data1 = new byte[] { 42, 12, 0, 127 };
				byte[] data2 = new byte[] { 15, 55 };
				byte[] data3 = new byte[] {};

				byte[] dataAll = new byte[] { 42, 12, 0, 127, 15, 55 };

				out.write(data1);
				out.write(data2);
				out.write(data3);
				out.close();

				checkArrays(this, "FIRST", bout, dataAll);
			}
		});
	}

	static void checkArrays(TestCase test, String prefix,
			ByteArrayOutputStream bout, byte[] expected) throws Exception {
		byte[] actual = bout.toByteArray();

		if (false) {
			System.out.print("\nExpected data: [ ");
			for (int i = 0; i < expected.length; i++) {
				if (i > 0)
					System.out.print(", ");
				System.out.print(expected[i]);
			}
			System.out.println(" ]");

			System.out.print("Actual data  : [ ");
			for (int i = 0; i < actual.length; i++) {
				if (i > 0)
					System.out.print(", ");
				System.out.print(actual[i]);
			}
			System.out.println(" ]");
		}

		test.assertEquals("The " + prefix
				+ " resulting array has not the correct number of items",
				expected.length, actual.length);
		for (int i = 0; i < actual.length; i++) {
			test.assertEquals(prefix + ": item " + i
					+ " (0-based) is not the same", expected[i], actual[i]);
		}
	}
}
