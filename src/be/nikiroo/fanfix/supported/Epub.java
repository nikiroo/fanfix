package be.nikiroo.fanfix.supported;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.ImageUtils;
import be.nikiroo.utils.MarkableFileInputStream;
import be.nikiroo.utils.Progress;

/**
 * Support class for EPUB files created with this program (as we need some
 * metadata available in those we create).
 * 
 * @author niki
 */
class Epub extends InfoText {
	private File tmp;
	protected MetaData meta;

	private URL fakeSource;
	private InputStream fakeIn;

	@Override
	public String getSourceName() {
		return "epub";
	}

	@Override
	protected boolean supports(URL url) {
		if (url.getPath().toLowerCase().endsWith(".epub")) {
			return true;
		}

		return false;
	}

	@Override
	protected MetaData getMeta(URL source, InputStream in) throws IOException {
		return meta;
	}

	@Override
	protected String getDesc(URL source, InputStream in) throws IOException {
		if (fakeIn != null) {
			fakeIn.reset();
			return super.getDesc(fakeSource, fakeIn);
		}

		return null;
	}

	@Override
	protected List<Entry<String, URL>> getChapters(URL source, InputStream in,
			Progress pg) throws IOException {
		if (fakeIn != null) {
			fakeIn.reset();
			return super.getChapters(fakeSource, fakeIn, pg);
		}

		return null;
	}

	@Override
	protected String getChapterContent(URL source, InputStream in, int number,
			Progress pg) throws IOException {
		if (fakeIn != null) {
			fakeIn.reset();
			return super.getChapterContent(fakeSource, fakeIn, number, pg);
		}

		return null;
	}

	@Override
	protected void preprocess(URL source, InputStream in) throws IOException {
		// Note: do NOT close this stream, as it would also close "in"
		ZipInputStream zipIn = new ZipInputStream(in);
		tmp = File.createTempFile("fanfic-reader-parser_", ".tmp");
		File tmpInfo = new File(tmp + ".info");
		fakeSource = tmp.toURI().toURL();
		BufferedImage cover = null;

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

				if (entry.getName().equals(getDataPrefix() + "version")) {
					// Nothing to do for now ("first"
					// version is 3.0)
				} else if (entryLName.endsWith(".info")) {
					// Info file
					IOUtils.write(zipIn, tmpInfo);
				} else if (imageEntry) {
					// Cover
					if (getCover()) {
						try {
							cover = ImageUtils.fromStream(zipIn);
						} catch (Exception e) {
							Instance.syserr(e);
						}
					}
				} else if (entry.getName().equals(getDataPrefix() + "URL")) {
					// Do nothing
				} else if (entry.getName().equals(getDataPrefix() + "SUMMARY")) {
					// Do nothing
				} else {
					// Hopefully the data file
					IOUtils.write(zipIn, tmp);
				}
			}
		}

		if (requireInfo() && (!tmp.exists() || !tmpInfo.exists())) {
			throw new IOException(
					"file not supported (maybe not created with this program or corrupt)");
		}

		if (tmp.exists()) {
			this.fakeIn = new MarkableFileInputStream(new FileInputStream(tmp));
		}

		if (tmpInfo.exists()) {
			meta = InfoReader.readMeta(tmpInfo, true);
			if (cover != null) {
				meta.setCover(cover);
			}
			tmpInfo.delete();
		} else {
			meta = new MetaData();
			meta.setUuid(source.toString());
			meta.setLang("EN");
			meta.setTags(new ArrayList<String>());
			meta.setSource(getSourceName());
			meta.setUrl(source.toString());
		}
	}

	@Override
	protected void close() throws IOException {
		if (tmp != null && tmp.exists()) {
			if (!tmp.delete()) {
				tmp.deleteOnExit();
			}
		}

		tmp = null;

		if (fakeIn != null) {
			fakeIn.close();
		}

		super.close();
	}

	protected String getDataPrefix() {
		return "DATA/";
	}

	protected boolean requireInfo() {
		return true;
	}

	protected boolean getCover() {
		return true;
	}
}
