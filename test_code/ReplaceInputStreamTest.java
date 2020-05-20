package be.nikiroo.utils.test_code;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.streams.ReplaceInputStream;
import be.nikiroo.utils.test.TestCase;
import be.nikiroo.utils.test.TestLauncher;

class ReplaceInputStreamTest extends TestLauncher {
	public ReplaceInputStreamTest(String[] args) {
		super("ReplaceInputStream test", args);

		addTest(new TestCase("Empty replace") {
			@Override
			public void test() throws Exception {
				byte[] data = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(data), new byte[0],
						new byte[0]);

				checkArrays(this, "FIRST", in, data);
			}
		});

		addTest(new TestCase("Simple replace") {
			@Override
			public void test() throws Exception {
				byte[] data = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(data), new byte[] { 0 },
						new byte[] { 10 });

				checkArrays(this, "FIRST", in, new byte[] { 42, 12, 10, 127 });
			}
		});

		addTest(new TestCase("3/4 replace") {
			@Override
			public void test() throws Exception {
				byte[] data = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(data),
						new byte[] { 12, 0, 127 }, new byte[] { 10, 10, 10 });

				checkArrays(this, "FIRST", in, new byte[] { 42, 10, 10, 10 });
			}
		});

		addTest(new TestCase("Longer replace") {
			@Override
			public void test() throws Exception {
				byte[] data = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(data), new byte[] { 0 },
						new byte[] { 10, 10, 10 });

				checkArrays(this, "FIRST", in, new byte[] { 42, 12, 10, 10, 10,
						127 });
			}
		});

		addTest(new TestCase("Shorter replace") {
			@Override
			public void test() throws Exception {
				byte[] data = new byte[] { 42, 12, 0, 127 };
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(data),
						new byte[] { 42, 12, 0 }, new byte[] { 10 });

				checkArrays(this, "FIRST", in, new byte[] { 10, 127 });
			}
		});

		addTest(new TestCase("String replace") {
			@Override
			public void test() throws Exception {
				byte[] data = "I like red".getBytes("UTF-8");
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(data),
						"red", "blue");

				checkArrays(this, "FIRST", in, "I like blue".getBytes("UTF-8"));

				data = "I like blue hammers".getBytes("UTF-8");
				in = new ReplaceInputStream(new ByteArrayInputStream(data),
						"blue", "red");

				checkArrays(this, "SECOND", in, "I like red hammers".getBytes("UTF-8"));
			}
		});
		
		addTest(new TestCase("Multiple replaces") {
			@Override
			public void test() throws Exception {
				byte[] data = "I like red cabage".getBytes("UTF-8");
				ReplaceInputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(data), //
						new String[] { "red", "like" }, //
						new String[] { "green", "very very much like" } //
				);
				
				String result = new String(IOUtils.toByteArray(in), "UTF-8");
				assertEquals("I very very much like green cabage", result);
			}
		});
		
		addTest(new TestCase("Multiple replaces") {
			@Override
			public void test() throws Exception {
				String str= ("" //
						+ "<!DOCTYPE html>\n" //
						+ "<html>\n" //
						+ "<head>\n" //
						+ "<!--\n" //
						+ "\tCopyright 2020 David ROULET\n" //
						+ "\t\n" //
						+ "\tThis file is part of fanfix.\n" //
						+ "\t\n" //
						+ "\tfanfix is free software: you can redistribute it and/or modify\n" //
						+ "\tit under the terms of the GNU Affero General Public License as published by\n" //
						+ "\tthe Free Software Foundation, either version 3 of the License, or\n" //
						+ "\t(at your option) any later version.\n" //
						+ "\t\n" //
						+ "\tfanfix is distributed in the hope that it will be useful,\n" //
						+ "\tbut WITHOUT ANY WARRANTY; without even the implied warranty of\n" //
						+ "\tMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n" //
						+ "\tGNU Affero General Public License for more details.\n" //
						+ "\t\n" //
						+ "\tYou should have received a copy of the GNU Affero General Public License\n" //
						+ "\talong with fanfix.  If not, see <https://www.gnu.org/licenses/>.\n" //
						+ "\t___________________________________________________________________________\n" //
						+ "\n" //
						+ "       This website was coded by:\n" //
						+ "       \t\tA kangaroo.\n" //
						+ "                                                  _  _\n" //
						+ "                                                 (\\\\( \\\n" //
						+ "                                                  `.\\-.)\n" //
						+ "                              _...._            _,-'   `-.\n" //
						+ "\\                           ,'      `-._.- -.,-'       .  \\\n" //
						+ " \\`.                      ,'                               `.\n" //
						+ "  \\ `-...__              /                           .   .:  y\n" //
						+ "   `._     ``-...__     /                           ,'```-._/\n" //
						+ "      `-._         ```-'                      |    /_          //\n" //
						+ "          `.._                   _            ;   <_ \\        //\n" //
						+ "              ``-.___             `.           `-._ \\ \\      //\n" //
						+ "                     `- <           `.     (\\ _/)/ `.\\/     //\n" //
						+ "                         \\            \\     `       ^^^^^^^^^\n" //
						+ "\t___________________________________________________________________________\n" //
						+ "\t\n" //
						+ "-->\n" //
						+ "\t<meta http-equiv='content-type' content='text/html; charset=UTF-8'>\n" //
						+ "\t<meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" //
						+ "\t<title>${title}</title>\n" //
						+ "\t<link rel='stylesheet' type='text/css' href='/style.css' />\n" //
						+ "\t<link rel='icon' type='image/x-icon' href='/${favicon}' />\n" //
						+ "</head>\n" //
						+ "<body>\n" //
						+ "\t<div class='main'>\n" //
						+ "${banner}${content}\t</div>\n" //
						+ "</body>\n" //
						+ "" //
				);
				byte[] data = str.getBytes("UTF-8");

				String title = "Fanfix";
				String banner = "<div class='banner'>Super banner v3</div>";
				String content = "";

				InputStream in = new ReplaceInputStream(
						new ByteArrayInputStream(data), //
						new String[] { "${title}", "${banner}", "${content}" }, //
						new String[] { title, banner, content } //
				);

				String result = new String(IOUtils.toByteArray(in), "UTF-8");
				assertEquals(str //
						.replace("${title}", title) //
						.replace("${banner}", banner) //
						.replace("${content}", content) //
				, result);
			}
		});
		
		
	}

	static void checkArrays(TestCase test, String prefix, InputStream in,
			byte[] expected) throws Exception {
		byte[] actual = IOUtils.toByteArray(in);
		
//		System.out.println("\nActual:");
//		for(byte byt : actual) {
//			System.out.print(byt+" ");
//		}
//		System.out.println("\nExpected:");
//		for(byte byt : expected) {
//			System.out.print(byt+" ");
//		}
		
		test.assertEquals("The " + prefix
				+ " resulting array has not the correct number of items",
				expected.length, actual.length);
		for (int i = 0; i < actual.length; i++) {
			test.assertEquals("Item " + i + " (0-based) is not the same",
					expected[i], actual[i]);
		}
	}
}
