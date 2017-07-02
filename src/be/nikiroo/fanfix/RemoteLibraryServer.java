package be.nikiroo.fanfix;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.ConnectActionServer;
import be.nikiroo.utils.serial.Server;

public class RemoteLibraryServer extends Server {

	public RemoteLibraryServer(int port) throws IOException {
		super(Version.getCurrentVersion(), port, true);
	}

	@Override
	protected Object onRequest(ConnectActionServer action,
			Version clientVersion, Object data) throws Exception {
		String command = null;
		String args = null;
		if (data instanceof String) {
			command = (String) data;
			int pos = command.indexOf(" ");
			if (pos >= 0) {
				args = command.substring(pos + 1);
				command = command.substring(0, pos);
			}
		}

		System.out.println(String.format("COMMAND: [%s], ARGS: [%s]", command,
				args));

		if (command != null) {
			if (command.equals("GET_METADATA")) {
				if (args != null && args.equals("*")) {
					List<MetaData> metas = Instance.getLibrary().getMetas(null);
					return metas.toArray(new MetaData[] {});
				}
			} else if (command.equals("GET_STORY")) {
				if (args != null) {
					return Instance.getLibrary().getStory(args, null);
				}
			}
		}

		return null;
	}
}
