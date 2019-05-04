package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
	 * Create a new {@link ConnectActionClientObject} .
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 */
	public ConnectActionClientObject(Socket s, String key) {
		super(s, key);
	}

	/**
	 * Create a new {@link ConnectActionClientObject} .
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 * @param clientVersion
	 *            the version of the client
	 */
	public ConnectActionClientObject(Socket s, String key, Version clientVersion) {
		super(s, key, clientVersion);
	}

	/**
	 * Create a new {@link ConnectActionClientObject}.
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
	 *             if the IP address of the host could not be determined
	 * @throws IllegalArgumentException
	 *             if the port parameter is outside the specified range of valid
	 *             port values, which is between 0 and 65535, inclusive
	 */
	public ConnectActionClientObject(String host, int port, String key)
			throws IOException {
		super(host, port, key);
	}

	/**
	 * Create a new {@link ConnectActionClientObject}.
	 * 
	 * @param host
	 *            the host to bind to
	 * @param port
	 *            the port to bind to
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 * @param clientVersion
	 *            the version of the client
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 * @throws UnknownHostException
	 *             if the IP address of the host could not be determined
	 * @throws IllegalArgumentException
	 *             if the port parameter is outside the specified range of valid
	 *             port values, which is between 0 and 65535, inclusive
	 */
	public ConnectActionClientObject(String host, int port, String key,
			Version clientVersion) throws IOException {
		super(host, port, key, clientVersion);
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

	// Deprecated //

	/**
	 * @deprecated SSL support has been replaced by key-based encryption.
	 *             <p>
	 *             Please use the version with key encryption (this deprecated
	 *             version uses an empty key when <tt>ssl</tt> is TRUE and no
	 *             key (NULL) when <tt>ssl</tt> is FALSE).
	 */
	@Deprecated
	public ConnectActionClientObject(String host, int port, boolean ssl)
			throws IOException {
		this(host, port, ssl ? "" : null);
	}

	/**
	 * @deprecated SSL support has been replaced by key-based encryption.
	 *             <p>
	 *             Please use the version with key encryption (this deprecated
	 *             version uses an empty key when <tt>ssl</tt> is TRUE and no
	 *             key (NULL) when <tt>ssl</tt> is FALSE).
	 */
	@Deprecated
	public ConnectActionClientObject(String host, int port, boolean ssl,
			Version version) throws IOException {
		this(host, port, ssl ? "" : null, version);
	}

	/**
	 * @deprecated SSL support has been replaced by key-based encryption.
	 *             <p>
	 *             Please use the version with key encryption (this deprecated
	 *             version uses an empty key when <tt>ssl</tt> is TRUE and no
	 *             key (NULL) when <tt>ssl</tt> is FALSE).
	 */
	@SuppressWarnings("unused")
	@Deprecated
	public ConnectActionClientObject(Socket s, boolean ssl) throws IOException {
		this(s, ssl ? "" : null);
	}

	/**
	 * @deprecated SSL support has been replaced by key-based encryption.
	 *             <p>
	 *             Please use the version with key encryption (this deprecated
	 *             version uses an empty key when <tt>ssl</tt> is TRUE and no
	 *             key (NULL) when <tt>ssl</tt> is FALSE).
	 */
	@SuppressWarnings("unused")
	@Deprecated
	public ConnectActionClientObject(Socket s, boolean ssl, Version version)
			throws IOException {
		this(s, ssl ? "" : null, version);
	}
}