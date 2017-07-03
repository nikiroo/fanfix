package be.nikiroo.fanfix.reader;

import java.io.IOException;

import be.nikiroo.fanfix.Instance;

class TuiReader extends BasicReader {
	public void read() throws IOException {
		if (getStory() == null) {
			throw new IOException("No story to read");
		}

		open(getLibrary(), getStory().getMeta().getLuid());
	}

	public void read(int chapter) throws IOException {
		// TODO: show a special page?
		read();
	}

	public void browse(String source) {
		try {
			TuiReaderApplication app = new TuiReaderApplication(getLibrary()
					.getListBySource(source), this);
			new Thread(app).start();
		} catch (Exception e) {
			Instance.syserr(e);
		}
	}
}
