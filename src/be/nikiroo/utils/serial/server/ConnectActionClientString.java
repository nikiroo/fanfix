package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

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
	 * Create a new {@link ConnectActionClientString}.
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param key
	 *            an optional key to encrypt all the communications (if NULL,
	 *            everything will be sent in clear text)
	 */
	public ConnectActionClientString(Socket s, String key) {
		super(s, key);
	}

	/**
	 * Create a new {@link ConnectActionClientString}.
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
	public ConnectActionClientString(String host, int port, String key)
			throws IOException {
		super(host, port, key);
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