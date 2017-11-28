package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;

import be.nikiroo.utils.Version;

/**
 * Class used for the client basic handling.
 * <p>
 * It represents a single action: a client is expected to only execute one
 * action.
 * 
 * @author niki
 */
public class ConnectActionClientObject extends ConnectActionClient {
	/**
	 * Create a new {@link ConnectActionClientObject} with the current
	 * application version (see {@link Version#getCurrentVersion()}) as the
	 * client version.
	 * 
	 * @param s
	 *            the socket to bind to
	 */
	public ConnectActionClientObject(Socket s) {
		super(s);
	}

	/**
	 * Create a new {@link ConnectActionClientObject} with the current
	 * application version (see {@link Version#getCurrentVersion()}) as the
	 * client version.
	 * 
	 * @param host
	 *            the host to bind to
	 * @param port
	 *            the port to bind to
	 * @param ssl
	 *            TRUE for an SSL connection, FALSE for plain text
	 * 
	 * @throws IOException
	 *             in case of I/O error when creating the socket
	 */
	public ConnectActionClientObject(String host, int port, boolean ssl)
			throws IOException {
		super(host, port, ssl);
	}

	/**
	 * Create a new {@link ConnectActionClientObject}.
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
	 *             in case of I/O error when creating the socket
	 */
	public ConnectActionClientObject(String host, int port, boolean ssl,
			Version version) throws IOException {
		super(host, port, ssl, version);
	}

	/**
	 * Create a new {@link ConnectActionClientObject}.
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param version
	 *            the client version
	 */
	public ConnectActionClientObject(Socket s, Version version) {
		super(s, version);
	}

	/**
	 * Serialise and send the given object to the server (and return the
	 * deserialised answer).
	 * 
	 * @param data
	 *            the data to send
	 * 
	 * @return the answer, which can be NULL
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
	public Object send(Object data) throws IOException, NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException {
		return action.sendObject(data);
	}
}