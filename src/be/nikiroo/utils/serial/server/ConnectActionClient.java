package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

/**
 * Base class used for the client basic handling.
 * <p>
 * It represents a single action: a client is expected to only execute one
 * action.
 * 
 * @author niki
 */
abstract class ConnectActionClient {
	/**
	 * The underlying {@link ConnectAction}.
	 * <p>
	 * Cannot be NULL.
	 */
	protected ConnectAction action;

	/**
	 * Create a new {@link ConnectActionClient}.
	 * 
	 * @param host
	 *            the host to bind to
	 * @param port
	 *            the port to bind to
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws UnknownHostException
	 *             if the host is not known
	 * @throws IllegalArgumentException
	 *             if the port parameter is outside the specified range of valid
	 *             port values, which is between 0 and 65535, inclusive
	 */
	public ConnectActionClient(String host, int port, String key)
			throws IOException {
		this(new Socket(host, port), key);
	}

	/**
	 * Create a new {@link ConnectActionClient}.
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 */
	public ConnectActionClient(Socket s, String key) {
		action = new ConnectAction(s, false, key) {
			@Override
			protected void action() throws Exception {
				ConnectActionClient.this.clientHello();
				ConnectActionClient.this.action();
			}

			@Override
			protected void onError(Exception e) {
				ConnectActionClient.this.onError(e);
			}
		};
	}

	/**
	 * Send the HELLO message (send a String "HELLO" to the server, to check I/O
	 * and encryption modes).
	 * <p>
	 * Will automatically handle the answer (the server must answer "HELLO" in
	 * kind).
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws SSLException
	 *             in case of encryption error
	 */
	protected void clientHello() throws IOException {
		String HELLO = action.sendString("HELLO");
		if (!"HELLO".equals(HELLO)) {
			throw new SSLException("Server did not accept the encryption key");
		}
	}

	/**
	 * Actually start the process and call the action (synchronous).
	 */
	public void connect() {
		action.connect();
	}

	/**
	 * Actually start the process and call the action (asynchronous).
	 */
	public void connectAsync() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				connect();
			}
		}).start();
	}

	/**
	 * Method that will be called when an action is performed on the client.
	 * 
	 * @throws Exception
	 *             in case of I/O error
	 */
	@SuppressWarnings("unused")
	public void action() throws Exception {
	}

	/**
	 * Handler called when an unexpected error occurs in the code.
	 * <p>
	 * Will just ignore the error by default.
	 * 
	 * @param e
	 *            the exception that occurred
	 */
	protected void onError(@SuppressWarnings("unused") Exception e) {
	}
}