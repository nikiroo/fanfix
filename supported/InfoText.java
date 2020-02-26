package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import be.nikiroo.fanfix.data.MetaData;

/**
 * Support class for <tt>.info</tt> text files ({@link Text} files with a
 * <tt>.info</tt> metadata file next to them).
 * <p>
 * The <tt>.info</tt> file is supposed to be written by this program, or
 * compatible.
 * 
 * @author niki
 */
class InfoText extends Text {
	protected File getInfoFile() {
		return new File(assureNoTxt(getSourceFile()).getPath() + ".info");
	}

	@Override
	protected MetaData getMeta() throws IOException {
		MetaData meta = InfoReader.readMeta(getInfoFile(), true);

		// Some old .info files don't have those now required fields...
		String test = meta.getTitle() == null ? "" : meta.getTitle();
		test += meta.getAuthor() == null ? "" : meta.getAuthor();
		test += meta.getDate() == null ? "" : meta.getDate();
		test += meta.getUrl() == null ? "" : meta.getUrl();
		if (test.isEmpty()) {
			MetaData superMeta = super.getMeta();
			if (meta.getTitle() == null || meta.getTitle().isEmpty()) {
				meta.setTitle(superMeta.getTitle());
			}
			if (meta.getAuthor() == null || meta.getAuthor().isEmpty()) {
				meta.setAuthor(superMeta.getAuthor());
			}
			if (meta.getDate() == null || meta.getDate().isEmpty()) {
				meta.setDate(superMeta.getDate());
			}
			if (meta.getUrl() == null || meta.getUrl().isEmpty()) {
				meta.setUrl(superMeta.getUrl());
			}
		}

		return meta;
	}

	@Override
	protected boolean supports(URL url) {
		return supports(url, true);
	}
}
