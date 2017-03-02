package be.nikiroo.fanfix.bundles;

import be.nikiroo.utils.resources.Meta;

/**
 * The configuration options.
 * 
 * @author niki
 */
public enum Config {
	@Meta(what = "language", where = "", format = "language (example: en-GB) or nothing for default system language", info = "Force the language (can be overwritten again with the env variable $LANG)")
	LANG, //
	@Meta(what = "reader type", where = "", format = "CLI or LOCAL", info = "Select the default reader to use to read stories (CLI = simple output to console, LOCAL = use local system file handler)")
	READER_TYPE, //
	@Meta(what = "directory", where = "", format = "absolute path, $HOME variable supported, / is always accepted as dir separator", info = "The directory where to store temporary files, defaults to directory 'tmp' in the conig directory (usually $HOME/.fanfix)")
	CACHE_DIR, //
	@Meta(what = "delay in hours", where = "", format = "integer | 0: no cache | -1: infinite time cache which is default", info = "The delay after which a cached resource that is thought to change ~often is considered too old and triggers a refresh")
	CACHE_MAX_TIME_CHANGING, //
	@Meta(what = "delay in hours", where = "", format = "integer | 0: no cache | -1: infinite time cache which is default", info = "The delay after which a cached resource that is thought to change rarely is considered too old and triggers a refresh")
	CACHE_MAX_TIME_STABLE, //
	@Meta(what = "string", where = "", format = "", info = "The user-agent to use to download files")
	USER_AGENT, //
	@Meta(what = "directory", where = "", format = "absolute path, $HOME variable supported, / is always accepted as dir separator", info = "The directory where to get the default story covers")
	DEFAULT_COVERS_DIR, //
	@Meta(what = "directory", where = "", format = "absolute path, $HOME variable supported, / is always accepted as dir separator", info = "The directory where to store the library")
	LIBRARY_DIR, //
	@Meta(what = "boolean", where = "", format = "'true' or 'false'", info = "Show debug information on errors")
	DEBUG_ERR, //
	@Meta(what = "image format", where = "", format = "PNG, JPG, BMP...", info = "Image format to use for cover images")
	IMAGE_FORMAT_COVER, //
	@Meta(what = "image format", where = "", format = "PNG, JPG, BMP...", info = "Image format to use for content images")
	IMAGE_FORMAT_CONTENT, //
	@Meta(what = "", where = "", format = "not used", info = "This key is only present to allow access to suffixes")
	LATEX_LANG, //
	@Meta(what = "LaTeX output language", where = "LaTeX", format = "", info = "LaTeX full name for English")
	LATEX_LANG_EN, //
	@Meta(what = "LaTeX output language", where = "LaTeX", format = "", info = "LaTeX full name for French")
	LATEX_LANG_FR, //
	@Meta(what = "other 'by' prefixes before author name", where = "", format = "comma-separated list", info = "used to identify the author")
	BYS, //
	@Meta(what = "Chapter identification languages", where = "", format = "comma-separated list", info = "used to identify a starting chapter in text mode")
	CHAPTER, //
	@Meta(what = "Chapter identification string", where = "", format = "", info = "used to identify a starting chapter in text mode")
	CHAPTER_EN, //
	@Meta(what = "Chapter identification string", where = "", format = "", info = "used to identify a starting chapter in text mode")
	CHAPTER_FR, //
	@Meta(what = "Login information", where = "", format = "", info = "used to login on YiffStar to have access to all the stories (should not be necessary anymore)")
	LOGIN_YIFFSTAR_USER, //
	@Meta(what = "Login information", where = "", format = "", info = "used to login on YiffStar to have access to all the stories (should not be necessary anymore)")
	LOGIN_YIFFSTAR_PASS, //
}
