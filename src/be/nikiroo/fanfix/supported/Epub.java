package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.MarkableFileInputStream;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;

/**
 * Support class for EPUB files created with this program (as we need some
 * metadata available in those we create).
 * 
 * @author niki
 */
class Epub extends InfoText {
	protected MetaData meta;
	private File tmpDir;
	private File tmp;
	private String desc;

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
		if (desc != null) {
			return desc;
		}

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
		tmpDir = Instance.getTempFiles().createTempDir("fanfic-reader-parser");
		tmp = new File(tmpDir, "file.txt");
		File tmpInfo = new File(tmpDir, "file.info");
		fakeSource = tmp.toURI().toURL();
		Image cover = null;

		String url = source.toString();
		String title = null;
		String author = null;

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
							cover = new Image(zipIn);
						} catch (Exception e) {
							Instance.getTraceHandler().error(e);
						}
					}
				} else if (entry.getName().equals(getDataPrefix() + "URL")) {
					String[] descArray = StringUtils
							.unhtml(IOUtils.readSmallStream(zipIn)).trim()
							.split("\n");
					if (descArray.length > 0) {
						url = descArray[0].trim();
					}
				} else if (entry.getName().equals(getDataPrefix() + "SUMMARY")) {
					String[] descArray = StringUtils
							.unhtml(IOUtils.readSmallStream(zipIn)).trim()
							.split("\n");
					int skip = 0;
					if (descArray.length > 1) {
						title = descArray[0].trim();
						skip = 1;
						if (descArray.length > 2
								&& descArray[1].startsWith("©")) {
							author = descArray[1].substring(1).trim();
							skip = 2;
						}
					}
					this.desc = "";
					for (int i = skip; i < descArray.length; i++) {
						this.desc += descArray[i].trim() + "\n";
					}

					this.desc = this.desc.trim();
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
			if (title == null || title.isEmpty()) {
				title = new File(source.getPath()).getName();
				if (title.toLowerCase().endsWith(".cbz")) {
					title = title.substring(0, title.length() - 4);
				}
				title = URLDecoder.decode(title, "UTF-8").trim();
			}

			meta = new MetaData();
			meta.setLang("en");
			meta.setTags(new ArrayList<String>());
			meta.setSource(getSourceName());
			meta.setUuid(url);
			meta.setUrl(url);
			meta.setTitle(title);
			meta.setAuthor(author);
			meta.setImageDocument(isImagesDocumentByDefault());
		}
	}

	@Override
	protected void close() {
		if (tmpDir != null) {
			IOUtils.deltree(tmpDir);
		}

		tmpDir = null;
		tmp = null;

		if (fakeIn != null) {
			try {
				fakeIn.close();
			} catch (Exception e) {
				Instance.getTraceHandler().error(e);
			}
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

	protected boolean isImagesDocumentByDefault() {
		return false;
	}
}
