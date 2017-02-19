package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Progress;

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
	protected boolean getCover() {
		return false;
	}

	@Override
	protected void preprocess(URL source, InputStream in) throws IOException {
		super.preprocess(source, in);
		meta.setImageDocument(true);
	}

	@Override
	public Story process(URL url, Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		} else {
			pg.setMinMax(0, 100);
		}

		Story story = processMeta(url, false, true);
		story.setChapters(new ArrayList<Chapter>());
		Chapter chap = new Chapter(1, null);
		story.getChapters().add(chap);

		ZipInputStream zipIn = new ZipInputStream(getInput());

		pg.setProgress(10);
		List<String> images = new ArrayList<String>();
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
					String uuid = meta.getUuid() + "_" + entry.getName();
					images.add(uuid);
					try {
						Instance.getCache().addToCache(zipIn, uuid);
					} catch (Exception e) {
						Instance.syserr(e);
					}
				}
			}
		}

		pg.setProgress(80);

		// ZIP order is not sure
		Collections.sort(images);
		pg.setProgress(90);

		for (String uuid : images) {
			try {
				chap.getParagraphs().add(
						new Paragraph(new File(uuid).toURI().toURL()));
			} catch (Exception e) {
				Instance.syserr(e);
			}
		}

		pg.setProgress(100);
		return story;
	}
}
