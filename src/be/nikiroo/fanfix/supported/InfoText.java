package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import be.nikiroo.fanfix.Instance;

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
	@Override
	public String getSourceName() {
		return "info-text";
	}

	@Override
	protected String getTitle(URL source, InputStream in) throws IOException {
		String tag = getInfoTag(source, "TITLE");
		if (tag != null) {
			return tag;
		}

		return super.getTitle(source, in);
	}

	@Override
	protected String getAuthor(URL source, InputStream in) throws IOException {
		String tag = getInfoTag(source, "AUTHOR");
		if (tag != null) {
			return tag;
		}

		return super.getAuthor(source, in);
	}

	@Override
	protected String getDate(URL source, InputStream in) throws IOException {
		String tag = getInfoTag(source, "DATE");
		if (tag != null) {
			return tag;
		}

		return super.getDate(source, in);
	}

	@Override
	protected String getSubject(URL source, InputStream in) throws IOException {
		String tag = getInfoTag(source, "SUBJECT");
		if (tag != null) {
			return tag;
		}

		return super.getSubject(source, in);
	}

	@Override
	protected String getLang(URL source, InputStream in) throws IOException {
		String tag = getInfoTag(source, "LANG");
		if (tag != null) {
			return tag;
		}

		return super.getLang(source, in);
	}

	@Override
	protected String getPublisher(URL source, InputStream in)
			throws IOException {
		String tag = getInfoTag(source, "PUBLISHER");
		if (tag != null) {
			return tag;
		}

		return super.getPublisher(source, in);
	}

	@Override
	protected String getUuid(URL source, InputStream in) throws IOException {
		String tag = getInfoTag(source, "UUID");
		if (tag != null) {
			return tag;
		}

		return super.getUuid(source, in);
	}

	@Override
	protected String getLuid(URL source, InputStream in) throws IOException {
		String tag = getInfoTag(source, "LUID");
		if (tag != null) {
			return tag;
		}

		return super.getLuid(source, in);
	}

	@Override
	protected List<String> getTags(URL source, InputStream in)
			throws IOException {
		List<String> tags = super.getTags(source, in);

		String tt = getInfoTag(source, "TAGS");
		if (tt != null) {
			for (String tag : tt.split(",")) {
				tags.add(tag.trim());
			}
		}

		return tags;
	}

	@Override
	public boolean isImageDocument(URL source, InputStream in)
			throws IOException {
		String tag = getInfoTag(source, "IMAGES_DOCUMENT");
		if (tag != null) {
			return tag.trim().toLowerCase().equals("true");
		}

		return super.isImageDocument(source, in);
	}

	@Override
	protected URL getCover(URL source, InputStream in) {
		File file;
		try {
			file = new File(source.toURI());
			file = new File(file.getPath() + ".info");
		} catch (URISyntaxException e) {
			Instance.syserr(e);
			file = null;
		}

		String path = null;
		if (file != null && file.exists()) {
			try {
				InputStream infoIn = new FileInputStream(file);
				try {
					String key = "COVER=";
					String tt = getLine(infoIn, key, 0);
					if (tt != null && !tt.isEmpty()) {
						tt = tt.substring(key.length()).trim();
						if (tt.startsWith("'") && tt.endsWith("'")) {
							tt = tt.substring(1, tt.length() - 1).trim();
						}

						URL cover = getImage(source, tt);
						if (cover != null) {
							path = cover.getFile();
						}
					}
				} finally {
					infoIn.close();
				}
			} catch (MalformedURLException e) {
				Instance.syserr(e);
			} catch (IOException e) {
				Instance.syserr(e);
			}
		}

		if (path != null) {
			try {
				return new File(path).toURI().toURL();
			} catch (MalformedURLException e) {
				Instance.syserr(e);
			}
		}

		return null;
	}

	@Override
	protected boolean supports(URL url) {
		if ("file".equals(url.getProtocol())) {
			File file;
			try {
				file = new File(url.toURI());
				file = new File(file.getPath() + ".info");
			} catch (URISyntaxException e) {
				Instance.syserr(e);
				file = null;
			}

			return file != null && file.exists();
		}

		return false;
	}

	/**
	 * Return the value of the given tag in the <tt>.info</tt> file if present.
	 * 
	 * @param source
	 *            the source story {@link URL}
	 * @param key
	 *            the tag key
	 * 
	 * @return the value or NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private String getInfoTag(URL source, String key) throws IOException {
		key = "^" + key + "=";

		File file;
		try {
			file = new File(source.toURI());
			file = new File(file.getPath() + ".info");
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}

		if (file.exists()) {
			InputStream infoIn = new FileInputStream(file);
			try {
				String value = getLine(infoIn, key, 0);
				if (value != null && !value.isEmpty()) {
					value = value.trim().substring(key.length()).trim();
					if (value.startsWith("'") && value.endsWith("'")
							|| value.startsWith("\"") && value.endsWith("\"")) {
						value = value.substring(1, value.length() - 1).trim();
					}

					return value;
				}
			} finally {
				infoIn.close();
			}
		}

		return null;
	}
}
