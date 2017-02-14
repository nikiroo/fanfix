package be.nikiroo.fanfix.reader;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.Library;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;

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
		OutputType text = OutputType.valueOfNullOkUC(Instance.getConfig()
				.getString(Config.LOCAL_READER_NON_IMAGES_DOCUMENT_TYPE));
		if (text == null) {
			text = OutputType.HTML;
		}

		OutputType images = OutputType.valueOfNullOkUC(Instance.getConfig()
				.getString(Config.LOCAL_READER_IMAGES_DOCUMENT_TYPE));
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

	// return new luid
	public String imprt(String luid) {
		try {
			Story story = Instance.getLibrary().getStory(luid);
			story = lib.save(story);
			return story.getMeta().getLuid();
		} catch (IOException e) {
			Instance.syserr(new IOException(
					"Cannot import story from library to LocalReader library: "
							+ luid, e));
		}

		return null;
	}

	public File getTarget(String luid) {
		MetaData meta = lib.getInfo(luid);
		File file = lib.getFile(luid);
		if (file == null) {
			luid = imprt(luid);
			file = lib.getFile(luid);
			meta = lib.getInfo(luid);
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
