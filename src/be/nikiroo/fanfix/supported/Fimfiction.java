package be.nikiroo.fanfix.supported;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.StringUtils;

/**
 * Support class for <a href="http://www.fimfiction.net/">FimFiction.net</a>
 * stories, a website dedicated to My Little Pony.
 * 
 * @author niki
 */
class Fimfiction extends BasicSupport {
	@Override
	protected boolean isHtml() {
		return true;
	}

	@Override
	public String getSourceName() {
		return "FimFiction.net";
	}

	@Override
	protected MetaData getMeta(URL source, InputStream in) throws IOException {
		MetaData meta = new MetaData();

		meta.setTitle(getTitle(reset(in)));
		meta.setAuthor(getAuthor(reset(in)));
		meta.setDate(getDate(reset(in)));
		meta.setTags(getTags(reset(in)));
		meta.setSource(getSourceName());
		meta.setUrl(source.toString());
		meta.setPublisher(getSourceName());
		meta.setUuid(source.toString());
		meta.setLuid("");
		meta.setLang("EN");
		meta.setSubject("MLP");
		meta.setType(getType().toString());
		meta.setImageDocument(false);
		meta.setCover(getCover(reset(in)));

		return meta;
	}

	@Override
	public Map<String, String> getCookies() {
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put("view_mature", "true");
		return cookies;
	}

	private List<String> getTags(InputStream in) {
		List<String> tags = new ArrayList<String>();
		tags.add("MLP");

		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		while (scan.hasNext()) {
			String line = scan.next();
			if (line.contains("story_category") && !line.contains("title=")) {
				int pos = line.indexOf('>');
				if (pos >= 0) {
					line = line.substring(pos + 1);
					pos = line.indexOf('<');
					if (pos >= 0) {
						line = line.substring(0, pos);
					}
				}

				line = line.trim();
				if (!tags.contains(line)) {
					tags.add(line);
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
					return StringUtils.unhtml(line.substring(0, pos)).trim();
				}
			}
		}

		return null;
	}

	private String getAuthor(InputStream in) {
		String line = getLine(in, " href=\"/user/", 0);
		if (line != null) {
			int pos = line.indexOf('"');
			if (pos >= 0) {
				line = line.substring(pos + 1);
				pos = line.indexOf('"');
				if (pos >= 0) {
					line = line.substring(0, pos);
					pos = line.lastIndexOf('/');
					if (pos >= 0) {
						line = line.substring(pos + 1);
						return line.replace('+', ' ');
					}
				}
			}
		}

		return null;
	}

	private String getDate(InputStream in) {
		String line = getLine(in, "<span class=\"date\">", 0);
		if (line != null) {
			int pos = -1;
			for (int i = 0; i < 3; i++) {
				pos = line.indexOf('>', pos + 1);
			}

			if (pos >= 0) {
				line = line.substring(pos + 1);
				pos = line.indexOf('<');
				if (pos >= 0) {
					return line.substring(0, pos).trim();
				}
			}
		}

		return null;
	}

	@Override
	protected String getDesc(URL source, InputStream in) {
		// the og: meta version is the SHORT resume, this is the LONG resume
		return getLine(in, "class=\"more_button hidden\"", -1);
	}

	private BufferedImage getCover(InputStream in) {
		// Note: the 'og:image' is the SMALL cover, not the full version
		String cover = getLine(in, "<div class=\"story_image\">", 1);
		if (cover != null) {
			int pos = cover.indexOf('"');
			if (pos >= 0) {
				cover = cover.substring(pos + 1);
				pos = cover.indexOf('"');
				if (pos >= 0) {
					cover = cover.substring(0, pos);
				}
			}
		}

		return getImage(null, cover);
	}

	@Override
	protected List<Entry<String, URL>> getChapters(URL source, InputStream in) {
		List<Entry<String, URL>> urls = new ArrayList<Entry<String, URL>>();
		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		while (scan.hasNext()) {
			String line = scan.next();
			if (line.contains("class=\"chapter_link\"")
					|| line.contains("class='chapter_link'")) {
				// Chapter name
				String name = line;
				int pos = name.indexOf('>');
				if (pos >= 0) {
					name = name.substring(pos + 1);
					pos = name.indexOf('<');
					if (pos >= 0) {
						name = name.substring(0, pos);
					}
				}
				// Chapter content
				pos = line.indexOf('/');
				if (pos >= 0) {
					line = line.substring(pos); // we take the /, not +1
					pos = line.indexOf('"');
					if (pos >= 0) {
						line = line.substring(0, pos);
					}
				}

				try {
					final String key = name;
					final URL value = new URL("http://www.fimfiction.net"
							+ line);
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

		return urls;
	}

	@Override
	protected String getChapterContent(URL source, InputStream in, int number) {
		return getLine(in, "<div id=\"chapter_container\">", 1);
	}

	@Override
	protected boolean supports(URL url) {
		return "fimfiction.net".equals(url.getHost())
				|| "www.fimfiction.net".equals(url.getHost());
	}
}
