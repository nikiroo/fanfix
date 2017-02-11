package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;

/**
 * Support class for CBZ files (works better with CBZ created with this program,
 * as they have some metadata available).
 * 
 * @author niki
 */
class Cbz extends Epub {
	@Override
	protected boolean supports(URL url) {
		return url.toString().toLowerCase().endsWith(".cbz");
	}

	@Override
	public String getSourceName() {
		return "cbz";
	}

	@Override
	protected String getDataPrefix() {
		return "";
	}

	@Override
	protected boolean requireInfo() {
		return false;
	}
	
	@Override
	public boolean isImageDocument(URL source, InputStream in)
			throws IOException {
		return true;
	}

	@Override
	protected boolean getCover() {
		return false;
	}

	@Override
	public Story process(URL url) throws IOException {
		Story story = processMeta(url, false, true);
		story.setChapters(new ArrayList<Chapter>());
		Chapter chap = new Chapter(1, null);
		story.getChapters().add(chap);

		ZipInputStream zipIn = new ZipInputStream(getInput());

		for (ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn
				.getNextEntry()) {
			if (!entry.isDirectory()
					&& entry.getName().startsWith(getDataPrefix())) {
				String entryLName = entry.getName().toLowerCase();
				boolean imageEntry = false;
				for (String ext : getImageExt(false)) {
					if (entryLName.endsWith(ext)) {
						imageEntry = true;
					}
				}

				if (imageEntry) {
					try {
						// we assume that we can get the UUID without a stream
						String uuid = getUuid(url, null) + "_"
								+ entry.getName();

						Instance.getCache().addToCache(zipIn, uuid);
						chap.getParagraphs().add(
								new Paragraph(new File(uuid).toURI().toURL()));
					} catch (Exception e) {
						Instance.syserr(e);
					}
				}
			}
		}

		return story;
	}
}
