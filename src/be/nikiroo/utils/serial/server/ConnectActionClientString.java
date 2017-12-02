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
public class ConnectActionClientString extends ConnectActionClient {
	/**
	 * Create a new {@link ConnectActionClientString} with the current
	 * application version (see {@link Version#getCurrentVersion()}) as the
	 * client version.
	 * 
	 * @param s
	 *            the socket to bind to
	 */
	public ConnectActionClientString(Socket s) {
		super(s);
	}

	/**
	 * Create a new {@link ConnectActionClientString} with the current
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
	 *             in case of I/O error
	 * @throws UnknownHostException
	 *             if the IP address of the host could not be determined
	 * @throws IllegalArgumentException
	 *             if the port parameter is outside the specified range of valid
	 *             port values, which is between 0 and 65535, inclusive
	 */
	public ConnectActionClientString(String host, int port, boolean ssl)
			throws IOException {
		super(host, port, ssl);
	}

	/**
	 * Create a new {@link ConnectActionClientString}.
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
	 *             if the IP address of the host could not be determined
	 * @throws IllegalArgumentException
	 *             if the port parameter is outside the specified range of valid
	 *             port values, which is between 0 and 65535, inclusive
	 */
	public ConnectActionClientString(String host, int port, boolean ssl,
			Version version) throws IOException {
		super(host, port, ssl, version);
	}

	/**
	 * Create a new {@link ConnectActionClientString}.
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param version
	 *            the client version
	 */
	public ConnectActionClientString(Socket s, Version version) {
		super(s, version);
	}

	/**
	 * Send the given object to the server (and return the answer).
	 * 
	 * @param data
	 *            the data to send
	 * 
	 * @return the answer, which can be NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public String send(String data) throws IOException {
		return action.sendString(data);
	}
}