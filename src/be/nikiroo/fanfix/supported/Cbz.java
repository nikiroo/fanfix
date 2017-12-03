package be.nikiroo.fanfix.supported;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Image;
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

		Progress pgMeta = new Progress();
		pg.addProgress(pgMeta, 10);
		Story story = processMeta(url, false, true, pgMeta);
		pgMeta.done(); // 10%

		story.setChapters(new ArrayList<Chapter>());
		Chapter chap = new Chapter(1, null);
		story.getChapters().add(chap);

		ZipInputStream zipIn = new ZipInputStream(getInput());

		Map<String, Image> images = new HashMap<String, Image>();
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
					try {
						images.put(uuid, new Image(zipIn));
					} catch (Exception e) {
						Instance.getTraceHandler().error(e);
					}

					if (pg.getProgress() < 85) {
						pg.add(1);
					}
				}
			}
		}

		pg.setProgress(85);

		// ZIP order is not correct for us
		List<String> imagesList = new ArrayList<String>(images.keySet());
		Collections.sort(imagesList);

		pg.setProgress(90);

		for (String uuid : imagesList) {
			try {
				chap.getParagraphs().add(new Paragraph(images.get(uuid)));
			} catch (Exception e) {
				Instance.getTraceHandler().error(e);
			}
		}

		if (meta.getCover() == null && !images.isEmpty()) {
			meta.setCover(images.get(imagesList.get(0)));
			meta.setFakeCover(true);
		}

		pg.setProgress(100);
		return story;
	}
}
