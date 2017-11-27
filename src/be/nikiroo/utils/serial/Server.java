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

import be.nikiroo.utils.TraceHandler;
import be.nikiroo.utils.Version;

/**
 * This class implements a simple server that can listen for connections and
 * send/receive objects.
 * <p>
 * Note: this {@link Server} has to be discarded after use (cannot be started
 * twice).
 * 
 * @author niki
 */
abstract public class Server implements Runnable {
	static private final String[] ANON_CIPHERS = getAnonCiphers();

	private final String name;
	private final boolean ssl;
	private final Object lock = new Object();
	private final Object counterLock = new Object();

	private ServerSocket ss;
	private int port;

	private boolean started;
	private boolean exiting = false;
	private int counter;

	private TraceHandler tracer = new TraceHandler();

	@Deprecated
	public Server(@SuppressWarnings("unused") Version notUsed, int port,
			boolean ssl) throws IOException {
		this(port, ssl);
	}

	/**
	 * Create a new server that will start listening on the network when
	 * {@link Server#start()} is called.
	 * 
	 * @param port
	 *            the port to listen on, or 0 to assign any unallocated port
	 *            found (which can later on be queried via
	 *            {@link Server#getPort()}
	 * @param ssl
	 *            use a SSL connection (or not)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Server(int port, boolean ssl) throws IOException {
		this((String) null, port, ssl);
	}

	/**
	 * Create a new server that will start listening on the network when
	 * {@link Server#start()} is called.
	 * 
	 * @param name
	 *            the server name (only used for debug info and traces)
	 * @param port
	 *            the port to listen on
	 * @param ssl
	 *            use a SSL connection (or not)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Server(String name, int port, boolean ssl) throws IOException {
		this.name = name;
		this.port = port;
		this.ssl = ssl;
		this.ss = createSocketServer(port, ssl);

		if (this.port == 0) {
			this.port = this.ss.getLocalPort();
		}
	}

	/**
	 * The traces handler for this {@link Server}.
	 * 
	 * @return the traces handler
	 */
	public TraceHandler getTraceHandler() {
		return tracer;
	}

	/**
	 * The traces handler for this {@link Server}.
	 * 
	 * @param tracer
	 *            the new traces handler
	 */
	public void setTraceHandler(TraceHandler tracer) {
		if (tracer == null) {
			tracer = new TraceHandler(false, false, false);
		}

		this.tracer = tracer;
	}

	/**
	 * Return the assigned port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Start the server (listen on the network for new connections).
	 * <p>
	 * Can only be called once.
	 */
	public void start() {
		boolean ok = false;
		synchronized (lock) {
			if (!started && ss != null) {
				started = true;
				new Thread(this).start();
				ok = true;
			}
		}

		if (ok) {
			tracer.trace(name + ": server started on port " + port);
		} else if (ss == null) {
			tracer.error(name + ": cannot start server on port " + port
					+ ", it has already been used");
		} else {
			tracer.error(name + ": cannot start server on port " + port
					+ ", it is already started");
		}
	}

	/**
	 * Will stop the server, synchronously and without a timeout.
	 */
	public void stop() {
		tracer.trace(name + ": stopping server");
		stop(0, true);
	}

	/**
	 * Stop the server.
	 * 
	 * @param timeout
	 *            the maximum timeout to wait for existing actions to complete,
	 *            or 0 for "no timeout"
	 * @param wait
	 *            wait for the server to be stopped before returning
	 *            (synchronous) or not (asynchronous)
	 */
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

	/**
	 * Stop the server (synchronous).
	 * 
	 * @param timeout
	 *            the maximum timeout to wait for existing actions to complete,
	 *            or 0 for "no timeout"
	 */
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
			tracer.trace(name + ": server starting on port " + port);
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

	/**
	 * This is the method that is called on each client request.
	 * <p>
	 * You are expected to react to it and return an answer (which can be NULL).
	 * 
	 * @param action
	 *            the client action
	 * @param clientVersion
	 *            the client version
	 * @param data
	 *            the data sent by the client (which can be NULL)
	 * 
	 * @return the answer to return to the client (which can be NULL)
	 * 
	 * @throws Exception
	 *             in case of an exception, the error will only be logged
	 */
	abstract protected Object onRequest(ConnectActionServer action,
			Version clientVersion, Object data) throws Exception;

	/**
	 * This method will be called on errors.
	 * <p>
	 * By default, it will only call the trace handler (so you may want to call
	 * super {@link Server#onError} if you override it).
	 * 
	 * @param e
	 *            the error
	 */
	protected void onError(Exception e) {
		tracer.error(e);
	}

	/**
	 * Change the number of currently serviced actions.
	 * 
	 * @param change
	 *            the number to increase or decrease
	 * 
	 * @return the current number after this operation
	 */
	private int count(int change) {
		synchronized (counterLock) {
			counter += change;
			return counter;
		}
	}

	/**
	 * Create a {@link Socket}.
	 * 
	 * @param host
	 *            the host to connect to
	 * @param port
	 *            the port to connect to
	 * @param ssl
	 *            TRUE for SSL mode (or FALSE for plain text mode)
	 * 
	 * @return the {@link Socket}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
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

	/**
	 * Create a {@link ServerSocket}.
	 * 
	 * @param port
	 *            the port to accept connections on
	 * @param ssl
	 *            TRUE for SSL mode (or FALSE for plain text mode)
	 * 
	 * @return the {@link ServerSocket}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
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

	/**
	 * Return all the supported ciphers that do not use authentication.
	 * 
	 * @return the list of such supported ciphers
	 */
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
