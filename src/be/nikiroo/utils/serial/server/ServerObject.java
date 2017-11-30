package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;

import be.nikiroo.utils.Version;

/**
 * This class implements a simple server that can listen for connections and
 * send/receive objects.
 * <p>
 * Note: this {@link ServerObject} has to be discarded after use (cannot be
 * started twice).
 * 
 * @author niki
 */
abstract public class ServerObject extends Server {
	/**
	 * Create a new server that will start listening on the network when
	 * {@link ServerObject#start()} is called.
	 * 
	 * @param port
	 *            the port to listen on, or 0 to assign any unallocated port
	 *            found (which can later on be queried via
	 *            {@link ServerObject#getPort()}
	 * @param ssl
	 *            use a SSL connection (or not)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public ServerObject(int port, boolean ssl) throws IOException {
		super(port, ssl);
	}

	/**
	 * Create a new server that will start listening on the network when
	 * {@link ServerObject#start()} is called.
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
	public ServerObject(String name, int port, boolean ssl) throws IOException {
		super(name, port, ssl);
	}

	@Override
	protected ConnectActionServer createConnectActionServer(Socket s) {
		return new ConnectActionServerObject(s) {
			@Override
			public void action(Version clientVersion) throws Exception {
				try {
					for (Object data = rec(); true; data = rec()) {
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
			protected void onError(Exception e) {
				ServerObject.this.onError(e);
			}
		};
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
	abstract protected Object onRequest(ConnectActionServerObject action,
			Version clientVersion, Object data) throws Exception;
}
