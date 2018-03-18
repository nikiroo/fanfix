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
	public URL getCanonicalUrl(URL source) {
		if (source.toString().endsWith(File.separator + "index.html")) {
			try {
				File fakeFile = new File(source.toURI()); // "story/index.html"
				fakeFile = new File(fakeFile.getParent()); // "story"
				fakeFile = new File(fakeFile, fakeFile.getName()); // "story/story"
				return fakeFile.toURI().toURL();
			} catch (Exception e) {
				Instance.getTraceHandler().error(
						new IOException("Cannot find the right URL for "
								+ source, e));
			}
		}

		return source;
	}
}
