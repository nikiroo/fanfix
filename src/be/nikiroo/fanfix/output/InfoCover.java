package be.nikiroo.fanfix.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;

/**
 * Helper class to write info, cover and summary (resume) files.
 * 
 * @author niki
 */
public class InfoCover {
	/**
	 * Write both the <tt>.info<tt> and the <tt>.summary</tt> files by taking
	 * the information from the {@link MetaData}.
	 * 
	 * @param targetDir
	 *            the directory where to write the 2 files
	 * @param targetName
	 *            the target name (no extension) to use (so you will get
	 *            <tt>targetName.info</tt> and <tt>targetName.summary</tt>)
	 * @param meta
	 *            the {@link MetaData} to get the data out of
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void writeInfo(File targetDir, String targetName,
			MetaData meta) throws IOException {
		File info = new File(targetDir, targetName + ".info");

		if (meta != null) {
			BufferedWriter infoWriter = null;
			try {
				infoWriter = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(info), "UTF-8"));

				String tags = "";
				if (meta.getTags() != null) {
					for (String tag : meta.getTags()) {
						if (!tags.isEmpty()) {
							tags += ", ";
						}
						tags += tag;
					}
				}

				writeMeta(infoWriter, "TITLE", meta.getTitle());
				writeMeta(infoWriter, "AUTHOR", meta.getAuthor());
				writeMeta(infoWriter, "DATE", meta.getDate());
				writeMeta(infoWriter, "SUBJECT", meta.getSubject());
				writeMeta(infoWriter, "SOURCE", meta.getSource());
				writeMeta(infoWriter, "URL", meta.getUrl());
				writeMeta(infoWriter, "TAGS", tags);
				writeMeta(infoWriter, "UUID", meta.getUuid());
				writeMeta(infoWriter, "LUID", meta.getLuid());
				writeMeta(infoWriter, "LANG", meta.getLang() == null ? ""
						: meta.getLang().toLowerCase());
				writeMeta(infoWriter, "IMAGES_DOCUMENT",
						meta.isImageDocument() ? "true" : "false");
				writeMeta(infoWriter, "TYPE", meta.getType());
				if (meta.getCover() != null) {
					String format = Instance.getInstance().getConfig()
							.getString(Config.FILE_FORMAT_IMAGE_FORMAT_COVER)
							.toLowerCase();
					writeMeta(infoWriter, "COVER", targetName + "." + format);
				} else {
					writeMeta(infoWriter, "COVER", "");
				}
				writeMeta(infoWriter, "EPUBCREATOR", BasicOutput.EPUB_CREATOR);
				writeMeta(infoWriter, "PUBLISHER", meta.getPublisher());
				writeMeta(infoWriter, "WORDCOUNT",
						Long.toString(meta.getWords()));
				writeMeta(infoWriter, "CREATION_DATE", meta.getCreationDate());
				writeMeta(infoWriter, "FAKE_COVER",
						Boolean.toString(meta.isFakeCover()));
			} finally {
				if (infoWriter != null) {
					infoWriter.close();
				}
			}

			if (meta.getResume() != null) {
				Story fakeStory = new Story();
				fakeStory.setMeta(meta);
				fakeStory.setChapters(Arrays.asList(meta.getResume()));

				Text summaryText = new Text() {
					@Override
					protected boolean isWriteCover() {
						return false;
					}

					@Override
					protected boolean isWriteInfo() {
						return false; // infinite loop if not!
					}

					@Override
					public String getDefaultExtension(boolean readerTarget) {
						return ".summary";
					}

					@Override
					protected void writeStoryHeader(Story story)
							throws IOException {
					}

					@Override
					protected void writeStoryFooter(Story story)
							throws IOException {
					}

					@Override
					protected void writeChapterHeader(Chapter chap)
							throws IOException {
					}

					@Override
					protected void writeChapterFooter(Chapter chap)
							throws IOException {
					}
				};

				summaryText.process(fakeStory, targetDir, targetName);
			}
		}
	}

	/**
	 * Write the cover file.
	 * 
	 * @param targetDir
	 *            the target directory
	 * @param targetName
	 *            the target name for the cover (the extension will be added)
	 * @param meta
	 *            the meta to get the information out of
	 */
	public static void writeCover(File targetDir, String targetName,
			MetaData meta) {
		if (meta != null && meta.getCover() != null) {
			try {
				Instance.getInstance().getCache().saveAsImage(meta.getCover(), new File(targetDir, targetName), true);
			} catch (IOException e) {
				// Allow to continue without cover
				Instance.getInstance().getTraceHandler().error(new IOException("Failed to save the cover image", e));
			}
		}
	}

	private static void writeMeta(BufferedWriter writer, String key,
			String value) throws IOException {
		if (value == null) {
			value = "";
		}

		writer.write(String.format("%s=\"%s\"\n", key, value.replace("\"", "'")));
	}
}
