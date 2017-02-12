package be.nikiroo.fanfix.supported;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import javax.imageio.ImageIO;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.StringUtils;

class MangaFox extends BasicSupport {
	@Override
	protected boolean isHtml() {
		return true;
	}

	@Override
	public String getSourceName() {
		return "MangaFox.me";
	}

	@Override
	protected MetaData getMeta(URL source, InputStream in) throws IOException {
		MetaData meta = new MetaData();

		meta.setTitle(getTitle(reset(in)));
		meta.setAuthor(getAuthor(reset(in)));
		meta.setDate(getDate(reset(in)));
		meta.setTags(getTags(reset(in)));
		meta.setSource(getSourceName());
		meta.setPublisher(getSourceName());
		meta.setUuid(source.toString());
		meta.setLuid("");
		meta.setLang("EN");
		meta.setSubject("manga");
		meta.setType(getType().toString());
		meta.setImageDocument(true);
		meta.setCover(getCover(reset(in)));

		return meta;
	}

	private List<String> getTags(InputStream in) {
		List<String> tags = new ArrayList<String>();

		String line = getLine(in, "/genres/", 0);
		if (line != null) {
			line = StringUtils.unhtml(line);
			String[] tab = line.split(",");
			if (tab != null) {
				for (String tag : tab) {
					tags.add(tag.trim());
				}
			}
		}

		return tags;
	}

	private String getTitle(InputStream in) {
		String line = getLine(in, " property=\"og:title\"", 0);
		if (line != null) {
			int pos = -1;
			for (int i = 0; i < 3; i++) {
				pos = line.indexOf('"', pos + 1);
			}

			if (pos >= 0) {
				line = line.substring(pos + 1);
				pos = line.indexOf('"');
				if (pos >= 0) {
					return line.substring(0, pos);
				}
			}
		}

		return null;
	}

	private String getAuthor(InputStream in) {
		List<String> authors = new ArrayList<String>();

		String line = getLine(in, "/author/", 0, false);
		if (line != null) {
			for (String ln : StringUtils.unhtml(line).split(",")) {
				if (ln != null && !ln.trim().isEmpty()
						&& !authors.contains(ln.trim())) {
					authors.add(ln.trim());
				}
			}
		}

		try {
			in.reset();
		} catch (IOException e) {
			Instance.syserr(e);
		}

		line = getLine(in, "/artist/", 0, false);
		if (line != null) {
			for (String ln : StringUtils.unhtml(line).split(",")) {
				if (ln != null && !ln.trim().isEmpty()
						&& !authors.contains(ln.trim())) {
					authors.add(ln.trim());
				}
			}
		}

		if (authors.isEmpty()) {
			return null;
		} else {
			StringBuilder builder = new StringBuilder();
			for (String author : authors) {
				if (builder.length() > 0) {
					builder.append(", ");
				}

				builder.append(author);
			}

			return builder.toString();
		}
	}

	private String getDate(InputStream in) {
		String line = getLine(in, "/released/", 0);
		if (line != null) {
			line = StringUtils.unhtml(line);
			return line.trim();
		}

		return null;
	}

	@Override
	protected String getDesc(URL source, InputStream in) {
		String line = getLine(in, " property=\"og:description\"", 0);
		if (line != null) {
			int pos = -1;
			for (int i = 0; i < 3; i++) {
				pos = line.indexOf('"', pos + 1);
			}

			if (pos >= 0) {
				line = line.substring(pos + 1);
				pos = line.indexOf('"');
				if (pos >= 0) {
					return line.substring(0, pos);
				}
			}
		}

		return null;
	}

	private BufferedImage getCover(InputStream in) {
		String line = getLine(in, " property=\"og:image\"", 0);
		String cover = null;
		if (line != null) {
			int pos = -1;
			for (int i = 0; i < 3; i++) {
				pos = line.indexOf('"', pos + 1);
			}

			if (pos >= 0) {
				line = line.substring(pos + 1);
				pos = line.indexOf('"');
				if (pos >= 0) {
					cover = line.substring(0, pos);
				}
			}
		}

		if (cover != null) {
			InputStream coverIn;
			try {
				coverIn = openEx(cover);
				try {
					return ImageIO.read(coverIn);
				} finally {
					coverIn.close();
				}
			} catch (IOException e) {
			}
		}

		return null;
	}

	@Override
	protected List<Entry<String, URL>> getChapters(URL source, InputStream in) {
		List<Entry<String, URL>> urls = new ArrayList<Entry<String, URL>>();

		String volumeAt = "<h3 class=\"volume\">";
		String linkAt = "href=\"http://mangafox.me/";
		String endAt = "<script type=\"text/javascript\">";

		boolean started = false;

		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		while (scan.hasNext()) {
			String line = scan.next();

			if (started && line.contains(endAt)) {
				break;
			} else if (!started && line.contains(volumeAt)) {
				started = true;
			}

			if (started && line.contains(linkAt)) {
				// Chapter content url
				String url = null;
				int pos = line.indexOf("href=\"");
				if (pos >= 0) {
					line = line.substring(pos + "href=\"".length());
					pos = line.indexOf('\"');
					if (pos >= 0) {
						url = line.substring(0, pos);
					}
				}

				// Chapter name
				String name = null;
				if (scan.hasNext()) {
					name = StringUtils.unhtml(scan.next()).trim();
					// Remove the "new" tag if present
					if (name.endsWith("new")) {
						name = name.substring(0, name.length() - 3).trim();
					}
				}

				// to help with the retry and the originalUrl
				refresh(url);

				try {
					final String key = name;
					final URL value = new URL(url);
					urls.add(new Entry<String, URL>() {
						public URL setValue(URL value) {
							return null;
						}

						public String getKey() {
							return key;
						}

						public URL getValue() {
							return value;
						}
					});
				} catch (MalformedURLException e) {
					Instance.syserr(e);
				}
			}
		}

		// the chapters are in reversed order
		Collections.reverse(urls);

		return urls;
	}

	@Override
	protected String getChapterContent(URL source, InputStream in, int number) {
		StringBuilder builder = new StringBuilder();
		String base = getCurrentReferer().toString();
		int pos = base.lastIndexOf('/');
		base = base.substring(0, pos + 1); // including the '/' at the end

		boolean close = false;
		while (in != null) {
			String linkNextLine = getLine(in, "return enlarge()", 0);
			try {
				in.reset();
			} catch (IOException e) {
				Instance.syserr(e);
			}

			String linkImageLine = getLine(in, "return enlarge()", 1);
			String linkNext = null;
			String linkImage = null;
			pos = linkNextLine.indexOf("href=\"");
			if (pos >= 0) {
				linkNextLine = linkNextLine.substring(pos + "href=\"".length());
				pos = linkNextLine.indexOf('\"');
				if (pos >= 0) {
					linkNext = linkNextLine.substring(0, pos);
				}
			}
			pos = linkImageLine.indexOf("src=\"");
			if (pos >= 0) {
				linkImageLine = linkImageLine
						.substring(pos + "src=\"".length());
				pos = linkImageLine.indexOf('\"');
				if (pos >= 0) {
					linkImage = linkImageLine.substring(0, pos);
				}
			}

			if (linkImage != null) {
				builder.append("[");
				// to help with the retry and the originalUrl, part 1
				builder.append(withoutQuery(linkImage));
				builder.append("]\n");
			}

			// to help with the retry and the originalUrl, part 2
			refresh(linkImage);

			if (close) {
				try {
					in.close();
				} catch (IOException e) {
					Instance.syserr(e);
				}
			}

			in = null;
			if (linkNext != null && !"javascript:void(0);".equals(linkNext)) {
				URL url;
				try {
					url = new URL(base + linkNext);
					in = openEx(base + linkNext);
					setCurrentReferer(url);
				} catch (IOException e) {
					Instance.syserr(new IOException(
							"Cannot get the next manga page which is: "
									+ linkNext, e));
				}
			}

			close = true;
		}

		setCurrentReferer(source);
		return builder.toString();
	}

	@Override
	protected boolean supports(URL url) {
		return "mangafox.me".equals(url.getHost())
				|| "www.mangafox.me".equals(url.getHost());
	}

	/**
	 * Refresh the {@link URL} by calling {@link MangaFox#openEx(String)}.
	 * 
	 * @param url
	 *            the URL to refresh
	 * 
	 * @return TRUE if it was refreshed
	 */
	private boolean refresh(String url) {
		try {
			openEx(url).close();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Open the URL through the cache, but: retry a second time after 100ms if
	 * it fails, remove the query part of the {@link URL} before saving it to
	 * the cache (so it can be recalled later).
	 * 
	 * @param url
	 *            the {@link URL}
	 * 
	 * @return the resource
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private InputStream openEx(String url) throws IOException {
		try {
			return Instance.getCache().open(new URL(url), this, true,
					withoutQuery(url));
		} catch (Exception e) {
			// second chance
			try {
				Thread.sleep(100);
			} catch (InterruptedException ee) {
			}

			return Instance.getCache().open(new URL(url), this, true,
					withoutQuery(url));
		}
	}

	/**
	 * Return the same input {@link URL} but without the query part.
	 * 
	 * @param url
	 *            the inpiut {@link URL} as a {@link String}
	 * 
	 * @return the input {@link URL} without query
	 */
	private URL withoutQuery(String url) {
		URL o = null;
		try {
			// Remove the query from o (originalUrl), so it can be cached
			// correctly
			o = new URL(url);
			o = new URL(o.getProtocol() + "://" + o.getHost() + o.getPath());

			return o;
		} catch (MalformedURLException e) {
			return null;
		}
	}
}
