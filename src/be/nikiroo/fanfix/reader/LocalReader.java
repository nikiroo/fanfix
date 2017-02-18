package be.nikiroo.fanfix.reader;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.Library;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.utils.ui.Progress;

class LocalReader extends BasicReader {
	private Library lib;

	public LocalReader() throws IOException {
		File dir = Instance.getReaderDir();
		dir.mkdirs();
		if (!dir.exists()) {
			throw new IOException(
					"Cannote create cache directory for local reader: " + dir);
		}

		// TODO: can throw an exception, manage that (convert to IOEx ?)
		OutputType text = OutputType.valueOfNullOkUC(Instance.getUiConfig()
				.getString(UiConfig.LOCAL_READER_NON_IMAGES_DOCUMENT_TYPE));
		if (text == null) {
			text = OutputType.HTML;
		}

		OutputType images = OutputType.valueOfNullOkUC(Instance.getUiConfig()
				.getString(UiConfig.LOCAL_READER_IMAGES_DOCUMENT_TYPE));
		if (images == null) {
			images = OutputType.CBZ;
		}
		//

		lib = new Library(dir, text, images);
	}

	@Override
	public void read() throws IOException {
	}

	@Override
	public void read(int chapter) {
	}

	/**
	 * Import the story into the local reader library, and keep the same LUID.
	 * 
	 * @param luid
	 *            the Library UID
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void imprt(String luid, Progress pg) throws IOException {
		try {
			Story story = Instance.getLibrary().getStory(luid, pg);
			if (story != null) {
				story = lib.save(story, luid);
			} else {
				throw new IOException("Cannot find story in Library: " + luid);
			}
		} catch (IOException e) {
			throw new IOException(
					"Cannot import story from library to LocalReader library: "
							+ luid, e);
		}
	}

	/**
	 * Get the target file related to this {@link Story}.
	 * 
	 * @param luid
	 *            the LUID of the {@link Story}
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the target file
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public File getTarget(String luid, Progress pg) throws IOException {
		File file = lib.getFile(luid);
		if (file == null) {
			imprt(luid, pg);
			file = lib.getFile(luid);
		}

		return file;
	}

	@Override
	public void start(String type) {
		final String typeFinal = type;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new LocalReaderFrame(LocalReader.this, typeFinal)
						.setVisible(true);
			}
		});
	}
}
