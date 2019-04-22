package be.nikiroo.utils.serial.server;

import java.net.Socket;

import be.nikiroo.utils.Version;

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
	 * Create a new {@link ConnectActionServer} with the current application
	 * version (see {@link Version#getCurrentVersion()}) as the server version.
	 * 
	 * @param s
	 *            the socket to bind to
	 */
	public ConnectActionServer(Socket s) {
		this(s, Version.getCurrentVersion());
	}

	/**
	 * Create a new {@link ConnectActionServer}.
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param version
	 *            the server version
	 */
	public ConnectActionServer(Socket s, Version version) {
		action = new ConnectAction(s, true, version) {
			@Override
			protected void action(Version clientVersion) throws Exception {
				ConnectActionServer.this.action(clientVersion);
			}

			@Override
			protected void onError(Exception e) {
				ConnectActionServer.this.onError(e);
			}

			@Override
			protected Version negotiateVersion(Version clientVersion) {
				return ConnectActionServer.this.negotiateVersion(clientVersion);
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
		return action.getBytesSent();
	}

	/**
	 * Method that will be called when an action is performed on the server.
	 * 
	 * @param clientVersion
	 *            the client version
	 * 
	 * @throws Exception
	 *             in case of I/O error
	 */
	@SuppressWarnings("unused")
	public void action(Version clientVersion) throws Exception {
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

	/**
	 * Method called when we negotiate the version with the client.
	 * <p>
	 * Will return the actual server version by default.
	 * 
	 * @param clientVersion
	 *            the client version
	 * 
	 * @return the version to send to the client
	 */
	protected Version negotiateVersion(
			@SuppressWarnings("unused") Version clientVersion) {
		return action.getVersion();
	}
}