package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
	public ServerString(int port, String key) throws IOException {
		super(port, key);
	}

	/**
	 * Create a new server that will start listening on the network when
	 * {@link ServerString#start()} is called.
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
	public ServerString(String name, int port, String key) throws IOException {
		super(name, port, key);
	}

	@Override
	protected ConnectActionServer createConnectActionServer(Socket s) {
		return new ConnectActionServerString(s, key) {
			@Override
			public void action() throws Exception {
				for (String data = rec(); data != null; data = rec()) {
					String rep = null;
					try {
						rep = onRequest(this, data);
						if (isClosing()) {
							return;
						}
					} catch (Exception e) {
						onError(e);
					}

					if (rep == null) {
						rep = "";
					}

					send(rep);
				}
			}

			@Override
			protected void onError(Exception e) {
				ServerString.this.onError(e);
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
	 * @param data
	 *            the data sent by the client
	 * 
	 * @return the answer to return to the client
	 * 
	 * @throws Exception
	 *             in case of an exception, the error will only be logged
	 */
	abstract protected String onRequest(ConnectActionServerString action,
			String data) throws Exception;
}
