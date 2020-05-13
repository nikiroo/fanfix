package be.nikiroo.fanfix.library;

class WebLibraryUrls {
	static public final String INDEX_URL = "/";

	static public final String VERSION_URL = "/version";

	static public final String LOGOUT_URL = "/logout";

	static private final String VIEWER_URL_BASE = "/view/story/";
	static private final String VIEWER_URL = VIEWER_URL_BASE
			+ "{luid}/{chap}/{para}";

	static private final String STORY_URL_BASE = "/story/";
	static private final String STORY_URL = STORY_URL_BASE
			+ "{luid}/{chap}/{para}";
	static private final String STORY_URL_COVER = STORY_URL_BASE
			+ "{luid}/cover";
	static private final String STORY_URL_JSON = STORY_URL_BASE + "{luid}/json";

	static private final String LIST_URL_BASE = "/list/";

	static public final String LIST_URL_METADATA = LIST_URL_BASE + "metadata";

	static public String getViewUrl(String luid, Integer chap, Integer para) {
		return VIEWER_URL //
				.replace("{luid}", luid) //
				.replace("/{chap}", chap == null ? "" : "/" + chap) //
				.replace("/{para}",
						(chap == null || para == null) ? "" : "/" + para);
	}

	static public String getStoryUrl(String luid, int chap, Integer para) {
		return STORY_URL //
				.replace("{luid}", luid) //
				.replace("{chap}", Integer.toString(chap)) //
				.replace("{para}", para == null ? "" : Integer.toString(para));
	}

	static public String getStoryUrlCover(String luid) {
		return STORY_URL_COVER //
				.replace("{luid}", luid);
	}

	static public String getStoryUrlJson(String luid) {
		return STORY_URL_JSON //
				.replace("{luid}", luid);
	}

	static public boolean isSupportedUrl(String url) {
		return INDEX_URL.equals(url) || VERSION_URL.equals(url)
				|| LOGOUT_URL.equals(url) || isViewUrl(url) || isStoryUrl(url)
				|| isListUrl(url);
	}

	static public boolean isViewUrl(String url) {
		return url != null && url.startsWith(VIEWER_URL_BASE);
	}

	static public boolean isStoryUrl(String url) {
		return url != null && url.startsWith(STORY_URL_BASE);
	}

	static public boolean isListUrl(String url) {
		return url != null && url.startsWith(LIST_URL_BASE);
	}
}
