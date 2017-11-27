package be.nikiroo.utils.test;

import be.nikiroo.utils.TraceHandler;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.ConnectActionClient;
import be.nikiroo.utils.serial.ConnectActionServer;
import be.nikiroo.utils.serial.Exporter;
import be.nikiroo.utils.serial.Importer;
import be.nikiroo.utils.serial.Server;

class SerialTest extends TestLauncher {
	private SerialTest() {
		super("Serial test", null);
	}

	private TestLauncher createServerTestCases(final String[] args,
			final boolean ssl) {
		final String ssls = (ssl ? "(ssl)" : "(plain text)");
		TestLauncher series = new TestLauncher("Client/Server " + ssls, args);

		series.addTest(new TestCase("Simple connection " + ssls) {
			@Override
			public void test() throws Exception {
				final Object[] rec = new Object[1];

				Server server = new Server(this.getName(), 0, ssl) {
					@Override
					protected Object onRequest(ConnectActionServer action,
							Version clientVersion, Object data)
							throws Exception {
						return null;
					}
				};

				assertEquals("A port should have been assigned", true,
						server.getPort() > 0);

				// TODO: remove
				server.setTraceHandler(new TraceHandler(true, true, true));

				server.start(false);

				try {
					new ConnectActionClient(null, server.getPort(), ssl) {
						@Override
						public void action(Version serverVersion)
								throws Exception {
							rec[0] = true;
						}
					}.connect();
				} finally {
					server.stop();
				}

				assertNotNull("The client action was not run", rec[0]);
				assertEquals(true, (boolean) ((Boolean) rec[0]));
			}
		});

		series.addTest(new TestCase("Simple exchange " + ssls) {
			final Object[] sent = new Object[1];
			final Object[] recd = new Object[1];
			final Exception[] err = new Exception[1];

			@Override
			public void test() throws Exception {
				Server server = new Server(this.getName(), 0, ssl) {
					@Override
					protected Object onRequest(ConnectActionServer action,
							Version clientVersion, Object data)
							throws Exception {
						sent[0] = data;
						return "pong";
					}

					@Override
					protected void onError(Exception e) {
						super.onError(e);
						err[0] = e;
					}
				};

				server.start(false);

				try {
					new ConnectActionClient(null, server.getPort(), ssl) {
						@Override
						public void action(Version serverVersion)
								throws Exception {
							recd[0] = send("ping");
						}
					}.connect();
				} finally {
					server.stop();
				}

				if (err[0] != null) {
					fail("An exception was thrown: " + err[0].getMessage());
				}

				assertEquals("ping", sent[0]);
				assertEquals("pong", recd[0]);
			}
		});

		series.addTest(new TestCase("Multiple exchanges " + ssls) {
			final Object[] sent = new Object[3];
			final Object[] recd = new Object[3];
			final Exception[] err = new Exception[1];

			@Override
			public void test() throws Exception {
				Server server = new Server(this.getName(), 0, ssl) {
					@Override
					protected Object onRequest(ConnectActionServer action,
							Version clientVersion, Object data)
							throws Exception {
						sent[0] = data;
						action.send("pong");
						sent[1] = action.flush();
						return "pong2";
					}

					@Override
					protected void onError(Exception e) {
						super.onError(e);
						err[0] = e;
					}
				};

				server.start(false);

				try {
					new ConnectActionClient(null, server.getPort(), ssl) {
						@Override
						public void action(Version serverVersion)
								throws Exception {
							recd[0] = send("ping");
							recd[1] = send("ping2");
						}
					}.connect();
				} finally {
					server.stop();
				}

				if (err[0] != null) {
					fail("An exception was thrown: " + err[0].getMessage());
				}

				assertEquals("ping", sent[0]);
				assertEquals("pong", recd[0]);
				assertEquals("ping2", sent[1]);
				assertEquals("pong2", recd[1]);
			}
		});

		series.addTest(new TestCase("Multiple call from client " + ssls) {
			final Object[] sent = new Object[3];
			final Object[] recd = new Object[3];
			final Exception[] err = new Exception[1];

			@Override
			public void test() throws Exception {
				Server server = new Server(this.getName(), 0, ssl) {
					@Override
					protected Object onRequest(ConnectActionServer action,
							Version clientVersion, Object data)
							throws Exception {
						sent[(Integer) data] = data;
						return ((Integer) data) * 2;
					}

					@Override
					protected void onError(Exception e) {
						super.onError(e);
						err[0] = e;
					}
				};

				server.start(false);

				try {
					new ConnectActionClient(null, server.getPort(), ssl) {
						@Override
						public void action(Version serverVersion)
								throws Exception {
							for (int i = 0; i < 3; i++) {
								recd[i] = send(i);
							}
						}
					}.connect();
				} finally {
					server.stop();
				}

				if (err[0] != null) {
					fail("An exception was thrown: " + err[0].getMessage());
				}

				assertEquals(0, sent[0]);
				assertEquals(0, recd[0]);
				assertEquals(1, sent[1]);
				assertEquals(2, recd[1]);
				assertEquals(2, sent[2]);
				assertEquals(4, recd[2]);
			}
		});

		return series;
	}

	public SerialTest(String[] args) {
		super("Serial test", args);

		addSeries(createServerTestCases(args, false));

		addSeries(createServerTestCases(args, true));

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

		addTest(new TestCase("Array in Object Import/Export") {
			@Override
			public void test() throws Exception {
				Object data = new DataArray();// new String[] { "un", "deux" };
				String encoded = new Exporter().append(data).toString(false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata)
						.toString(false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("Array Import/Export") {
			@Override
			public void test() throws Exception {
				Object data = new String[] { "un", "deux" };
				String encoded = new Exporter().append(data).toString(false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata)
						.toString(false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});

		addTest(new TestCase("Enum Import/Export") {
			@Override
			public void test() throws Exception {
				Object data = EnumToSend.FANFAN;
				String encoded = new Exporter().append(data).toString(false);
				Object redata = new Importer().read(encoded).getValue();
				String reencoded = new Exporter().append(redata)
						.toString(false);

				assertEquals(encoded.replaceAll("@[0-9]*", "@REF"),
						reencoded.replaceAll("@[0-9]*", "@REF"));
			}
		});
	}

	class DataArray {
		public String[] data = new String[] { "un", "deux" };
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

	enum EnumToSend {
		FANFAN, TULIPE,
	}
}
