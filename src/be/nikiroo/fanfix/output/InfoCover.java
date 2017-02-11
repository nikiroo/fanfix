package be.nikiroo.fanfix.output;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;

class InfoCover {
	public static void writeInfo(File targetDir, String targetName,
			MetaData meta) throws IOException {
		File info = new File(targetDir, targetName + ".info");
		FileWriter infoWriter = new FileWriter(info);

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

				String lang = meta.getLang();
				if (lang != null) {
					lang = lang.toLowerCase();
				}

				writeMeta(infoWriter, "TITLE", meta.getTitle());
				writeMeta(infoWriter, "AUTHOR", meta.getAuthor());
				writeMeta(infoWriter, "DATE", meta.getDate());
				writeMeta(infoWriter, "SUBJECT", meta.getSubject());
				writeMeta(infoWriter, "SOURCE", meta.getSource());
				writeMeta(infoWriter, "TAGS", tags);
				writeMeta(infoWriter, "UUID", meta.getUuid());
				writeMeta(infoWriter, "LUID", meta.getLuid());
				writeMeta(infoWriter, "LANG", lang);
				writeMeta(infoWriter, "IMAGES_DOCUMENT",
						meta.isImageDocument() ? "true" : "false");
				if (meta.getCover() != null) {
					String format = Instance.getConfig()
							.getString(Config.IMAGE_FORMAT_COVER).toLowerCase();
					writeMeta(infoWriter, "COVER", targetName + "." + format);
				} else {
					writeMeta(infoWriter, "COVER", "");
				}
				writeMeta(infoWriter, "EPUBCREATOR", BasicOutput.EPUB_CREATOR);
				writeMeta(infoWriter, "PUBLISHER", meta.getPublisher());
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

	private static void writeMeta(FileWriter writer, String key, String value)
			throws IOException {
		if (value == null) {
			value = "";
		}

		writer.write(String.format("%s=\"%s\"\n", key, value.replace("\"", "'")));
	}
}
