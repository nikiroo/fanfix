package be.nikiroo.fanfix.supported;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.Progress;

/**
 * Support class for <a href="http://www.fimfiction.net/">FimFiction.net</a>
 * stories, a website dedicated to My Little Pony.
 * <p>
 * This version uses the new, official API of FimFiction.
 * 
 * @author niki
 */
class FimfictionApi extends BasicSupport {
	private String oauth;
	private String storyId;
	private String json;

	private Map<Integer, String> chapterNames;
	private Map<Integer, String> chapterContents;

	public FimfictionApi() throws IOException {
		if (Instance.getConfig().getBoolean(
				Config.LOGIN_FIMFICTION_APIKEY_FORCE_HTML, false)) {
			throw new IOException(
					"Configuration is set to force HTML scrapping");
		}

		String oauth = Instance.getConfig().getString(
				Config.LOGIN_FIMFICTION_APIKEY_TOKEN);

		if (oauth == null || oauth.isEmpty()) {
			String clientId = Instance.getConfig().getString(
					Config.LOGIN_FIMFICTION_APIKEY_CLIENT_ID)
					+ "";
			String clientSecret = Instance.getConfig().getString(
					Config.LOGIN_FIMFICTION_APIKEY_CLIENT_SECRET)
					+ "";

			if (clientId.trim().isEmpty() || clientSecret.trim().isEmpty()) {
				throw new IOException("API key required for the beta API v2");
			}

			oauth = generateOAuth(clientId, clientSecret);

			Instance.getConfig().setString(
					Config.LOGIN_FIMFICTION_APIKEY_TOKEN, oauth);
			Instance.getConfig().updateFile();
		}

		this.oauth = oauth;
	}

	@Override
	public String getOAuth() {
		return oauth;
	}

	@Override
	protected boolean isHtml() {
		return true;
	}

	@Override
	public String getSourceName() {
		return "FimFiction.net";
	}

	@Override
	protected void preprocess(URL source, InputStream in) throws IOException {
		// extract the ID from:
		// https://www.fimfiction.net/story/123456/name-of-story
		storyId = getKeyText(source.toString(), "/story/", null, "/");

		// Selectors, so to download all I need and only what I need
		String storyContent = "fields[story]=title,description,date_published,cover_image";
		String authorContent = "fields[author]=name";
		String chapterContent = "fields[chapter]=chapter_number,title,content,authors_note";
		String contentContent = "fields[content]=html";
		String authorsNoteContent = "fields[authors_note]=html";
		String includes = "author,chapters,tags";

		String urlString = String.format(
				"https://www.fimfiction.net/api/v2/stories/%s?" //
						+ "%s&%s&"//
						+ "%s&%s&%s&" //
						+ "include=%s", //
				storyId, //
				storyContent, authorContent, //
				chapterContent, contentContent, authorsNoteContent,//
				includes);

		// URL params must be URL-encoded: "[ ]" <-> "%5B %5D"
		urlString = urlString.replace("[", "%5B").replace("]", "%5D");

		URL url = new URL(urlString);
		InputStream jsonIn = Instance.getCache().open(url, this, false);
		try {
			json = IOUtils.readSmallStream(jsonIn);
		} finally {
			jsonIn.close();
		}
	}

	@Override
	protected InputStream openInput(URL source) throws IOException {
		return null;
	}

	@Override
	protected MetaData getMeta(URL source, InputStream in) throws IOException {
		MetaData meta = new MetaData();

		meta.setTitle(getKeyJson(json, 0, "type", "story", "title"));
		meta.setAuthor(getKeyJson(json, 0, "type", "user", "name"));
		meta.setDate(getKeyJson(json, 0, "type", "story", "date_published"));
		meta.setTags(getTags());
		meta.setSource(getSourceName());
		meta.setUrl(source.toString());
		meta.setPublisher(getSourceName());
		meta.setUuid(source.toString());
		meta.setLuid("");
		meta.setLang("EN");
		meta.setSubject("MLP");
		meta.setType(getType().toString());
		meta.setImageDocument(false);
		meta.setCover(getImage(this, null,
				getKeyJson(json, 0, "type", "story", "cover_image", "full")));

		return meta;
	}

	private List<String> getTags() {
		List<String> tags = new ArrayList<String>();
		tags.add("MLP");

		int pos = 0;
		while (pos >= 0) {
			pos = indexOfJsonAfter(json, pos, "type", "story_tag");
			if (pos >= 0) {
				tags.add(getKeyJson(json, pos, "name"));
			}
		}

		return tags;
	}

	@Override
	protected String getDesc(URL source, InputStream in) {
		return getKeyJson(json, 0, "type", "story", "description");
	}

	@Override
	protected List<Entry<String, URL>> getChapters(URL source, InputStream in,
			Progress pg) {
		List<Entry<String, URL>> urls = new ArrayList<Entry<String, URL>>();

		chapterNames = new HashMap<Integer, String>();
		chapterContents = new HashMap<Integer, String>();

		int pos = 0;
		while (pos >= 0) {
			pos = indexOfJsonAfter(json, pos, "type", "chapter");
			if (pos >= 0) {
				int posNumber = indexOfJsonAfter(json, pos, "chapter_number");
				int posComa = json.indexOf(",", posNumber);
				final int number = Integer.parseInt(json.substring(posNumber,
						posComa).trim());
				final String title = getKeyJson(json, pos, "title");
				String notes = getKeyJson(json, pos, "authors_note", "html");
				String content = getKeyJson(json, pos, "content", "html");

				chapterNames.put(number, title);
				chapterContents
						.put(number, content + "<br/>* * *<br/>" + notes);

				urls.add(new Entry<String, URL>() {
					@Override
					public URL setValue(URL value) {
						return null;
					}

					@Override
					public String getKey() {
						return title;
					}

					@Override
					public URL getValue() {
						return null;
					}
				});
			}
		}

		return urls;
	}

	@Override
	protected String getChapterContent(URL source, InputStream in, int number,
			Progress pg) {
		return chapterContents.get(number);
	}

	@Override
	protected boolean supports(URL url) {
		return "fimfiction.net".equals(url.getHost())
				|| "www.fimfiction.net".equals(url.getHost());
	}

	static private String generateOAuth(String clientId, String clientSecret)
			throws IOException {
		URL url = new URL("https://www.fimfiction.net/api/v2/token");
		Map<String, String> params = new HashMap<String, String>();
		params.put("client_id", clientId);
		params.put("client_secret", clientSecret);
		params.put("grant_type", "client_credentials");
		InputStream in = Instance.getCache().openNoCache(url, null, params,
				null, null);

		String jsonToken = IOUtils.readSmallStream(in);

		// Extract token type and token from: {
		// token_type = "bearer",
		// access_token = "xxxxxxxxxxxxxx"
		// }

		String token = getKeyText(jsonToken, "\"access_token\"", "\"", "\"");
		String tokenType = getKeyText(jsonToken, "\"token_type\"", "\"", "\"");

		// TODO: remove this once the bug is fixed on the server side
		if ("bearer".equals(tokenType)) {
			tokenType = "Bearer";
		}

		return tokenType + " " + token;
	}

	// afters: [name, value] pairs (or "" for any of them), can end without
	// value
	static private int indexOfJsonAfter(String json, int startAt,
			String... afterKeys) {
		ArrayList<String> afters = new ArrayList<String>();
		boolean name = true;
		for (String key : afterKeys) {
			if (key != null && !key.isEmpty()) {
				afters.add("\"" + key + "\"");
			} else {
				afters.add("\"");
				afters.add("\"");
			}

			if (name) {
				afters.add(":");
			}

			name = !name;
		}

		return indexOfAfter(json, startAt, afters.toArray(new String[] {}));
	}

	// afters: [name, value] pairs (or "" for any of them), can end without
	// value
	static private String getKeyJson(String json, int startAt,
			String... afterKeys) {
		int pos = indexOfJsonAfter(json, startAt, afterKeys);
		if (pos < 0) {
			return null;
		}

		return getKeyText(json.substring(pos), "\"", null, "\"");
	}
}
