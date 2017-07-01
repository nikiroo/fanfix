package be.nikiroo.utils.serial;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import be.nikiroo.utils.Version;

abstract class ConnectAction {
	private Socket s;
	private boolean server;
	private Version version;

	private Object lock = new Object();
	private BufferedReader in;
	private OutputStreamWriter out;
	private boolean contentToSend;

	// serverVersion = null on server (or bad clients)
	abstract public void action(Version serverVersion) throws Exception;

	// server = version NULL
	protected ConnectAction(Socket s, boolean server, Version version) {
		this.s = s;
		this.server = server;

		if (version == null) {
			this.version = new Version();
		} else {
			this.version = version;
		}
	}

	public void connectAsync() {
		new Thread(new Runnable() {
			public void run() {
				connect();
			}
		}).start();
	}

	public void connect() {
		try {
			in = new BufferedReader(new InputStreamReader(s.getInputStream(),
					"UTF-8"));
			try {
				out = new OutputStreamWriter(s.getOutputStream(), "UTF-8");
				try {
					if (server) {
						action(version);
					} else {
						String v = sendString("VERSION " + version.toString());
						if (v != null && v.startsWith("VERSION ")) {
							v = v.substring("VERSION ".length());
						}

						action(new Version(v));
					}
				} finally {
					out.close();
				}
			} finally {
				in.close();
			}
		} catch (Exception e) {
			onError(e);
		} finally {
			try {
				s.close();
			} catch (Exception e) {
				onError(e);
			}
		}
	}

	// (also, server never get anything)
	public Object send(Object data) throws IOException, NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException {
		synchronized (lock) {
			String rep = sendString(new Exporter().append(data).toString(true));
			return new Importer().read(rep).getValue();
		}
	}

	public Object flush() throws NoSuchFieldException, NoSuchMethodException,
			ClassNotFoundException, IOException, java.lang.NullPointerException {
		String str = flushString();
		if (str == null) {
			throw new NullPointerException("No more data from client");
		}

		return new Importer().read(str).getValue();
	}

	protected void onClientVersionReceived(Version clientVersion) {

	}

	protected void onError(Exception e) {

	}

	// \n included in line, but not in rep (also, server never get anything)
	private String sendString(String line) throws IOException {
		synchronized (lock) {
			out.write(line);
			out.write("\n");

			if (server) {
				out.flush();
				return null;
			} else {
				contentToSend = true;
				return flushString();
			}
		}
	}

	// server can receive something even without pending content
	private String flushString() throws IOException {
		synchronized (lock) {
			if (server || contentToSend) {
				if (contentToSend) {
					out.flush();
					contentToSend = false;
				}

				String line = in.readLine();
				if (server && line != null && line.startsWith("VERSION ")) {
					// "VERSION client-version" (VERSION 1.0.0)
					Version clientVersion = new Version(
							line.substring("VERSION ".length()));
					onClientVersionReceived(clientVersion);
					sendString("VERSION " + version.toString());

					line = in.readLine();
				}

				return line;
			} else {
				return null;
			}
		}
	}
}