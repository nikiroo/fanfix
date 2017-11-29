package be.nikiroo.utils.serial.server;

import java.io.IOException;
import java.net.Socket;

import be.nikiroo.utils.TraceHandler;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.Importer;

/**
 * This class implements a simple server that can bridge two other
 * {@link Server}s.
 * <p>
 * It can, of course, inspect the data that goes through it (by default, it
 * prints traces of the data).
 * <p>
 * Note: this {@link ServerBridge} has to be discarded after use (cannot be
 * started twice).
 * 
 * @author niki
 */
public class ServerBridge extends Server {
	private final String forwardToHost;
	private final int forwardToPort;
	private final boolean forwardToSsl;

	/**
	 * Create a new server that will start listening on the network when
	 * {@link ServerBridge#start()} is called.
	 * 
	 * @param port
	 *            the port to listen on, or 0 to assign any unallocated port
	 *            found (which can later on be queried via
	 *            {@link ServerBridge#getPort()}
	 * @param ssl
	 *            use a SSL connection (or not)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public ServerBridge(int port, boolean ssl, String forwardToHost,
			int forwardToPort, boolean forwardToSsl) throws IOException {
		super(port, ssl);
		this.forwardToHost = forwardToHost;
		this.forwardToPort = forwardToPort;
		this.forwardToSsl = forwardToSsl;
	}

	/**
	 * Create a new server that will start listening on the network when
	 * {@link ServerBridge#start()} is called.
	 * 
	 * @param name
	 *            the server name (only used for debug info and traces)
	 * @param port
	 *            the port to listen on
	 * @param ssl
	 *            use a SSL connection (or not)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public ServerBridge(String name, int port, boolean ssl,
			String forwardToHost, int forwardToPort, boolean forwardToSsl)
			throws IOException {
		super(name, port, ssl);
		this.forwardToHost = forwardToHost;
		this.forwardToPort = forwardToPort;
		this.forwardToSsl = forwardToSsl;
	}

	/**
	 * The traces handler for this {@link Server}.
	 * <p>
	 * <ul>
	 * <li>At level 1, it will only print basic IN/OUT messages with length.</li>
	 * <li>At level 2, it will also print the data as a String.</li>
	 * <li>At level 3, it will try to interpret it (SLOW) and print the object
	 * type if possible.</li>
	 * <li>At level 4, it will try to print the {@link Object#toString()} value.
	 * </li>
	 * </ul>
	 * 
	 * @param tracer
	 *            the new traces handler
	 */
	@Override
	public void setTraceHandler(TraceHandler tracer) {
		super.setTraceHandler(tracer);
	}

	@Override
	protected ConnectActionServer createConnectActionServer(Socket s) {
		return new ConnectActionServerString(s) {
			@Override
			public void action(final Version clientVersion) throws Exception {
				onClientContact(clientVersion);
				final ConnectActionServerString bridge = this;

				new ConnectActionClientString(forwardToHost, forwardToPort,
						forwardToSsl, clientVersion) {
					@Override
					public void action(final Version serverVersion)
							throws Exception {
						onServerContact(serverVersion);

						for (String fromClient = bridge.rec(); fromClient != null; fromClient = bridge
								.rec()) {
							onRec(clientVersion, fromClient);
							String fromServer = send(fromClient);
							onSend(serverVersion, fromServer);
							bridge.send(fromServer);
						}
					}
				}.connect();
			}
		};
	}

	/**
	 * This is the method that is called each time a client contact us.
	 * 
	 * @param clientVersion
	 *            the client version
	 * @param data
	 *            the data sent by the client
	 */
	protected void onClientContact(Version clientVersion) {
		getTraceHandler().trace("CLIENT " + clientVersion);
	}

	/**
	 * This is the method that is called each time a client contact us.
	 * 
	 * @param serverVersion
	 *            the server version
	 * @param data
	 *            the data sent by the client
	 */
	protected void onServerContact(Version serverVersion) {
		getTraceHandler().trace("SERVER " + serverVersion);
	}

	/**
	 * This is the method that is called each time a client contact us.
	 * 
	 * @param clientVersion
	 *            the client version
	 * @param data
	 *            the data sent by the client
	 */
	protected void onRec(Version clientVersion, String data) {
		trace("<<< CLIENT (" + clientVersion + ")", data);
	}

	/**
	 * This is the method that is called each time the forwarded server contact
	 * us.
	 * 
	 * @param serverVersion
	 *            the client version
	 * @param data
	 *            the data sent by the client
	 */
	protected void onSend(Version serverVersion, String data) {
		trace(">>> SERVER (" + serverVersion + ")", data);
	}

	/**
	 * Trace the data with the given prefix.
	 * 
	 * @param prefix
	 *            the prefix (client, server, version...)
	 * @param data
	 *            the data to trace
	 */
	private void trace(String prefix, String data) {
		getTraceHandler().trace(prefix + ": " + data.length() + " characters",
				1);

		if (getTraceHandler().getTraceLevel() >= 2) {
			try {
				Object obj = new Importer().read(data).getValue();
				if (obj == null) {
					getTraceHandler().trace("NULL", 3);
					getTraceHandler().trace("NULL", 4);
				} else {
					getTraceHandler().trace("(" + obj.getClass() + ")", 3);
					getTraceHandler().trace("" + obj.toString(), 4);
				}
			} catch (Exception e) {
				getTraceHandler().trace("(not an object)", 3);
				getTraceHandler().trace(data, 4);
			}

			getTraceHandler().trace("", 4);
		}
	}

	/**
	 * Start a bridge between 2 servers.
	 * 
	 * @param args
	 *            an array containing:
	 *            <ul>
	 *            <li>The bridge name</li>
	 *            <li>The bridge port</li>
	 *            <li>TRUE for a ssl bridge, FALSE for plain text</li>
	 *            <li>The forward server host</li>
	 *            <li>The forward server port</li>
	 *            <li>TRUE for a ssl forward server, FALSE for plain text</li>
	 *            <li>(optional) a trace level</li>
	 *            </ul>
	 */
	public static void main(String[] args) {
		final TraceHandler tracer = new TraceHandler(true, false, 0);
		try {
			if (args.length < 6) {
				tracer.error("Invalid syntax.\n"
						+ "Syntax: [name] [port] [ssl] [fhost] [fport] [fssl] ([trace level])\n"
						+ "\tname: the bridge name\n"
						+ "\tport: the bridge port\n"
						+ "\tssl: TRUE for an SSL bridge, FALSE for plain text\n"
						+ "\tfhost: the forward server host\n"
						+ "\tfport: the forward server port\n"
						+ "\tfssl: TRUE for an SSL forward server, FALSE for plain text\n"
						+ "\ttrace level: the optional trace level (default is 1)\n");
				return;
			}

			int i = 0;
			String name = args[i++];
			int port = Integer.parseInt(args[i++]);
			boolean ssl = Boolean.parseBoolean(args[i++]);
			String fhost = args[i++];
			int fport = Integer.parseInt(args[i++]);
			boolean fssl = Boolean.parseBoolean(args[i++]);

			int traceLevel = 1;
			if (args.length > 6) {
				traceLevel = Integer.parseInt(args[i++]);
			}

			ServerBridge bridge = new ServerBridge(name, port, ssl, fhost,
					fport, fssl);
			bridge.setTraceHandler(new TraceHandler(true, true, traceLevel));
			bridge.run();
		} catch (Exception e) {
			tracer.error(e);
		}
	}
}
