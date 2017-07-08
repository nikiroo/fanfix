package be.nikiroo.utils.serial;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import be.nikiroo.utils.Version;

abstract public class Server implements Runnable {
	static private final String[] ANON_CIPHERS = getAnonCiphers();

	private int port;
	private boolean ssl;
	private ServerSocket ss;
	private boolean started;
	private boolean exiting = false;
	private int counter;
	private Object lock = new Object();
	private Object counterLock = new Object();

	@Deprecated
	public Server(@SuppressWarnings("unused") Version notUsed, int port,
			boolean ssl) throws IOException {
		this(port, ssl);
	}

	public Server(int port, boolean ssl) throws IOException {
		this.port = port;
		this.ssl = ssl;
		this.ss = createSocketServer(port, ssl);
	}

	public void start() {
		synchronized (lock) {
			if (!started) {
				started = true;
				new Thread(this).start();
			}
		}
	}

	public void stop() {
		stop(0, true);
	}

	// wait = wait before returning (sync VS async) timeout in ms, 0 or -1 for
	// never
	public void stop(final long timeout, final boolean wait) {
		if (wait) {
			stop(timeout);
		} else {
			new Thread(new Runnable() {
				@Override
				public void run() {
					stop(timeout);
				}
			}).start();
		}
	}

	// timeout in ms, 0 or -1 or never
	private void stop(long timeout) {
		synchronized (lock) {
			if (started && !exiting) {
				exiting = true;

				try {
					new ConnectActionClient(createSocket(null, port, ssl)) {
						@Override
						public void action(Version serverVersion)
								throws Exception {
						}
					}.connect();

					long time = 0;
					while (ss != null && timeout > 0 && timeout > time) {
						Thread.sleep(10);
						time += 10;
					}
				} catch (Exception e) {
					if (ss != null) {
						counter = 0; // will stop the main thread
						onError(e);
					}
				}
			}

			// only return when stopped
			while (started || exiting) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	@Override
	public void run() {
		try {
			while (started && !exiting) {
				count(1);
				Socket s = ss.accept();
				new ConnectActionServer(s) {
					private Version clientVersion = new Version();

					@Override
					public void action(Version dummy) throws Exception {
						try {
							for (Object data = flush(); true; data = flush()) {
								Object rep = null;
								try {
									rep = onRequest(this, clientVersion, data);
								} catch (Exception e) {
									onError(e);
								}
								send(rep);
							}
						} catch (NullPointerException e) {
							// Client has no data any more, we quit
						}
					}

					@Override
					public void connect() {
						try {
							super.connect();
						} finally {
							count(-1);
						}
					}

					@Override
					protected void onClientVersionReceived(Version clientVersion) {
						this.clientVersion = clientVersion;
					}
				}.connectAsync();
			}

			// Will be covered by @link{Server#stop(long)} for timeouts
			while (counter > 0) {
				Thread.sleep(10);
			}
		} catch (Exception e) {
			if (counter > 0) {
				onError(e);
			}
		} finally {
			try {
				ss.close();
			} catch (Exception e) {
				onError(e);
			}

			ss = null;

			started = false;
			exiting = false;
			counter = 0;
		}
	}

	abstract protected Object onRequest(ConnectActionServer action,
			Version clientVersion, Object data) throws Exception;

	protected void onError(Exception e) {
		if (e == null) {
			e = new Exception("Unknown error");
		}

		e.printStackTrace();
	}

	private int count(int change) {
		synchronized (counterLock) {
			counter += change;
			return counter;
		}
	}

	static Socket createSocket(String host, int port, boolean ssl)
			throws IOException {
		Socket s;
		if (ssl) {
			s = SSLSocketFactory.getDefault().createSocket(host, port);
			((SSLSocket) s).setEnabledCipherSuites(ANON_CIPHERS);
		} else {
			s = new Socket(host, port);
		}

		return s;
	}

	static ServerSocket createSocketServer(int port, boolean ssl)
			throws IOException {
		ServerSocket ss;
		if (ssl) {
			ss = SSLServerSocketFactory.getDefault().createServerSocket(port);
			((SSLServerSocket) ss).setEnabledCipherSuites(ANON_CIPHERS);
		} else {
			ss = new ServerSocket(port);
		}

		return ss;
	}

	private static String[] getAnonCiphers() {
		List<String> anonCiphers = new ArrayList<String>();
		for (String cipher : ((SSLSocketFactory) SSLSocketFactory.getDefault())
				.getSupportedCipherSuites()) {
			if (cipher.contains("_anon_")) {
				anonCiphers.add(cipher);
			}
		}

		return anonCiphers.toArray(new String[] {});
	}
}
