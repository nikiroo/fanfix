package be.nikiroo.utils.main;

import be.nikiroo.utils.TraceHandler;
import be.nikiroo.utils.serial.server.ServerBridge;

/**
 * Serialiser bridge (starts a {@link ServerBridge} and can thus intercept
 * communication between a client and a server).
 * 
 * @author niki
 */
public class bridge {
	/**
	 * Start a bridge between 2 servers.
	 * 
	 * @param args
	 *            an array containing:
	 *            <ul>
	 *            <li>The bridge name</li>
	 *            <li>The bridge port</li>
	 *            <li>TRUE for an SSL bridge, FALSE for plain text</li>
	 *            <li>The forward server host</li>
	 *            <li>The forward server port</li>
	 *            <li>TRUE for an SSL forward server, FALSE for plain text</li>
	 *            <li>(optional) a trace level</li>
	 *            <li>(optional) a truncate size for data</li>
	 *            </ul>
	 */
	public static void main(String[] args) {
		final TraceHandler tracer = new TraceHandler(true, false, 0);
		try {
			if (args.length < 6) {
				tracer.error("Invalid syntax.\n"
						+ "Syntax: [name] [port] [ssl] [fhost] [fport] [fssl] ([trace level]) ([max])\n"
						+ "\tname: the bridge name\n"
						+ "\tport: the bridge port\n"
						+ "\tssl: TRUE for an SSL bridge, FALSE for plain text\n"
						+ "\tfhost: the forward server host\n"
						+ "\tfport: the forward server port\n"
						+ "\tfssl: TRUE for an SSL forward server, FALSE for plain text\n"
						+ "\ttrace level: the optional trace level (default is 1)\n"
						+ "\tmax: the maximum size after which to truncate data\n");
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
			int maxPrintSize = 0;
			if (args.length > 7) {
				maxPrintSize = Integer.parseInt(args[i++]);
			}

			ServerBridge bridge = new ServerBridge(name, port, ssl, fhost,
					fport, fssl);
			bridge.setTraceHandler(new TraceHandler(true, true, traceLevel,
					maxPrintSize));
			bridge.run();
		} catch (Exception e) {
			tracer.error(e);
		}
	}
}
