package be.nikiroo.fanfix;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.ConnectActionClient;

public class RemoteLibrary extends Library {
	private String host;
	private int port;

	private Library lib;

	public RemoteLibrary(String host, int port) throws IOException {
		this.host = host;
		this.port = port;

		this.localSpeed = false;
		this.baseDir = Instance.getRemoteDir(host);
		this.baseDir.mkdirs();

		this.lib = new Library(baseDir, OutputType.INFO_TEXT, OutputType.CBZ);
	}

	@Override
	public synchronized Story save(Story story, String luid, Progress pg)
			throws IOException {
		throw new java.lang.InternalError(
				"No write support allowed on remote Libraries");
	}

	@Override
	public synchronized boolean delete(String luid) {
		throw new java.lang.InternalError(
				"No write support allowed on remote Libraries");
	}

	@Override
	public synchronized boolean changeType(String luid, String newType) {
		throw new java.lang.InternalError(
				"No write support allowed on remote Libraries");
	}

	@Override
	protected synchronized Map<MetaData, File> getStories(Progress pg) {
		// TODO: progress
		if (stories.isEmpty()) {
			try {
				new ConnectActionClient(host, port, true, null) {
					public void action(Version serverVersion) throws Exception {
						try {
							Object rep = send("GET_METADATA *");
							for (MetaData meta : (MetaData[]) rep) {
								stories.put(meta, null);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}.connect();
			} catch (IOException e) {
				Instance.syserr(e);
			}
		}

		return stories;
	}

	@Override
	public synchronized File getFile(final String luid) {
		File file = lib.getFile(luid);
		if (file == null) {
			final File[] tmp = new File[1];
			try {
				new ConnectActionClient(host, port, true, null) {
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
			stories.put(meta, file);
		}

		return file;
	}
}
