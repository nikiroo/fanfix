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
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;

/**
 * Support class for <a href="https://sofurry.com/">SoFurry.com</a>, a Furry
 * website supporting images and stories (we only retrieve the stories).
 * 
 * @author niki
 */
class YiffStar extends BasicSupport {

	@Override
	public String getSourceName() {
		return "YiffStar";
	}

	@Override
	protected MetaData getMeta(URL source, InputStream in) throws IOException {
		MetaData meta = new MetaData();

		meta.setTitle(getTitle(reset(in)));
		meta.setAuthor(getAuthor(source, reset(in)));
		meta.setDate("");
		meta.setTags(getTags(reset(in)));
		meta.setSource(getSourceName());
		meta.setUrl(source.toString());
		meta.setPublisher(getSourceName());
		meta.setUuid(source.toString());
		meta.setLuid("");
		meta.setLang("EN");
		meta.setSubject("Furry");
		meta.setType(getType().toString());
		meta.setImageDocument(false);
		meta.setCover(getCover(source, reset(in)));

		return meta;
	}

	@Override
	protected boolean supports(URL url) {
		String host = url.getHost();
		if (host.startsWith("www.")) {
			host = host.substring("www.".length());
		}

		return "sofurry.com".equals(host);
	}

	@Override
	protected boolean isHtml() {
		return true;
	}

	@Override
	public void login() throws IOException {
		// Note: this should not be necessary anymore
		// (the "/guest" trick is enough)
		String login = Instance.getConfig().getString(
				Config.LOGIN_YIFFSTAR_USER);
		String password = Instance.getConfig().getString(
				Config.LOGIN_YIFFSTAR_PASS);

		if (login != null && !login.isEmpty() && password != null
				&& !password.isEmpty()) {
			Map<String, String> post = new HashMap<String, String>();
			post.put("sfLoginUsername", login);
			post.put("sfLoginPassword", password);
			post.put("YII_CSRF_TOKEN", "");

			// Cookies will actually be retained by the cache manager once
			// logged in
			Instance.getCache()
					.openNoCache(new URL("https://www.sofurry.com/user/login"),
							this, post).close();
		}
	}

	@Override
	public URL getCanonicalUrl(URL source) throws IOException {
		if (source.getPath().startsWith("/view")) {
			source = new URL(source.toString() + "/guest");
			InputStream in = Instance.getCache().open(source, this, false);
			String line = getLine(in, "/browse/folder/", 0);
			if (line != null) {
				String[] tab = line.split("\"");
				if (tab.length > 1) {
					String groupUrl = source.getProtocol() + "://"
							+ source.getHost() + tab[1];
					return guest(groupUrl);
				}
			}
		}

		return super.getCanonicalUrl(source);
	}

	private List<String> getTags(InputStream in) {
		List<String> tags = new ArrayList<String>();

		String line = getLine(in, "class=\"sf-story-big-tags", 0);
		if (line != null) {
			String[] tab = StringUtils.unhtml(line).split(",");
			for (String possibleTag : tab) {
				String tag = possibleTag.trim();
				if (!tag.isEmpty() && !tag.equals("...") && !tags.contains(tag)) {
					tags.add(tag);
				}
			}
		}

		return tags;
	}

	private BufferedImage getCover(URL source, InputStream in)
			throws IOException {

		List<Entry<String, URL>> chaps = getChapters(source, in, null);
		if (!chaps.isEmpty()) {
			in = Instance.getCache().open(chaps.get(0).getValue(), this, true);
			String line = getLine(in, " name=\"og:image\"", 0);
			if (line != null) {
				int pos = -1;
				for (int i = 0; i < 3; i++) {
					pos = line.indexOf('"', pos + 1);
				}

				if (pos >= 0) {
					line = line.substring(pos + 1);
					pos = line.indexOf('"');
					if (pos >= 0) {
						line = line.substring(0, pos);
						if (line.contains("/thumb?")) {
							line = line.replace("/thumb?",
									"/auxiliaryContent?type=25&");
							return getImage(this, null, line);
						}
					}
				}
			}
		}

		return null;
	}

	private String getAuthor(URL source, InputStream in) throws IOException {
		String author = getLine(in, "class=\"onlinestatus", 0);
		if (author != null) {
			return StringUtils.unhtml(author).trim();
		}

		return null;
	}

	private String getTitle(InputStream in) throws IOException {
		String title = getLine(in, "class=\"sflabel pagetitle", 0);
		if (title != null) {
			if (title.contains("(series)")) {
				title = title.replace("(series)", "");
			}
			return StringUtils.unhtml(title).trim();
		}

		return null;
	}

	@Override
	protected String getDesc(URL source, InputStream in) throws IOException {
		return null; // TODO: no description at all? Cannot find one...
	}

	@Override
	protected List<Entry<String, URL>> getChapters(URL source, InputStream in,
			Progress pg) throws IOException {
		List<Entry<String, URL>> urls = new ArrayList<Entry<String, URL>>();

		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		while (scan.hasNext()) {
			String line = scan.next();
			if (line.contains("\"/view/") && line.contains("title=")) {
				String[] tab = line.split("\"");
				if (tab.length > 5) {
					String link = tab[5];
					if (link.startsWith("/")) {
						link = source.getProtocol() + "://" + source.getHost()
								+ link;
					}
					final URL value = guest(link);
					final String key = StringUtils.unhtml(line).trim();
					urls.add(new Entry<String, URL>() {
						public URL setValue(URL value) {
							return null;
						}

						public URL getValue() {
							return value;
						}

						public String getKey() {
							return key;
						}
					});
				}
			}
		}

		return urls;
	}

	@Override
	protected String getChapterContent(URL source, InputStream in, int number,
			Progress pg) throws IOException {
		StringBuilder builder = new StringBuilder();

		String startAt = "id=\"sfContentBody";
		String endAt = "id=\"recommendationArea";
		boolean ok = false;

		@SuppressWarnings("resource")
		Scanner scan = new Scanner(in, "UTF-8");
		scan.useDelimiter("\\n");
		while (scan.hasNext()) {
			String line = scan.next();
			if (!ok && line.contains(startAt)) {
				ok = true;
			} else if (ok && line.contains(endAt)) {
				ok = false;
				break;
			}

			if (ok) {
				builder.append(line);
				builder.append(' ');
			}
		}

		return builder.toString();
	}

	/**
	 * Return a {@link URL} from the given link, but add the "/guest" part to it
	 * to make sure we don't need to be logged-in to see it.
	 * 
	 * @param link
	 *            the link
	 * 
	 * @return the {@link URL}
	 * 
	 * @throws MalformedURLException
	 *             in case of data error
	 */
	private URL guest(String link) throws MalformedURLException {
		if (link.contains("?")) {
			if (link.contains("/?")) {
				return new URL(link.replace("?", "guest?"));
			} else {
				return new URL(link.replace("?", "/guest?"));
			}
		} else {
			return new URL(link + "/guest");
		}
	}
}
