package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;

import be.nikiroo.utils.Version;

/**
 * This class implements a simple server that can listen for connections and
 * send/receive Strings.
 * <p>
 * Note: this {@link ServerString} has to be discarded after use (cannot be
 * started twice).
 * 
 * @author niki
 */
abstract public class ServerString extends Server {
	/**
	 * Create a new server that will start listening on the network when
	 * {@link ServerString#start()} is called.
	 * 
	 * @param port
	 *            the port to listen on, or 0 to assign any unallocated port
	 *            found (which can later on be queried via
	 *            {@link ServerString#getPort()}
	 * @param ssl
	 *            use a SSL connection (or not)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public ServerString(int port, boolean ssl) throws IOException {
		super(port, ssl);
	}

	/**
	 * Create a new server that will start listening on the network when
	 * {@link ServerString#start()} is called.
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
	public ServerString(String name, int port, boolean ssl) throws IOException {
		super(name, port, ssl);
	}

	@Override
	protected ConnectActionServer createConnectActionServer(Socket s) {
		return new ConnectActionServerString(s) {
			@Override
			public void action(Version clientVersion) throws Exception {
				try {
					for (String data = rec(); data != null; data = rec()) {
						String rep = null;
						try {
							rep = onRequest(this, clientVersion, data);
						} catch (Exception e) {
							onError(e);
						}

						if (rep == null) {
							rep = "";
						}

						send(rep);
					}
				} catch (NullPointerException e) {
					// Client has no data any more, we quit
					getTraceHandler()
							.trace(getName()
									+ ": client has data no more, stopping connection");
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
		};
	}

	/**
	 * This is the method that is called on each client request.
	 * <p>
	 * You are expected to react to it and return an answer (NULL will be
	 * converted to an empty {@link String}).
	 * 
	 * @param action
	 *            the client action
	 * @param clientVersion
	 *            the client version
	 * @param data
	 *            the data sent by the client
	 * 
	 * @return the answer to return to the client
	 * 
	 * @throws Exception
	 *             in case of an exception, the error will only be logged
	 */
	abstract protected String onRequest(ConnectActionServerString action,
			Version clientVersion, String data) throws Exception;
}
