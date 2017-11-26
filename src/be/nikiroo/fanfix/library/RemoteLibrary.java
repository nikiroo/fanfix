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
import be.nikiroo.utils.serial.ConnectActionClient;

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
	private File baseDir;

	private LocalLibrary lib;
	private List<MetaData> metas;

	/**
	 * Create a {@link RemoteLibrary} linked to the given server.
	 * 
	 * @param host
	 *            the host to contact or NULL for localhost
	 * @param port
	 *            the port to contact it on
	 */
	public RemoteLibrary(String host, int port) {
		this.host = host;
		this.port = port;

		this.baseDir = Instance.getRemoteDir(host);
		this.baseDir.mkdirs();

		this.lib = new LocalLibrary(baseDir);
	}

	@Override
	public String getLibraryName() {
		return host + ":" + port;
	}

	@Override
	protected List<MetaData> getMetas(Progress pg) {
		// TODO: progress

		if (metas == null) {
			metas = new ArrayList<MetaData>();

			try {
				new ConnectActionClient(host, port, true) {
					@Override
					public void action(Version serverVersion) throws Exception {
						try {
							Object rep = send("GET_METADATA *");
							for (MetaData meta : (MetaData[]) rep) {
								metas.add(meta);
							}
						} catch (Exception e) {
							Instance.syserr(e);
						}
					}
				}.connect();
			} catch (IOException e) {
				Instance.syserr(e);
			}

			List<String> test = new ArrayList<String>();
			for (MetaData meta : metas) {
				if (test.contains(meta.getLuid())) {
					throw new RuntimeException("wwops");
				}
				test.add(meta.getLuid());
			}
		}

		return metas;
	}

	@Override
	public synchronized File getFile(final String luid) {
		File file = lib.getFile(luid);
		if (file == null) {
			final File[] tmp = new File[1];
			try {
				new ConnectActionClient(host, port, true) {
					@Override
					public void action(Version serverVersion) throws Exception {
						try {
							Object rep = send("GET_STORY " + luid);
							Story story = (Story) rep;
							if (story != null) {
								lib.save(story, luid, null);
								tmp[0] = lib.getFile(luid);
							}
						} catch (Exception e) {
							Instance.syserr(e);
						}
					}
				}.connect();
			} catch (IOException e) {
				Instance.syserr(e);
			}

			file = tmp[0];
		}

		if (file != null) {
			MetaData meta = getInfo(luid);
			metas.add(meta);
		}

		return file;
	}

	@Override
	public BufferedImage getCover(final String luid) {
		// Retrieve it from the cache if possible:
		if (lib.getInfo(luid) != null) {
			return lib.getCover(luid);
		}

		final BufferedImage[] result = new BufferedImage[1];
		try {
			new ConnectActionClient(host, port, true) {
				@Override
				public void action(Version serverVersion) throws Exception {
					try {
						Object rep = send("GET_COVER " + luid);
						result[0] = (BufferedImage) rep;
					} catch (Exception e) {
						Instance.syserr(e);
					}
				}
			}.connect();
		} catch (IOException e) {
			Instance.syserr(e);
		}

		return result[0];
	}

	@Override
	protected void clearCache() {
		metas = null;
		lib.clearCache();
	}

	@Override
	public synchronized Story save(Story story, String luid, Progress pg)
			throws IOException {
		throw new java.lang.InternalError(
				"No write support allowed on remote Libraries");
	}

	@Override
	public synchronized void delete(String luid) throws IOException {
		throw new java.lang.InternalError(
				"No write support allowed on remote Libraries");
	}

	@Override
	public void setSourceCover(String source, String luid) {
		throw new java.lang.InternalError(
				"No write support allowed on remote Libraries");
	}

	// All the following methods are only used by Save and Delete in
	// BasicLibrary:

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
	 * Return the backing local library.
	 * 
	 * @return the library
	 */
	LocalLibrary getLocalLibrary() {
		return lib;
	}
}
