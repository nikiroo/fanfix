package be.nikiroo.fanfix.reader;

import java.io.IOException;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;

class TuiReader extends BasicReader {
	@Override
	public void read() throws IOException {
		if (getStory() == null) {
			throw new IOException("No story to read");
		}

		open(getStory().getMeta().getLuid());
	}

	@Override
	public void read(int chapter) throws IOException {
		// TODO: show a special page?
		read();
	}

	@Override
	public void start(String type) {
		List<MetaData> stories = Instance.getLibrary().getListByType(type);
		try {
			TuiReaderApplication app = new TuiReaderApplication(stories, this);
			new Thread(app).start();
		} catch (Exception e) {
			Instance.syserr(e);
		}
	}
}
