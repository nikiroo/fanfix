package be.nikiroo.utils.serial;

import java.io.IOException;
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
public class ConnectActionServer {
	private ConnectAction action;

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
	 * Serialise and send the given object to the client.
	 * 
	 * @param data
	 *            the data to send
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws NoSuchFieldException
	 *             if the serialised data contains information about a field
	 *             which does actually not exist in the class we know of
	 * @throws NoSuchMethodException
	 *             if a class described in the serialised data cannot be created
	 *             because it is not compatible with this code
	 * @throws ClassNotFoundException
	 *             if a class described in the serialised data cannot be found
	 */
	public void send(Object data) throws IOException, NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException {
		action.send(data);
	}

	/**
	 * (Flush the data to the client if needed and) retrieve its answer.
	 * 
	 * @return the deserialised answer (which can actually be NULL)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws NoSuchFieldException
	 *             if the serialised data contains information about a field
	 *             which does actually not exist in the class we know of
	 * @throws NoSuchMethodException
	 *             if a class described in the serialised data cannot be created
	 *             because it is not compatible with this code
	 * @throws ClassNotFoundException
	 *             if a class described in the serialised data cannot be found
	 * @throws java.lang.NullPointerException
	 *             if the counter part has no data to send
	 */
	public Object rec() throws NoSuchFieldException, NoSuchMethodException,
			ClassNotFoundException, IOException, java.lang.NullPointerException {
		return action.rec();
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

	// old stuff:

	/**
	 * Not used anymore. See {@link ConnectActionServer#rec()}.
	 */
	@SuppressWarnings("javadoc")
	@Deprecated
	public Object flush() throws NoSuchFieldException, NoSuchMethodException,
			ClassNotFoundException, IOException, java.lang.NullPointerException {
		return rec();
	}

	/**
	 * Not used anymore. See
	 * {@link ConnectActionServer#negotiateVersion(Version)}.
	 */
	@SuppressWarnings({ "unused", "javadoc" })
	@Deprecated
	protected void onClientVersionReceived(Version clientVersion) {
	}
}