package be.nikiroo.utils.serial;

import java.io.IOException;
import java.net.Socket;

import be.nikiroo.utils.Version;

public class ConnectActionClient extends ConnectAction {
	protected ConnectActionClient(Socket s) {
		super(s, false, Version.getCurrentVersion());
	}

	protected ConnectActionClient(Socket s, Version version) {
		super(s, false, version);
	}

	protected ConnectActionClient(String host, int port, boolean ssl)
			throws IOException {
		super(Server.createSocket(host, port, ssl), false, Version
				.getCurrentVersion());
	}

	protected ConnectActionClient(String host, int port, boolean ssl,
			Version version) throws IOException {
		super(Server.createSocket(host, port, ssl), false, version);
	}

	@Override
	public void action(Version serverVersion) throws Exception {
	}
}