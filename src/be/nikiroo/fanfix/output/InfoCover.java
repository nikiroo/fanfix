package be.nikiroo.fanfix.output;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.imageio.ImageIO;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;

public class InfoCover {
	public static void writeInfo(File targetDir, String targetName,
			MetaData meta) throws IOException {
		File info = new File(targetDir, targetName + ".info");
		BufferedWriter infoWriter = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(info), "UTF-8"));

		if (meta != null) {
			try {
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
					String format = Instance.getConfig()
							.getString(Config.IMAGE_FORMAT_COVER).toLowerCase();
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
				infoWriter.close();
			}
		}
	}

	public static void writeCover(File targetDir, String targetName,
			MetaData meta) {
		if (meta != null && meta.getCover() != null) {
			try {
				String format = Instance.getConfig()
						.getString(Config.IMAGE_FORMAT_COVER).toLowerCase();
				ImageIO.write(meta.getCover(), format, new File(targetDir,
						targetName + "." + format));
			} catch (IOException e) {
				// Allow to continue without cover
				Instance.syserr(new IOException(
						"Failed to save the cover image", e));
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
