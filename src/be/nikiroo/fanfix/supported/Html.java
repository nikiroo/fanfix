package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.MarkableFileInputStream;

/**
 * Support class for HTML files created with this program (as we need some
 * metadata available in those we create).
 * 
 * @author niki
 */
class Html extends InfoText {
	private URL fakeSource;

	@Override
	public String getSourceName() {
		return "html";
	}

	@Override
	protected boolean supports(URL url) {
		if (url.getPath().toLowerCase()
				.endsWith(File.separatorChar + "index.html")) {
			try {
				File file = new File(url.toURI()).getParentFile();
				return super.supports(file.toURI().toURL());
			} catch (URISyntaxException e) {
			} catch (MalformedURLException e) {
			}
		}

		return false;
	}

	@Override
	protected MetaData getMeta(URL source, InputStream in) throws IOException {
		return super.getMeta(fakeSource, in);
	}

	@Override
	protected String getDesc(URL source, InputStream in) throws IOException {
		return super.getDesc(fakeSource, in);
	}

	@Override
	protected List<Entry<String, URL>> getChapters(URL source, InputStream in)
			throws IOException {
		return super.getChapters(fakeSource, in);
	}

	@Override
	protected String getChapterContent(URL source, InputStream in, int number)
			throws IOException {
		return super.getChapterContent(fakeSource, in, number);
	}

	@Override
	protected InputStream openInput(URL source) throws IOException {
		try {
			File fakeFile = new File(source.toURI()); // "story/index.html"
			fakeFile = new File(fakeFile.getParent()); // "story"
			fakeFile = new File(fakeFile, fakeFile.getName()); // "story/story"
			fakeSource = fakeFile.toURI().toURL();
			return new MarkableFileInputStream(new FileInputStream(fakeFile));
		} catch (URISyntaxException e) {
			throw new IOException(
					"file not supported (maybe not created with this program or corrupt)",
					e);
		} catch (MalformedURLException e) {
			throw new IOException("file not supported (bad URL)", e);
		}
	}
}
