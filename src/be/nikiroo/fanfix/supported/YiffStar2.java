package be.nikiroo.fanfix.supported;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.Progress;

/**
 * Support class for <a href="https://sofurry.com/">SoFurry.com</a>, a Furry
 * website supporting images and stories (we only retrieve the stories).
 * 
 * @author niki
 */
class YiffStar2 extends BasicSupport {
	@Override
	protected String getSourceName() {
		return "YiffStar";
	}

	@Override
	protected void login() throws IOException {
		// Note: this is not necessary anymore for NSFW
		// (the "/guest" trick is enough)
		// ...but still required for RegUsersOnly pages
		String login = Instance.getConfig().getString(
				Config.LOGIN_YIFFSTAR_USER);
		String password = Instance.getConfig().getString(
				Config.LOGIN_YIFFSTAR_PASS);

		if (login != null && !login.isEmpty() && password != null
				&& !password.isEmpty()) {
			Map<String, String> post = new HashMap<String, String>();
			post.put("YII_CSRF_TOKEN", "");
			post.put("LoginForm[sfLoginUsername]", login);
			post.put("LoginForm[sfLoginPassword]", password);
			post.put("returnUrl", "/");
			post.put("yt1", "Login");

			// Cookies will actually be retained by the cache manager once
			// logged in
			setCurrentReferer(null);
			Instance.getCache()
					.openNoCache(new URL("https://www.sofurry.com/user/login"),
							this, post, null, null).close();
		}
	}

	@Override
	protected Document loadDocument(URL source) throws IOException {
		String url = getCanonicalUrl(source).toString();
		return DataUtil
				.load(Instance.getCache().openNoCache(source, this, null, null,
						null), "UTF-8", url.toString());
	}

	@Override
	protected boolean isHtml() {
		return true;
	}

	@Override
	protected MetaData getMeta() throws IOException {

		IOUtils.writeSmallFile(new File("/tmp/node.html"), getSourceNode()
				.outerHtml());

		MetaData meta = new MetaData();

		meta.setTitle("");
		meta.setAuthor("");
		meta.setDate("");
		meta.setTags(new ArrayList<String>());
		meta.setSource(getSourceName());
		meta.setUrl(getSource().toString());
		meta.setPublisher(getSourceName());
		meta.setUuid(getSource().toString());
		meta.setLuid("");
		meta.setLang("en");
		meta.setSubject("Furry");
		meta.setType(getType().toString());
		meta.setImageDocument(false);
		meta.setCover(null);

		return meta;
	}

	@Override
	protected String getDesc() throws IOException {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	protected List<Entry<String, URL>> getChapters(Progress pg)
			throws IOException {
		// TODO Auto-generated method stub
		return new ArrayList<Map.Entry<String, URL>>();
	}

	@Override
	protected String getChapterContent(URL chapUrl, int number, Progress pg)
			throws IOException {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	protected boolean supports(URL url) {
		String host = url.getHost();
		if (host.startsWith("www.")) {
			host = host.substring("www.".length());
		}

		return "sofurry.com".equals(host);
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
	static private URL guestUrl(String link) throws MalformedURLException {

		if (true)
			return new URL(link);

		if (link.contains("?")) {
			if (link.contains("/?")) {
				return new URL(link.replace("?", "guest?"));
			}

			return new URL(link.replace("?", "/guest?"));
		}

		return new URL(link + "/guest");
	}
}
