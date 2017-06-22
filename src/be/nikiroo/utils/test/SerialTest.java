package be.nikiroo.utils.test;

import be.nikiroo.utils.serial.Exporter;
import be.nikiroo.utils.serial.Importer;

class SerialTest extends TestLauncher {
	private SerialTest() {
		super("Serial test", null);
	}

	public SerialTest(String[] args) {
		super("Serial test", args);

		addTest(new TestCase("Simple class Import/Export") {
			@Override
			public void test() throws Exception {
				Data data = new Data(42);
				String encoded = new Exporter().append(data).toString(false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata)
						.toString(false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("Import/Export with nested objects") {
			@Override
			public void test() throws Exception {
				Data data = new DataObject(new Data(21));
				String encoded = new Exporter().append(data).toString(false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata)
						.toString(false);

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

				String encoded = new Exporter().append(data).toString(false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata)
						.toString(false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});
	}

	@SuppressWarnings("unused")
	class Data {
		private int value;

		private Data() {
		}

		public Data(int value) {
			this.value = value;
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
}
