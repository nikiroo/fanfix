package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.IOException;
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
import be.nikiroo.utils.IOUtils;
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
	protected boolean isImagesDocumentByDefault() {
		return true;
	}

	@Override
	protected boolean getCover() {
		return false;
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

		File tmpDir = Instance.getTempFiles().createTempDir("info-text");
		String basename = null;

		Map<String, Image> images = new HashMap<String, Image>();
		try {
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
						String uuid = meta.getUuid() + "_" + entry.getName();
						try {
							images.put(uuid, new Image(zipIn));
						} catch (Exception e) {
							Instance.getTraceHandler().error(e);
						}

						if (pg.getProgress() < 85) {
							pg.add(1);
						}
					} else if (entryLName.endsWith(".info")) {
						basename = entryLName.substring(0, entryLName.length()
								- ".info".length());
						IOUtils.write(zipIn, new File(tmpDir, entryLName));
					} else if (entryLName.endsWith(".txt")) {
						IOUtils.write(zipIn, new File(tmpDir, entryLName));
					}
				}
			}

			pg.setProgress(85);

			// ZIP order is not correct for us
			List<String> imagesList = new ArrayList<String>(images.keySet());
			Collections.sort(imagesList);

			pg.setProgress(90);

			File txt = new File(tmpDir, basename + ".txt");
			if (!txt.exists()) {
				basename = null;
			}
			if (basename != null) {
				try {
					BasicSupport support = BasicSupport.getSupport(txt.toURI()
							.toURL());
					Story origStory = support.process(null);
					story.setChapters(origStory.getChapters());
					story.setMeta(origStory.getMeta());
				} catch (Exception e) {
					basename = null;
				}
			}

			if (basename == null) {
				story.setChapters(new ArrayList<Chapter>());
			}

			if (!imagesList.isEmpty()) {
				Chapter chap = new Chapter(story.getChapters().size() + 1, null);
				story.getChapters().add(chap);

				for (String uuid : imagesList) {
					try {
						chap.getParagraphs().add(
								new Paragraph(images.get(uuid)));
					} catch (Exception e) {
						Instance.getTraceHandler().error(e);
					}
				}
			}

			if (meta.getCover() == null && !images.isEmpty()) {
				meta.setCover(images.get(imagesList.get(0)));
				meta.setFakeCover(true);
			}

		} finally {
			IOUtils.deltree(tmpDir);
		}

		pg.setProgress(100);
		return story;
	}
}
