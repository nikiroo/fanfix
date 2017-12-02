package be.nikiroo.utils.test;

import java.net.URL;

import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.server.ConnectActionClientObject;
import be.nikiroo.utils.serial.server.ConnectActionClientString;
import be.nikiroo.utils.serial.server.ConnectActionServerObject;
import be.nikiroo.utils.serial.server.ConnectActionServerString;
import be.nikiroo.utils.serial.server.ServerBridge;
import be.nikiroo.utils.serial.server.ServerObject;
import be.nikiroo.utils.serial.server.ServerString;

class SerialServerTest extends TestLauncher {
	private TestLauncher createServerStringTestCases(final String[] args,
			final boolean ssl, final boolean bridge) {
		final String ssls = (ssl ? "(ssl)" : "(plain text)");
		final String bridges = (bridge ? " with bridge" : "");
		TestLauncher series = new TestLauncher(
				"ServerString " + ssls + bridges, args);

		series.addTest(new TestCase("Simple connection " + ssls) {
			@Override
			public void test() throws Exception {
				final String[] rec = new String[1];

				ServerString server = new ServerString(this.getName(), 0, ssl) {
					@Override
					protected String onRequest(
							ConnectActionServerString action,
							Version clientVersion, String data)
							throws Exception {
						return null;
					}

					@Override
					protected void onError(Exception e) {
					}
				};

				int port = server.getPort();
				assertEquals("A port should have been assigned", true, port > 0);

				server.start();

				ServerBridge br = null;
				if (bridge) {
					br = new ServerBridge(0, ssl, "", port, ssl);
					br.setTraceHandler(null);

					port = br.getPort();
					assertEquals(
							"A port should have been assigned to the bridge",
							true, port > 0);

					br.start();
				}

				try {
					try {
						new ConnectActionClientObject(null, port, ssl) {
							@Override
							public void action(Version serverVersion)
									throws Exception {
								rec[0] = "ok";
							}
						}.connect();
					} finally {
						server.stop();
					}
				} finally {
					if (br != null) {
						br.stop();
					}
				}

				assertNotNull("The client action was not run", rec[0]);
				assertEquals("ok", rec[0]);
			}
		});

		series.addTest(new TestCase("Simple exchange " + ssls) {
			final String[] sent = new String[1];
			final String[] recd = new String[1];
			final Exception[] err = new Exception[1];

			@Override
			public void test() throws Exception {
				ServerString server = new ServerString(this.getName(), 0, ssl) {
					@Override
					protected String onRequest(
							ConnectActionServerString action,
							Version clientVersion, String data)
							throws Exception {
						sent[0] = data;
						return "pong";
					}

					@Override
					protected void onError(Exception e) {
						err[0] = e;
					}
				};

				int port = server.getPort();

				server.start();

				ServerBridge br = null;
				if (bridge) {
					br = new ServerBridge(0, ssl, "", port, ssl);
					br.setTraceHandler(null);
					port = br.getPort();
					br.start();
				}

				try {
					try {
						new ConnectActionClientString(null, port, ssl) {
							@Override
							public void action(Version serverVersion)
									throws Exception {
								recd[0] = send("ping");
							}
						}.connect();
					} finally {
						server.stop();
					}
				} finally {
					if (br != null) {
						br.stop();
					}
				}

				if (err[0] != null) {
					fail("An exception was thrown: " + err[0].getMessage());
				}

				assertEquals("ping", sent[0]);
				assertEquals("pong", recd[0]);
			}
		});

		series.addTest(new TestCase("Multiple exchanges " + ssls) {
			final String[] sent = new String[3];
			final String[] recd = new String[3];
			final Exception[] err = new Exception[1];

			@Override
			public void test() throws Exception {
				ServerString server = new ServerString(this.getName(), 0, ssl) {
					@Override
					protected String onRequest(
							ConnectActionServerString action,
							Version clientVersion, String data)
							throws Exception {
						sent[0] = data;
						action.send("pong");
						sent[1] = action.rec();
						return "pong2";
					}

					@Override
					protected void onError(Exception e) {
						err[0] = e;
					}
				};

				int port = server.getPort();

				server.start();

				ServerBridge br = null;
				if (bridge) {
					br = new ServerBridge(0, ssl, "", port, ssl);
					br.setTraceHandler(null);
					port = br.getPort();
					br.start();
				}

				try {
					try {
						new ConnectActionClientString(null, port, ssl) {
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
				} finally {
					if (br != null) {
						br.stop();
					}
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
			final String[] sent = new String[3];
			final String[] recd = new String[3];
			final Exception[] err = new Exception[1];

			@Override
			public void test() throws Exception {
				ServerString server = new ServerString(this.getName(), 0, ssl) {
					@Override
					protected String onRequest(
							ConnectActionServerString action,
							Version clientVersion, String data)
							throws Exception {
						sent[Integer.parseInt(data)] = data;
						return "" + (Integer.parseInt(data) * 2);
					}

					@Override
					protected void onError(Exception e) {
						err[0] = e;
					}
				};

				int port = server.getPort();

				server.start();

				ServerBridge br = null;
				if (bridge) {
					br = new ServerBridge(0, ssl, "", port, ssl);
					br.setTraceHandler(null);
					port = br.getPort();
					br.start();
				}

				try {
					try {
						new ConnectActionClientString(null, port, ssl) {
							@Override
							public void action(Version serverVersion)
									throws Exception {
								for (int i = 0; i < 3; i++) {
									recd[i] = send("" + i);
								}
							}
						}.connect();
					} finally {
						server.stop();
					}
				} finally {
					if (br != null) {
						br.stop();
					}
				}

				if (err[0] != null) {
					fail("An exception was thrown: " + err[0].getMessage());
				}

				assertEquals("0", sent[0]);
				assertEquals("0", recd[0]);
				assertEquals("1", sent[1]);
				assertEquals("2", recd[1]);
				assertEquals("2", sent[2]);
				assertEquals("4", recd[2]);
			}
		});

		return series;
	}

	private TestLauncher createServerObjectTestCases(final String[] args,
			final boolean ssl, final boolean bridge) {
		final String ssls = (ssl ? "(ssl)" : "(plain text)");
		final String bridges = (bridge ? " with bridge" : "");
		TestLauncher series = new TestLauncher(
				"ServerObject " + ssls + bridges, args);

		series.addTest(new TestCase("Simple connection " + ssls) {
			@Override
			public void test() throws Exception {
				final Object[] rec = new Object[1];

				ServerObject server = new ServerObject(this.getName(), 0, ssl) {
					@Override
					protected Object onRequest(
							ConnectActionServerObject action,
							Version clientVersion, Object data)
							throws Exception {
						return null;
					}

					@Override
					protected void onError(Exception e) {
					}
				};

				int port = server.getPort();
				assertEquals("A port should have been assigned", true, port > 0);

				server.start();

				ServerBridge br = null;
				if (bridge) {
					br = new ServerBridge(0, ssl, "", port, ssl);
					br.setTraceHandler(null);
					port = br.getPort();
					br.start();
				}

				try {
					try {
						new ConnectActionClientObject(null, port, ssl) {
							@Override
							public void action(Version serverVersion)
									throws Exception {
								rec[0] = true;
							}

							@Override
							protected void onError(Exception e) {
							}
						}.connect();
					} finally {
						server.stop();
					}
				} finally {
					if (br != null) {
						br.stop();
					}
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
				ServerObject server = new ServerObject(this.getName(), 0, ssl) {
					@Override
					protected Object onRequest(
							ConnectActionServerObject action,
							Version clientVersion, Object data)
							throws Exception {
						sent[0] = data;
						return "pong";
					}

					@Override
					protected void onError(Exception e) {
						err[0] = e;
					}
				};

				int port = server.getPort();

				server.start();

				ServerBridge br = null;
				if (bridge) {
					br = new ServerBridge(0, ssl, "", port, ssl);
					br.setTraceHandler(null);
					port = br.getPort();
					br.start();
				}

				try {
					try {
						new ConnectActionClientObject(null, port, ssl) {
							@Override
							public void action(Version serverVersion)
									throws Exception {
								recd[0] = send("ping");
							}
						}.connect();
					} finally {
						server.stop();
					}
				} finally {
					if (br != null) {
						br.stop();
					}
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
				ServerObject server = new ServerObject(this.getName(), 0, ssl) {
					@Override
					protected Object onRequest(
							ConnectActionServerObject action,
							Version clientVersion, Object data)
							throws Exception {
						sent[0] = data;
						action.send("pong");
						sent[1] = action.rec();
						return "pong2";
					}

					@Override
					protected void onError(Exception e) {
						err[0] = e;
					}
				};

				int port = server.getPort();

				server.start();

				ServerBridge br = null;
				if (bridge) {
					br = new ServerBridge(0, ssl, "", port, ssl);
					br.setTraceHandler(null);
					port = br.getPort();
					br.start();
				}

				try {
					try {
						new ConnectActionClientObject(null, port, ssl) {
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
				} finally {
					if (br != null) {
						br.stop();
					}
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

		series.addTest(new TestCase("Object array of URLs " + ssls) {
			final Object[] sent = new Object[1];
			final Object[] recd = new Object[1];
			final Exception[] err = new Exception[1];

			@Override
			public void test() throws Exception {
				ServerObject server = new ServerObject(this.getName(), 0, ssl) {
					@Override
					protected Object onRequest(
							ConnectActionServerObject action,
							Version clientVersion, Object data)
							throws Exception {
						sent[0] = data;
						return new Object[] { "ACK" };
					}

					@Override
					protected void onError(Exception e) {
						err[0] = e;
					}
				};

				int port = server.getPort();

				server.start();

				ServerBridge br = null;
				if (bridge) {
					br = new ServerBridge(0, ssl, "", port, ssl);
					br.setTraceHandler(null);
					port = br.getPort();
					br.start();
				}

				try {
					try {
						new ConnectActionClientObject(null, port, ssl) {
							@Override
							public void action(Version serverVersion)
									throws Exception {
								recd[0] = send(new Object[] {
										"key",
										new URL(
												"https://example.com/from_client"),
										"https://example.com/from_client" });
							}
						}.connect();
					} finally {
						server.stop();
					}
				} finally {
					if (br != null) {
						br.stop();
					}
				}

				if (err[0] != null) {
					fail("An exception was thrown: " + err[0].getMessage());
				}

				Object[] sento = (Object[]) (sent[0]);
				Object[] recdo = (Object[]) (recd[0]);

				assertEquals("key", sento[0]);
				assertEquals("https://example.com/from_client",
						((URL) sento[1]).toString());
				assertEquals("https://example.com/from_client", sento[2]);
				assertEquals("ACK", recdo[0]);
			}
		});

		series.addTest(new TestCase("Multiple call from client " + ssls) {
			final Object[] sent = new Object[3];
			final Object[] recd = new Object[3];
			final Exception[] err = new Exception[1];

			@Override
			public void test() throws Exception {
				ServerObject server = new ServerObject(this.getName(), 0, ssl) {
					@Override
					protected Object onRequest(
							ConnectActionServerObject action,
							Version clientVersion, Object data)
							throws Exception {
						sent[(Integer) data] = data;
						return ((Integer) data) * 2;
					}

					@Override
					protected void onError(Exception e) {
						err[0] = e;
					}
				};

				int port = server.getPort();

				server.start();

				ServerBridge br = null;
				if (bridge) {
					br = new ServerBridge(0, ssl, "", port, ssl);
					br.setTraceHandler(null);
					port = br.getPort();
					br.start();
				}

				try {
					try {
						new ConnectActionClientObject(null, port, ssl) {
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
				} finally {
					if (br != null) {
						br.stop();
					}
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

	public SerialServerTest(String[] args) {
		super("SerialServer test", args);

		for (boolean ssl : new Boolean[] { false, true }) {
			for (boolean bridge : new Boolean[] { false, true }) {
				addSeries(createServerObjectTestCases(args, ssl, bridge));
				addSeries(createServerStringTestCases(args, ssl, bridge));
			}
		}
	}
}
