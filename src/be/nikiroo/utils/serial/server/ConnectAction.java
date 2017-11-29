package be.nikiroo.utils.serial.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.Exporter;
import be.nikiroo.utils.serial.Importer;

/**
 * Base class used for the client/server basic handling.
 * <p>
 * It represents a single action: a client is expected to only execute one
 * action, while a server is expected to execute one action for each client
 * action.
 * 
 * @author niki
 */
abstract class ConnectAction {
	private Socket s;
	private boolean server;
	private Version version;
	private Version clientVersion;

	private Object lock = new Object();
	private BufferedReader in;
	private OutputStreamWriter out;
	private boolean contentToSend;

	/**
	 * Method that will be called when an action is performed on either the
	 * client or server this {@link ConnectAction} represent.
	 * 
	 * @param version
	 *            the counter part version
	 * 
	 * @throws Exception
	 *             in case of I/O error
	 */
	abstract protected void action(Version version) throws Exception;

	/**
	 * Method called when we negotiate the version with the client.
	 * <p>
	 * Thus, it is only called on the server.
	 * <p>
	 * Will return the actual server version by default.
	 * 
	 * @param clientVersion
	 *            the client version
	 * 
	 * @return the version to send to the client
	 */
	abstract protected Version negotiateVersion(Version clientVersion);

	/**
	 * Handler called when an unexpected error occurs in the code.
	 * 
	 * @param e
	 *            the exception that occurred
	 */
	abstract protected void onError(Exception e);

	/**
	 * Create a new {@link ConnectAction}.
	 * 
	 * @param s
	 *            the socket to bind to
	 * @param server
	 *            TRUE for a server action, FALSE for a client action (will
	 *            impact the process)
	 * @param version
	 *            the version of this client-or-server
	 */
	protected ConnectAction(Socket s, boolean server, Version version) {
		this.s = s;
		this.server = server;

		if (version == null) {
			this.version = new Version();
		} else {
			this.version = version;
		}

		clientVersion = new Version();
	}

	/**
	 * The version of this client-or-server.
	 * 
	 * @return the version
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Actually start the process (this is synchronous).
	 */
	public void connect() {
		try {
			in = new BufferedReader(new InputStreamReader(s.getInputStream(),
					"UTF-8"));
			try {
				out = new OutputStreamWriter(s.getOutputStream(), "UTF-8");
				try {
					if (server) {
						String line = in.readLine();
						if (line != null && line.startsWith("VERSION ")) {
							// "VERSION client-version" (VERSION 1.0.0)
							Version clientVersion = new Version(
									line.substring("VERSION ".length()));
							this.clientVersion = clientVersion;
							Version v = negotiateVersion(clientVersion);
							if (v == null) {
								v = new Version();
							}

							sendString("VERSION " + v.toString());
						}

						action(clientVersion);
					} else {
						String v = sendString("VERSION " + version.toString());
						if (v != null && v.startsWith("VERSION ")) {
							v = v.substring("VERSION ".length());
						}

						action(new Version(v));
					}
				} finally {
					out.close();
				}
			} finally {
				in.close();
			}
		} catch (Exception e) {
			onError(e);
		} finally {
			try {
				s.close();
			} catch (Exception e) {
				onError(e);
			}
		}
	}

	/**
	 * Serialise and send the given object to the counter part (and, only for
	 * client, return the deserialised answer -- the server will always receive
	 * NULL).
	 * 
	 * @param data
	 *            the data to send
	 * 
	 * @return the answer (which can be NULL) if this action is a client, always
	 *         NULL if it is a server
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
	protected Object sendObject(Object data) throws IOException,
			NoSuchFieldException, NoSuchMethodException, ClassNotFoundException {
		synchronized (lock) {
			String rep = sendString(new Exporter().append(data).toString(true));
			if (rep != null) {
				return new Importer().read(rep).getValue();
			}

			return null;
		}
	}

	/**
	 * Reserved for the server: flush the data to the client and retrieve its
	 * answer.
	 * <p>
	 * Also used internally for the client (only do something if there is
	 * contentToSend).
	 * <p>
	 * Will only flush the data if there is contentToSend.
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
	protected Object recObject() throws IOException, NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException,
			java.lang.NullPointerException {
		String str = recString();
		if (str == null) {
			throw new NullPointerException("No more data available");
		}

		return new Importer().read(str).getValue();
	}

	/**
	 * Send the given string to the counter part (and, only for client, return
	 * the answer -- the server will always receive NULL).
	 * 
	 * @param line
	 *            the data to send (we will add a line feed)
	 * 
	 * @return the answer if this action is a client (without the added line
	 *         feed), NULL if it is a server
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected String sendString(String line) throws IOException {
		synchronized (lock) {
			out.write(line);
			out.write("\n");

			if (server) {
				out.flush();
				return null;
			}

			contentToSend = true;
			return recString();
		}
	}

	/**
	 * Reserved for the server (externally): flush the data to the client and
	 * retrieve its answer.
	 * <p>
	 * Also used internally for the client (only do something if there is
	 * contentToSend).
	 * <p>
	 * Will only flush the data if there is contentToSend.
	 * 
	 * @return the answer (which can be NULL)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected String recString() throws IOException {
		synchronized (lock) {
			if (server || contentToSend) {
				if (contentToSend) {
					out.flush();
					contentToSend = false;
				}

				return in.readLine();
			}

			return null;
		}
	}
}