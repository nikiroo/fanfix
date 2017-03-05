package be.nikiroo.fanfix.bundles;

import be.nikiroo.utils.resources.Meta;

/**
 * The configuration options.
 * 
 * @author niki
 */
public enum Config {
	@Meta(what = "language (example: en-GB, fr-BE...) or nothing for default system language", where = "", format = "Locale|''", info = "Force the language (can be overwritten again with the env variable $LANG)")
	LANG, //
	@Meta(what = "reader type (CLI = simple output to console, LOCAL = use local system file handler)", where = "", format = "'CLI'|'LOCAL'", info = "Select the default reader to use to read stories")
	READER_TYPE, //
	@Meta(what = "absolute path, $HOME variable supported, / is always accepted as dir separator", where = "", format = "Directory", info = "The directory where to store temporary files, defaults to directory 'tmp' in the conig directory (usually $HOME/.fanfix)")
	CACHE_DIR, //
	@Meta(what = "delay in hours, or 0 for no cache, or -1 for infinite time (default)", where = "", format = "int", info = "The delay after which a cached resource that is thought to change ~often is considered too old and triggers a refresh")
	CACHE_MAX_TIME_CHANGING, //
	@Meta(what = "delay in hours, or 0 for no cache, or -1 for infinite time (default)", where = "", format = "int", info = "The delay after which a cached resource that is thought to change rarely is considered too old and triggers a refresh")
	CACHE_MAX_TIME_STABLE, //
	@Meta(what = "string", where = "", format = "String", info = "The user-agent to use to download files")
	USER_AGENT, //
	@Meta(what = "absolute path, $HOME variable supported, / is always accepted as dir separator", where = "", format = "Directory", info = "The directory where to get the default story covers")
	DEFAULT_COVERS_DIR, //
	@Meta(what = "absolute path, $HOME variable supported, / is always accepted as dir separator", where = "", format = "Directory", info = "The directory where to store the library")
	LIBRARY_DIR, //
	@Meta(what = "boolean", where = "", format = "'true'|'false'", info = "Show debug information on errors")
	DEBUG_ERR, //
	@Meta(what = "image format", where = "", format = "'PNG'|JPG'|'BMP'", info = "Image format to use for cover images")
	IMAGE_FORMAT_COVER, //
	@Meta(what = "image format", where = "", format = "'PNG'|JPG'|'BMP'", info = "Image format to use for content images")
	IMAGE_FORMAT_CONTENT, //
	// This key is only present to allow access to suffixes, so no Meta
	LATEX_LANG, //
	@Meta(what = "LaTeX output language", where = "LaTeX", format = "String", info = "LaTeX full name for English")
	LATEX_LANG_EN, //
	@Meta(what = "LaTeX output language", where = "LaTeX", format = "String", info = "LaTeX full name for French")
	LATEX_LANG_FR, //
	@Meta(what = "other 'by' prefixes before author name", where = "", format = "comma-separated list|String", info = "used to identify the author")
	BYS, //
	@Meta(what = "Chapter identification languages", where = "", format = "comma-separated list|String", info = "used to identify a starting chapter in text mode")
	CHAPTER, //
	@Meta(what = "Chapter identification string", where = "String", format = "", info = "used to identify a starting chapter in text mode")
	CHAPTER_EN, //
	@Meta(what = "Chapter identification string", where = "String", format = "", info = "used to identify a starting chapter in text mode")
	CHAPTER_FR, //
	@Meta(what = "Login information", where = "", format = "String", info = "used to login on YiffStar to have access to all the stories (should not be necessary anymore)")
	LOGIN_YIFFSTAR_USER, //
	@Meta(what = "Login information", where = "", format = "Password", info = "used to login on YiffStar to have access to all the stories (should not be necessary anymore)")
	LOGIN_YIFFSTAR_PASS, //
	@Meta(what = "Minimum time between version update checks in days, or -1 for 'no checks' -- default is 1 day", where = "VersionCheck", format = "int", info = "If the last update check was done at least that many days, check for updates at startup")
	UPDATE_INTERVAL,
}
