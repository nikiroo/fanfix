package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.MarkableFileInputStream;

// not complete: no "description" tag
public class InfoReader {
	public static MetaData readMeta(File infoFile, boolean withCover)
			throws IOException {
		if (infoFile == null) {
			throw new IOException("File is null");
		}

		if (infoFile.exists()) {
			InputStream in = new MarkableFileInputStream(new FileInputStream(
					infoFile));
			try {
				return createMeta(infoFile.toURI().toURL(), in, withCover);
			} finally {
				in.close();
				in = null;
			}
		} else {
			throw new FileNotFoundException(
					"File given as argument does not exists: "
							+ infoFile.getAbsolutePath());
		}
	}

	private static MetaData createMeta(URL sourceInfoFile, InputStream in,
			boolean withCover) throws IOException {
		MetaData meta = new MetaData();

		meta.setTitle(getInfoTag(in, "TITLE"));
		meta.setAuthor(getInfoTag(in, "AUTHOR"));
		meta.setDate(getInfoTag(in, "DATE"));
		meta.setTags(getInfoTagList(in, "TAGS", ","));
		meta.setSource(getInfoTag(in, "SOURCE"));
		meta.setUrl(getInfoTag(in, "URL"));
		meta.setPublisher(getInfoTag(in, "PUBLISHER"));
		meta.setUuid(getInfoTag(in, "UUID"));
		meta.setLuid(getInfoTag(in, "LUID"));
		meta.setLang(getInfoTag(in, "LANG"));
		meta.setSubject(getInfoTag(in, "SUBJECT"));
		meta.setType(getInfoTag(in, "TYPE"));
		meta.setImageDocument(getInfoTagBoolean(in, "IMAGES_DOCUMENT", false));
		if (withCover) {
			meta.setCover(BasicSupport.getImage(null, sourceInfoFile,
					getInfoTag(in, "COVER")));
			// Second chance: try to check for a cover next to the info file
			if (meta.getCover() == null) {
				String info = sourceInfoFile.getFile().toString();
				if (info.endsWith(".info")) {
					info = info.substring(0, info.length() - ".info".length());
					String ext = "."
							+ Instance.getConfig().getString(
									Config.IMAGE_FORMAT_COVER);
					meta.setCover(BasicSupport.getImage(null, sourceInfoFile,
							info + ext));
				}
			}
		}
		try {
			meta.setWords(Long.parseLong(getInfoTag(in, "WORDCOUNT")));
		} catch (NumberFormatException e) {
			meta.setWords(0);
		}
		meta.setCreationDate(getInfoTag(in, "CREATION_DATE"));
		meta.setFakeCover(Boolean.parseBoolean(getInfoTag(in, "FAKE_COVER")));

		if (withCover && meta.getCover() == null) {
			meta.setCover(BasicSupport.getDefaultCover(meta.getSubject()));
		}

		return meta;
	}

	private static boolean getInfoTagBoolean(InputStream in, String key,
			boolean def) throws IOException {
		Boolean value = getInfoTagBoolean(in, key);
		return value == null ? def : value;
	}

	private static Boolean getInfoTagBoolean(InputStream in, String key)
			throws IOException {
		String value = getInfoTag(in, key);
		if (value != null && !value.trim().isEmpty()) {
			value = value.toLowerCase().trim();
			return value.equals("1") || value.equals("on")
					|| value.equals("true") || value.equals("yes");
		}

		return null;
	}

	private static List<String> getInfoTagList(InputStream in, String key,
			String separator) throws IOException {
		List<String> list = new ArrayList<String>();
		String tt = getInfoTag(in, key);
		if (tt != null) {
			for (String tag : tt.split(separator)) {
				list.add(tag.trim());
			}
		}

		return list;
	}

	/**
	 * Return the value of the given tag in the <tt>.info</tt> file if present.
	 * 
	 * @param key
	 *            the tag key
	 * 
	 * @return the value or NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private static String getInfoTag(InputStream in, String key)
			throws IOException {
		key = "^" + key + "=";

		if (in != null) {
			in.reset();
			String value = BasicSupport.getLine(in, key, 0);
			if (value != null && !value.isEmpty()) {
				value = value.trim().substring(key.length() - 1).trim();
				if (value.startsWith("'") && value.endsWith("'")
						|| value.startsWith("\"") && value.endsWith("\"")) {
					value = value.substring(1, value.length() - 1).trim();
				}

				return value;
			}
		}

		return null;
	}
}
