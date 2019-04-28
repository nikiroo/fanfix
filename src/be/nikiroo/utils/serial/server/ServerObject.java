package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws UnknownHostException
	 *             if the IP address of the host could not be determined
	 * @throws IllegalArgumentException
	 *             if the port parameter is outside the specified range of valid
	 *             port values, which is between 0 and 65535, inclusive
	 */
	public ServerObject(int port, String key) throws IOException {
		super(port, key);
	}

	/**
	 * Create a new server that will start listening on the network when
	 * {@link ServerObject#start()} is called.
	 * 
	 * @param name
	 *            the server name (only used for debug info and traces)
	 * @param port
	 *            the port to listen on
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws UnknownHostException
	 *             if the IP address of the host could not be determined
	 * @throws IllegalArgumentException
	 *             if the port parameter is outside the specified range of valid
	 *             port values, which is between 0 and 65535, inclusive
	 */
	public ServerObject(String name, int port, String key) throws IOException {
		super(name, port, key);
	}

	@Override
	protected ConnectActionServer createConnectActionServer(Socket s) {
		return new ConnectActionServerObject(s, key) {
			@Override
			public void action() throws Exception {
				try {
					for (Object data = rec(); true; data = rec()) {
						Object rep = null;
						try {
							rep = onRequest(this, data);
							if (isClosing()) {
								return;
							}
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
	 * @param data
	 *            the data sent by the client (which can be NULL)
	 * 
	 * @return the answer to return to the client (which can be NULL)
	 * 
	 * @throws Exception
	 *             in case of an exception, the error will only be logged
	 */
	abstract protected Object onRequest(ConnectActionServerObject action,
			Object data) throws Exception;
}
