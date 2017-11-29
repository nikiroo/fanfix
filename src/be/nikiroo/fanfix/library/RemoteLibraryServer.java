package be.nikiroo.fanfix.library;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.server.ConnectActionServerObject;
import be.nikiroo.utils.serial.server.ServerObject;

/**
 * Create a new remote server that will listen for order on the given port.
 * <p>
 * The available commands are given as arrays of objects (first item is the key,
 * second is the command, the rest are the arguments).
 * <p>
 * The key is always a String, the commands are also Strings; the parameters
 * vary depending upon the command.
 * <ul>
 * <li>[key] GET_METADATA *: will return the metadata of all the stories in the
 * library</li>
 * <li>[key] GET_STORY [luid]: will return the given story if it exists (or NULL
 * if not)</li>
 * <li>[key] SAVE_STORY [story] [luid]: save the story with the given LUID</li>
 * <li>[key] DELETE_STORY [luid]: delete the story of LUID luid</li>
 * <li>[key] GET_COVER [luid]: return the cover of the story</li>
 * <li>[key] GET_SOURCE_COVER [source]: return the cover for this source</li>
 * <li>[key] SET_SOURCE_COVER [source], [luid]: set the default cover for the
 * given source to the cover of the story denoted by luid</li>
 * <li>[key] EXIT: stop the server</li>
 * </ul>
 * 
 * @author niki
 */
public class RemoteLibraryServer extends ServerObject {
	private final String key;

	/**
	 * Create a new remote server (will not be active until
	 * {@link RemoteLibraryServer#start()} is called).
	 * 
	 * @param key
	 *            the key that will restrict access to this server
	 * @param port
	 *            the port to listen on
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public RemoteLibraryServer(String key, int port) throws IOException {
		super(port, true);
		this.key = key;
	}

	@Override
	protected Object onRequest(ConnectActionServerObject action,
			Version clientVersion, Object data) throws Exception {
		String key = "";
		String command = "";
		Object[] args = new Object[0];
		if (data instanceof Object[]) {
			Object[] dataArray = (Object[]) data;
			if (dataArray.length >= 2) {
				args = new Object[dataArray.length - 2];
				for (int i = 2; i < dataArray.length; i++) {
					args[i - 2] = dataArray[i];
				}

				key = "" + dataArray[0];
				command = "" + dataArray[1];
			}
		}

		System.out.print("[" + command + "] ");
		for (Object arg : args) {
			System.out.print(arg + " ");
		}
		System.out.println("");

		if (!key.equals(this.key)) {
			System.out.println("Key rejected.");
			throw new SecurityException("Invalid key");
		}

		// TODO: progress (+send name + %age info back to client)

		if ("GET_METADATA".equals(command)) {
			if (args[0].equals("*")) {
				List<MetaData> metas = Instance.getLibrary().getMetas(null);
				return metas.toArray(new MetaData[] {});
			}
			throw new InvalidParameterException(
					"only * is valid here, but you passed: " + args[0]);
		} else if ("GET_STORY".equals(command)) {
			return Instance.getLibrary().getStory("" + args[0], null);
		} else if ("SAVE_STORY".equals(command)) {
			Instance.getLibrary().save((Story) args[0], "" + args[1], null);
		} else if ("DELETE_STORY".equals(command)) {
			Instance.getLibrary().delete("" + args[0]);
		} else if ("GET_COVER".equals(command)) {
			return Instance.getLibrary().getCover("" + args[0]);
		} else if ("GET_SOURCE_COVER".equals(command)) {
			return Instance.getLibrary().getSourceCover("" + args[0]);
		} else if ("SET_SOURCE_COVER".equals(command)) {
			Instance.getLibrary().setSourceCover("" + args[0], "" + args[1]);
		} else if ("EXIT".equals(command)) {
			stop(0, false);
		}

		return null;
	}
}
