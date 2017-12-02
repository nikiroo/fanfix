package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import be.nikiroo.utils.Version;

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
	 * Create a new {@link ConnectActionClient} with the current application
	 * version (see {@link Version#getCurrentVersion()}) as the client version.
	 * 
	 * @param s
	 *            the socket to bind to
	 */
	public ConnectActionClient(Socket s) {
		this(s, Version.getCurrentVersion());
	}

	/**
	 * Create a new {@link ConnectActionClient} with the current application
	 * version (see {@link Version#getCurrentVersion()}) as the client version.
	 * 
	 * @param host
	 *            the host to bind to
	 * @param port
	 *            the port to bind to
	 * @param ssl
	 *            TRUE for an SSL connection, FALSE for plain text
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws UnknownHostException
	 *             if the host is not known
	 * @throws IllegalArgumentException
	 *             if the port parameter is outside the specified range of valid
	 *             port values, which is between 0 and 65535, inclusive
	 */
	public ConnectActionClient(String host, int port, boolean ssl)
			throws IOException {
		this(Server.createSocket(host, port, ssl), Version.getCurrentVersion());
	}

	/**
	 * Create a new {@link ConnectActionClient}.
	 * 
	 * @param host
	 *            the host to bind to
	 * @param port
	 *            the port to bind to
	 * @param ssl
	 *            TRUE for an SSL connection, FALSE for plain text
	 * @param version
	 *            the client version
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws UnknownHostException
	 *             if the host is not known
	 * @throws IllegalArgumentException
	 *             if the port parameter is outside the specified range of valid
	 *             port values, which is between 0 and 65535, inclusive
	 */
	public ConnectActionClient(String host, int port, boolean ssl,
			Version version) throws IOException {
		this(Server.createSocket(host, port, ssl), version);
	}

	/**
	 * Create a new {@link ConnectActionClient}.
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param version
	 *            the client version
	 */
	public ConnectActionClient(Socket s, Version version) {
		action = new ConnectAction(s, false, version) {
			@Override
			protected void action(Version serverVersion) throws Exception {
				ConnectActionClient.this.action(serverVersion);
			}

			@Override
			protected void onError(Exception e) {
				ConnectActionClient.this.onError(e);
			}

			@Override
			protected Version negotiateVersion(Version clientVersion) {
				new Exception("Should never be called on a client")
						.printStackTrace();
				return null;
			}
		};
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
	 * @param serverVersion
	 *            the server version
	 * 
	 * @throws Exception
	 *             in case of I/O error
	 */
	@SuppressWarnings("unused")
	public void action(Version serverVersion) throws Exception {
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