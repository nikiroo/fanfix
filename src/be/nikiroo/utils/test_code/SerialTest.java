package be.nikiroo.utils.test_code;

import java.net.URL;

import be.nikiroo.utils.serial.Exporter;
import be.nikiroo.utils.serial.Importer;
import be.nikiroo.utils.test.TestCase;
import be.nikiroo.utils.test.TestLauncher;

class SerialTest extends TestLauncher {
	/**
	 * Required for Import/Export of objects.
	 */
	public SerialTest() {
		this(null);
	}

	public SerialTest(String[] args) {
		super("Serial test", args);

		addTest(new TestCase("Simple class Import/Export") {
			@Override
			public void test() throws Exception {
				Data data = new Data(42);
				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase() {
			@SuppressWarnings("unused")
			private TestCase me = setName("Anonymous inner class");

			@Override
			public void test() throws Exception {
				Data data = new Data() {
					@SuppressWarnings("unused")
					int value = 42;
				};

				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase() {
			@SuppressWarnings("unused")
			private TestCase me = setName("Array of anonymous inner classes");

			@Override
			public void test() throws Exception {
				Data[] data = new Data[] { new Data() {
					@SuppressWarnings("unused")
					int value = 42;
				} };

				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				// Comparing the 2 strings won't be useful, because the @REFs
				// will be ZIP-encoded; so we parse and re-encode the object
				encoded = new Exporter().append(data[0]).toString(false, false);
				try {
					reencoded = new Exporter().append(((Data[]) redata)[0])
							.toString(false, false);
				} catch (Exception e) {
					fail("Cannot cast the returned data into its original object",
							e);
				}

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("URL Import/Export") {
			@Override
			public void test() throws Exception {
				URL data = new URL("https://fanfan.be/");
				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("URL-String Import/Export") {
			@Override
			public void test() throws Exception {
				String data = new URL("https://fanfan.be/").toString();
				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
				assertEquals(data, redata);
			}
		});

		addTest(new TestCase("URL/URL-String arrays Import/Export") {
			@Override
			public void test() throws Exception {
				final String url = "https://fanfan.be/";

				Object[] data = new Object[] { new URL(url), url };
				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
				assertEquals(data[0], ((Object[]) redata)[0]);
				assertEquals(data[1], ((Object[]) redata)[1]);
			}
		});

		addTest(new TestCase("Import/Export with nested objects") {
			@Override
			public void test() throws Exception {
				Data data = new DataObject(new Data(21));
				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("Import/Export with nested objects forming a loop") {
			@Override
			public void test() throws Exception {
				DataLoop data = new DataLoop("looping");
				data.next = new DataLoop("level 2");
				data.next.next = data;

				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("Array in Object Import/Export") {
			@Override
			public void test() throws Exception {
				Object data = new DataArray();// new String[] { "un", "deux" };
				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("Array Import/Export") {
			@Override
			public void test() throws Exception {
				Object data = new String[] { "un", "deux" };
				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("Enum Import/Export") {
			@Override
			public void test() throws Exception {
				Object data = EnumToSend.FANFAN;
				String encoded = new Exporter().append(data).toString(false,
						false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata).toString(
						false, false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("B64 and ZIP String test") {
			@Override
			public void test() throws Exception {
				Object data = "Fanfan la tulipe";
				String encoded = new Exporter().append(data).toString(true,
						false);
				String redata = (String) new Importer().read(encoded)
						.getValue();

				assertEquals("Items not identical after B64", data, redata);

				encoded = new Exporter().append(data).toString(true, true);
				redata = (String) new Importer().read(encoded).getValue();

				assertEquals("Items not identical after ZIP", data, redata);
			}
		});

		addTest(new TestCase("B64 and ZIP Data test") {
			@Override
			public void test() throws Exception {
				Object data = new Data(55);
				String encoded = new Exporter().append(data).toString(true,
						false);
				Data redata = (Data) new Importer().read(encoded).getValue();

				assertEquals("Items not identical after B64", data, redata);

				encoded = new Exporter().append(data).toString(true, true);
				redata = (Data) new Importer().read(encoded).getValue();

				assertEquals("Items not identical after ZIP", data, redata);
			}
		});

		addTest(new TestCase("B64 and ZIP 70000 chars test") {
			@Override
			public void test() throws Exception {
				StringBuilder builder = new StringBuilder();
				for (int i = 0; i < 7000; i++) {
					builder.append("0123456789");
				}

				Object data = builder.toString();
				String encoded = new Exporter().append(data).toString(true,
						false);
				String redata = (String) new Importer().read(encoded)
						.getValue();

				assertEquals("Items not identical after B64", data, redata);

				encoded = new Exporter().append(data).toString(true, true);
				redata = (String) new Importer().read(encoded).getValue();

				assertEquals("Items not identical after ZIP", data, redata);
			}
		});
	}

	class DataArray {
		public String[] data = new String[] { "un", "deux" };
	}

	class Data {
		private int value;

		private Data() {
		}

		public Data(int value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Data) {
				Data other = (Data) obj;
				return other.value == this.value;
			}

			return false;
		}

		@Override
		public int hashCode() {
			return new Integer(value).hashCode();
		}
	}

	@SuppressWarnings("unused")
	class DataObject extends Data {
		private Data data;

		@SuppressWarnings("synthetic-access")
		private DataObject() {
		}

		@SuppressWarnings("synthetic-access")
		public DataObject(Data data) {
			this.data = data;
		}
	}

	@SuppressWarnings("unused")
	class DataLoop extends Data {
		public DataLoop next;
		private String value;

		@SuppressWarnings("synthetic-access")
		private DataLoop() {
		}

		@SuppressWarnings("synthetic-access")
		public DataLoop(String value) {
			this.value = value;
		}
	}

	enum EnumToSend {
		FANFAN, TULIPE,
	}
}
