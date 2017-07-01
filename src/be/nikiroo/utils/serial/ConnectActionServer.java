package be.nikiroo.utils.serial;

import java.net.Socket;

import be.nikiroo.utils.Version;

public class ConnectActionServer extends ConnectAction {
	protected ConnectActionServer(Socket s) {
		super(s, true, Version.getCurrentVersion());
	}

	protected ConnectActionServer(Socket s, Version version) {
		super(s, true, version);
	}

	@Override
	public void action(Version serverVersion) throws Exception {
	}
}