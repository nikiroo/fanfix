package be.nikiroo.fanfix.supported;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.MarkableFileInputStream;

/**
 * Support class for EPUB files created with this program (as we need some
 * metadata available in those we create).
 * 
 * @author niki
 */
class Epub extends BasicSupport {
	private InfoText base;
	private URL fakeSource;

	private File tmpCover;
	private File tmpInfo;
	private File tmp;

	/** Only used by {@link Epub#getInput()} so it is always reset. */
	private InputStream in;

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
	protected boolean isHtml() {
		if (tmpInfo.exists()) {
			return base.isHtml();
		}

		return false;
	}

	@Override
	protected String getTitle(URL source, InputStream in) throws IOException {
		if (tmpInfo.exists()) {
			return base.getTitle(fakeSource, getFakeInput());
		}

		return source.toString();
	}

	@Override
	protected String getAuthor(URL source, InputStream in) throws IOException {
		if (tmpInfo.exists()) {
			return base.getAuthor(fakeSource, getFakeInput());
		}

		return null;
	}

	@Override
	protected String getDate(URL source, InputStream in) throws IOException {
		if (tmpInfo.exists()) {
			return base.getDate(fakeSource, getFakeInput());
		}

		return null;
	}

	@Override
	protected String getSubject(URL source, InputStream in) throws IOException {
		if (tmpInfo.exists()) {
			return base.getSubject(fakeSource, getFakeInput());
		}

		return null;
	}

	@Override
	protected String getDesc(URL source, InputStream in) throws IOException {
		if (tmpInfo.exists()) {
			return base.getDesc(fakeSource, getFakeInput());
		}

		return null;
	}

	@Override
	protected URL getCover(URL source, InputStream in) throws IOException {
		if (tmpCover.exists()) {
			return tmpCover.toURI().toURL();
		}

		return null;
	}

	@Override
	protected List<Entry<String, URL>> getChapters(URL source, InputStream in)
			throws IOException {
		if (tmpInfo.exists()) {
			return base.getChapters(fakeSource, getFakeInput());
		}

		return null;
	}

	@Override
	protected String getChapterContent(URL source, InputStream in, int number)
			throws IOException {
		if (tmpInfo.exists()) {
			return base.getChapterContent(fakeSource, getFakeInput(), number);
		}

		return null;
	}

	@Override
	protected String getLang(URL source, InputStream in) throws IOException {
		if (tmpInfo.exists()) {
			return base.getLang(fakeSource, getFakeInput());
		}

		return super.getLang(source, in);
	}

	@Override
	protected String getPublisher(URL source, InputStream in)
			throws IOException {
		if (tmpInfo.exists()) {
			return base.getPublisher(fakeSource, getFakeInput());
		}

		return super.getPublisher(source, in);
	}

	@Override
	protected List<String> getTags(URL source, InputStream in)
			throws IOException {
		if (tmpInfo.exists()) {
			return base.getTags(fakeSource, getFakeInput());
		}

		return super.getTags(source, in);
	}

	@Override
	protected String getUuid(URL source, InputStream in) throws IOException {
		if (tmpInfo.exists()) {
			return base.getUuid(fakeSource, getFakeInput());
		}

		return super.getUuid(source, in);
	}

	@Override
	protected String getLuid(URL source, InputStream in) throws IOException {
		if (tmpInfo.exists()) {
			return base.getLuid(fakeSource, getFakeInput());
		}

		return super.getLuid(source, in);
	}

	@Override
	protected void preprocess(InputStream in) throws IOException {
		// Note: do NOT close this stream, as it would also close "in"
		ZipInputStream zipIn = new ZipInputStream(in);
		tmp = File.createTempFile("fanfic-reader-parser_", ".tmp");
		tmpInfo = new File(tmp + ".info");
		tmpCover = File.createTempFile("fanfic-reader-parser_", ".tmp");

		base = new InfoText();
		fakeSource = tmp.toURI().toURL();

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
							BufferedImage image = ImageIO.read(zipIn);
							ImageIO.write(image, Instance.getConfig()
									.getString(Config.IMAGE_FORMAT_COVER)
									.toLowerCase(), tmpCover);
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
			this.in = new MarkableFileInputStream(new FileInputStream(tmp));
		}
	}

	@Override
	protected void close() throws IOException {
		for (File file : new File[] { tmp, tmpInfo, tmpCover }) {
			if (file != null && file.exists()) {
				if (!file.delete()) {
					file.deleteOnExit();
				}
			}
		}

		tmp = null;
		tmpInfo = null;
		tmpCover = null;
		fakeSource = null;

		try {
			if (in != null) {
				in.close();
			}
		} finally {
			in = null;
			base.close();
		}
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

	/**
	 * Reset then return {@link Epub#in}.
	 * 
	 * @return {@link Epub#in}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private InputStream getFakeInput() throws IOException {
		in.reset();
		return in;
	}
}
