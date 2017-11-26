package be.nikiroo.fanfix.library;

import java.io.IOException;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.ConnectActionServer;
import be.nikiroo.utils.serial.Server;

/**
 * Create a new remote server that will listen for order on the given port.
 * <p>
 * The available commands are:
 * <ul>
 * <li>GET_METADATA *: will get the metadata of all the stories in the library</li>
 * <li>GET_STORY [luid]: will return the given story if it exists (or NULL if
 * not)</li>
 * </ul>
 * 
 * @author niki
 */
public class RemoteLibraryServer extends Server {

	/**
	 * Create a new remote server (will not be active until
	 * {@link RemoteLibraryServer#start()} is called).
	 * 
	 * @param port
	 *            the port to listen on
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public RemoteLibraryServer(int port) throws IOException {
		super(port, true);
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

		// TODO: progress (+send name + %age info back to client)

		if ("GET_METADATA".equals(command)) {
			if (args != null && args.equals("*")) {
				List<MetaData> metas = Instance.getLibrary().getMetas(null);
				return metas.toArray(new MetaData[] {});
			}
		} else if ("GET_STORY".equals(command)) {
			if (args != null) {
				return Instance.getLibrary().getStory(args, null);
			}
		} else if ("GET_COVER".equals(command)) {
			if (args != null) {
				return Instance.getLibrary().getCover(args);
			}
		}

		return null;
	}
}
