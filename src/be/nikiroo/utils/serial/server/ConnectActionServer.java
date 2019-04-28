package be.nikiroo.utils.serial.server;

import java.net.Socket;

/**
 * Base class used for the server basic handling.
 * <p>
 * It represents a single action: a server is expected to execute one action for
 * each client action.
 * 
 * @author niki
 */
abstract class ConnectActionServer {
	private boolean closing;

	/**
	 * The underlying {@link ConnectAction}.
	 * <p>
	 * Cannot be NULL.
	 */
	protected ConnectAction action;

	/**
	 * Create a new {@link ConnectActionServer}.
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 */
	public ConnectActionServer(Socket s, String key) {
		action = new ConnectAction(s, true, key) {
			@Override
			protected void action() throws Exception {
				ConnectActionServer.this.action();
			}

			@Override
			protected void onError(Exception e) {
				ConnectActionServer.this.onError(e);
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
	 * Stop the client/server connection on behalf of the server (usually, the
	 * client connects then is allowed to send as many requests as it wants; in
	 * some cases, though, the server may wish to forcefully close the
	 * connection and can do via this value, when it is set to TRUE).
	 * <p>
	 * Example of usage: the client failed an authentication check, cut the
	 * connection here and now.
	 * 
	 * @return TRUE when it is
	 */
	public boolean isClosing() {
		return closing;
	}

	/**
	 * Can be called to stop the client/server connection on behalf of the
	 * server (usually, the client connects then is allowed to send as many
	 * requests as it wants; in some cases, though, the server may wish to
	 * forcefully close the connection and can do so by calling this method).
	 * <p>
	 * Example of usage: the client failed an authentication check, cut the
	 * connection here and now.
	 */
	public void close() {
		closing = true;
	}

	/**
	 * The total amount of bytes received.
	 * 
	 * @return the amount of bytes received
	 */
	public long getBytesReceived() {
		return action.getBytesReceived();
	}

	/**
	 * The total amount of bytes sent.
	 * 
	 * @return the amount of bytes sent
	 */
	public long getBytesSent() {
		return action.getBytesWritten();
	}

	/**
	 * Method that will be called when an action is performed on the server.
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