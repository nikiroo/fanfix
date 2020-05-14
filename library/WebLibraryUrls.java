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
	static private final String STORY_URL_METADATA = STORY_URL_BASE
			+ "{luid}/metadata";

	// GET/SET ("value" param -> set STA to this value)
	static private final String STORY_URL_SOURCE = STORY_URL_BASE
			+ "{luid}/source";
	static private final String STORY_URL_TITLE = STORY_URL_BASE
			+ "{luid}/title";
	static private final String STORY_URL_AUTHOR = STORY_URL_BASE
			+ "{luid}/author";

	static private final String LIST_URL_BASE = "/list/";

	static public final String LIST_URL_METADATA = LIST_URL_BASE + "metadata";

	// "import" requires param "url" and return an luid, "/{luid}" return
	// progress status as a JSON Progress or 404 if none (done or failed)
	static private final String IMPRT_URL_BASE = "/import/";
	static private final String IMPRT_URL_PROGRESS = IMPRT_URL_BASE + "{luid}";
	static public final String IMPRT_URL_IMPORT = IMPRT_URL_BASE + "import";

	static private final String DELETE_URL_BASE = "/delete/";
	static private final String DELETE_URL_STORY = DELETE_URL_BASE + "{luid}";

	// GET/SET ("luid" param -> set cover to the cover of this story -- not ok
	// for /cover/story/)
	static private final String COVER_URL_BASE = "/cover/";
	static private final String COVER_URL_STORY = COVER_URL_BASE
			+ "story/{luid}";
	static private final String COVER_URL_AUTHOR = COVER_URL_BASE
			+ "author/{author}";
	static private final String COVER_URL_SOURCE = COVER_URL_BASE
			+ "source/{source}";

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

	static public String getStoryUrlSource(String luid) {
		return STORY_URL_SOURCE //
				.replace("{luid}", luid);
	}

	static public String getStoryUrlTitle(String luid) {
		return STORY_URL_TITLE//
				.replace("{luid}", luid);
	}

	static public String getStoryUrlAuthor(String luid) {
		return STORY_URL_AUTHOR //
				.replace("{luid}", luid);
	}

	static public String getStoryUrlMetadata(String luid) {
		return STORY_URL_METADATA //
				.replace("{luid}", luid);
	}

	static public String getImprtProgressUrl(String luid) {
		return IMPRT_URL_PROGRESS //
				.replace("{luid}", luid);
	}

	static public boolean isSupportedUrl(String url) {
		return INDEX_URL.equals(url) || VERSION_URL.equals(url)
				|| LOGOUT_URL.equals(url) || isViewUrl(url) || isStoryUrl(url)
				|| isListUrl(url) || isCoverUrl(url) || isImprtUrl(url);
	}

	static public String getCoverUrlStory(String luid) {
		return COVER_URL_STORY //
				.replace("{luid}", luid);
	}

	static public String getCoverUrlSource(String source) {
		return COVER_URL_SOURCE //
				.replace("{source}", source);
	}

	static public String getCoverUrlAuthor(String author) {
		return COVER_URL_AUTHOR //
				.replace("{author}", author);
	}

	static public String getDeleteUrlStory(String luid) {
		return DELETE_URL_STORY //
				.replace("{luid}", luid);
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

	static public boolean isCoverUrl(String url) {
		return url != null && url.startsWith(COVER_URL_BASE);
	}

	static public boolean isImprtUrl(String url) {
		return url != null && url.startsWith(IMPRT_URL_BASE);
	}

	static public boolean isDeleteUrl(String url) {
		return url != null && url.startsWith(DELETE_URL_BASE);
	}
}
