package be.nikiroo.fanfix.library;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.server.ConnectActionClientObject;

/**
 * This {@link BasicLibrary} will access a remote server to list the available
 * stories, and download the ones you try to load to the local directory
 * specified in the configuration.
 * 
 * @author niki
 */
public class RemoteLibrary extends BasicLibrary {
	private String host;
	private int port;
	private final String key;

	/**
	 * Create a {@link RemoteLibrary} linked to the given server.
	 * 
	 * @param key
	 *            the key that will allow us to exchange information with the
	 *            server
	 * @param host
	 *            the host to contact or NULL for localhost
	 * @param port
	 *            the port to contact it on
	 */
	public RemoteLibrary(String key, String host, int port) {
		this.key = key;
		this.host = host;
		this.port = port;
	}

	@Override
	public String getLibraryName() {
		return host + ":" + port;
	}

	@Override
	protected List<MetaData> getMetas(Progress pg) {
		// TODO: progress
		final List<MetaData> metas = new ArrayList<MetaData>();
		MetaData[] fromNetwork = this.getRemoteObject( //
				new Object[] { key, "GET_METADATA", "*" });

		if (fromNetwork != null) {
			for (MetaData meta : fromNetwork) {
				metas.add(meta);
			}
		}

		return metas;
	}

	@Override
	public BufferedImage getCover(final String luid) {
		return this.getRemoteObject( //
				new Object[] { key, "GET_COVER", luid });
	}

	@Override
	public BufferedImage getSourceCover(final String source) {
		return this.getRemoteObject( //
				new Object[] { key, "GET_SOURCE_COVER", source });
	}

	@Override
	public synchronized Story getStory(final String luid, Progress pg) {
		return this.getRemoteStory( //
				new Object[] { key, "GET_STORY", luid });
	}

	@Override
	protected void clearCache() {
	}

	@Override
	public synchronized Story save(Story story, String luid, Progress pg)
			throws IOException {
		getRemoteObject(new Object[] { key, "SAVE_STORY", story, luid });

		// because the meta changed:
		clearCache();
		story.setMeta(getInfo(luid));

		return story;
	}

	@Override
	public synchronized void delete(String luid) throws IOException {
		getRemoteObject(new Object[] { key, "DELETE_STORY", luid });
	}

	@Override
	public void setSourceCover(String source, String luid) {
		this.<BufferedImage> getRemoteObject( //
		new Object[] { key, "SET_SOURCE_COVER", source, luid });
	}

	@Override
	public synchronized File getFile(final String luid, Progress pg) {
		throw new java.lang.InternalError(
				"Operation not supportorted on remote Libraries");
	}

	// The following methods are only used by Save and Delete in BasicLibrary:

	@Override
	protected int getNextId() {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected void doDelete(String luid) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected Story doSave(Story story, Progress pg) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}

	/**
	 * Return an object from the server.
	 * 
	 * @param <T>
	 *            the expected type of object
	 * @param command
	 *            the command to send (can contain at most ONE {@link Story})
	 * 
	 * @return the object or NULL
	 */
	private <T> T getRemoteObject(final Object[] command) {
		return getRemoteObjectOrStory(command, false);
	}

	/**
	 * Return an object from the server.
	 * 
	 * @param command
	 *            the command to send (can contain at most ONE {@link Story})
	 * 
	 * @return the object or NULL
	 */
	private Story getRemoteStory(final Object[] command) {
		return getRemoteObjectOrStory(command, true);
	}

	/**
	 * Return an object from the server.
	 * 
	 * @param <T>
	 *            the expected type of object
	 * @param command
	 *            the command to send (can contain at most ONE {@link Story})
	 * 
	 * @return the object or NULL
	 */
	@SuppressWarnings("unchecked")
	private <T> T getRemoteObjectOrStory(final Object[] command,
			final boolean getStory) {
		final Object[] result = new Object[1];
		try {
			new ConnectActionClientObject(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					try {
						Story story = null;
						for (int i = 0; i < command.length; i++) {
							if (command[i] instanceof Story) {
								story = (Story) command[i];
								command[i] = null;
							}
						}

						Object rep = send(command);

						if (story != null) {
							RemoteLibraryServer.sendStory(story, this);
						}

						if (getStory) {
							rep = RemoteLibraryServer.recStory(this);
						}

						result[0] = rep;
					} catch (Exception e) {
						Instance.getTraceHandler().error(e);
					}
				}
			}.connect();
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}

		try {
			return (T) result[0];
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
			return null;
		}
	}
}
