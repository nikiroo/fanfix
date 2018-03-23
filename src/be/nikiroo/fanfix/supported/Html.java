package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import be.nikiroo.fanfix.Instance;

/**
 * Support class for HTML files created with this program (as we need some
 * metadata available in those we create).
 * 
 * @author niki
 */
class Html extends InfoText {
	@Override
	public String getSourceName() {
		return "html";
	}

	@Override
	protected boolean supports(URL url) {
		try {
			File file = new File(url.toURI());
			if (file.getName().equals("index.html")) {
				file = file.getParentFile();
			}

			file = new File(file, file.getName());

			return super.supports(file.toURI().toURL());
		} catch (URISyntaxException e) {
		} catch (MalformedURLException e) {
		}

		return false;
	}

	@Override
	public URL getCanonicalUrl(URL source) {

		try {
			File fakeFile = new File(source.toURI());
			if (fakeFile.getName().equals("index.html")) { // "story/index.html"
				fakeFile = new File(fakeFile.getParent()); // -> "story/"
			}

			if (fakeFile.isDirectory()) { // "story/"
				fakeFile = new File(fakeFile, fakeFile.getName() + ".txt"); // "story/story.txt"
			}

			return fakeFile.toURI().toURL();
		} catch (Exception e) {
			Instance.getTraceHandler().error(
					new IOException("Cannot find the right URL for " + source,
							e));
		}

		return source;
	}
}
