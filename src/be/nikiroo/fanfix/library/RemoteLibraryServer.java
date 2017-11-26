package be.nikiroo.fanfix.library;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.ConnectActionServer;
import be.nikiroo.utils.serial.Server;

/**
 * Create a new remote server that will listen for order on the given port.
 * <p>
 * The available commands are given as String arrays (first item is the command,
 * the rest are the arguments):
 * <ul>
 * <li>GET_METADATA *: will return the metadata of all the stories in the
 * library</li>
 * <li>GET_STORY [luid]: will return the given story if it exists (or NULL if
 * not)</li>
 * <li>SAVE_STORY [story] [luid]: save the story with the given LUID</li>
 * <li>DELETE_STORY [luid]: delete the story of LUID luid</li>
 * <li>GET_COVER [luid]: return the cover of the story</li>
 * <li>GET_SOURCE_COVER [source]: return the cover for this source</li>
 * <li>SET_SOURCE_COVER [source], [luid]: set the default cover for the given
 * source to the cover of the story denoted by luid</li>
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

		String command = "";
		Object[] args = new Object[0];
		if (data instanceof Object[]) {
			args = (Object[]) data;
			if (args.length > 0) {
				command = "" + args[0];
			}
		}

		System.out.print("COMMAND: ");
		for (Object arg : args) {
			System.out.print(arg + " ");
		}
		System.out.println("");

		// TODO: progress (+send name + %age info back to client)

		if ("GET_METADATA".equals(command)) {
			if (args[1].equals("*")) {
				List<MetaData> metas = Instance.getLibrary().getMetas(null);
				return metas.toArray(new MetaData[] {});
			}
			throw new InvalidParameterException(
					"only * is valid here, but you passed: " + args[1]);
		} else if ("GET_STORY".equals(command)) {
			return Instance.getLibrary().getStory("" + args[1], null);
		} else if ("SAVE_STORY".equals(command)) {
			Instance.getLibrary().save((Story) args[1], "" + args[2], null);
		} else if ("DELETE_STORY".equals(command)) {
			Instance.getLibrary().delete("" + args[1]);
		} else if ("GET_COVER".equals(command)) {
			return Instance.getLibrary().getCover("" + args[1]);
		} else if ("GET_SOURCE_COVER".equals(command)) {
			return Instance.getLibrary().getSourceCover("" + args[1]);
		} else if ("SET_SOURCE_COVER".equals(command)) {
			Instance.getLibrary().setSourceCover("" + args[1], "" + args[2]);
		}

		return null;
	}
}
