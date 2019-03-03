package be.nikiroo.fanfix.bundles;

import be.nikiroo.utils.resources.Meta;
import be.nikiroo.utils.resources.Meta.Format;

/**
 * The configuration options.
 * 
 * @author niki
 */
@SuppressWarnings("javadoc")
public enum Config {
	@Meta(description = "language (example: en-GB, fr-BE...) or nothing for default system language", format = Format.LOCALE, info = "Force the language (can be overwritten again with the env variable $LANG)")
	LANG, //
	@Meta(description = "reader type (CLI = simple output to console, TUI = Text User Interface with menus and windows, GUI = a GUI with locally stored files)", format = Format.FIXED_LIST, list = {
			"CLI", "GUI", "TUI" }, info = "Select the default reader to use to read stories")
	READER_TYPE, //
	@Meta(def = "INFO_TEXT", format = Format.COMBO_LIST, list = { "INFO_TEXT",
			"EPUB", "HTML", "TEXT" }, info = "One of the known output type", description = "The type of output for the Local Reader for non-images documents")
	NON_IMAGES_DOCUMENT_TYPE, //
	@Meta(def = "CBZ", format = Format.COMBO_LIST, list = { "CBZ", "HTML" }, description = "The type of output for the Local Reader for images documents")
	IMAGES_DOCUMENT_TYPE, //
	@Meta(description = "absolute path, $HOME variable supported, / is always accepted as dir separator", format = Format.DIRECTORY, info = "The directory where to store temporary files, defaults to directory 'tmp' in the conig directory (usually $HOME/.fanfix)")
	CACHE_DIR, //
	@Meta(def = "24", description = "delay in hours, or 0 for no cache, or -1 for infinite time (default)", format = Format.INT, info = "The delay after which a cached resource that is thought to change ~often is considered too old and triggers a refresh")
	CACHE_MAX_TIME_CHANGING, //
	@Meta(def = "720", description = "delay in hours, or 0 for no cache, or -1 for infinite time (default)", format = Format.INT, info = "The delay after which a cached resource that is thought to change rarely is considered too old and triggers a refresh")
	CACHE_MAX_TIME_STABLE, //
	@Meta(def = "Mozilla/5.0 (X11; Linux x86_64; rv:44.0) Gecko/20100101 Firefox/44.0 -- ELinks/0.9.3 (Linux 2.6.11 i686; 80x24)", description = "string", info = "The user-agent to use to download files")
	USER_AGENT, //
	@Meta(def = "$HOME/.fanfix/covers/", description = "absolute path, $HOME variable supported, / is always accepted as dir separator", format = Format.DIRECTORY, info = "The directory where to get the default story covers")
	DEFAULT_COVERS_DIR, //
	@Meta(description = "string", info = "The default library to use (KEY:SERVER:PORT), or empty for the local library")
	DEFAULT_LIBRARY, //
	@Meta(def = "$HOME/Books", description = "absolute path, $HOME variable supported, / is always accepted as dir separator", format = Format.DIRECTORY, info = "The directory where to store the library")
	LIBRARY_DIR, //
	@Meta(def = "false", description = "boolean", format = Format.BOOLEAN, info = "Show debug information on errors")
	DEBUG_ERR, //
	@Meta(def = "false", description = "boolean", format = Format.BOOLEAN, info = "Show debug trace information")
	DEBUG_TRACE, //
	@Meta(def = "PNG", description = "image format", format = Format.COMBO_LIST, list = {
			"PNG", "JPG", "BMP" }, info = "Image format to use for cover images")
	IMAGE_FORMAT_COVER, //
	@Meta(def = "JPG", description = "image format", format = Format.COMBO_LIST, list = {
			"PNG", "JPG", "BMP" }, info = "Image format to use for content images")
	IMAGE_FORMAT_CONTENT, //
	@Meta(group = true)
	LATEX_LANG, //
	@Meta(def = "english", description = "LaTeX output language: English", info = "LaTeX full name")
	LATEX_LANG_EN, //
	@Meta(def = "french", description = "LaTeX output language: French", info = "LaTeX full name")
	LATEX_LANG_FR, //
	@Meta(def = "by,par,de,©,(c)", description = "other 'by' prefixes before author name, used to identify the author", array = true)
	BYS, //
	@Meta(def = "EN,FR", description = "List of languages codes used for chapter identification (should not be changed)", array = true, info = "EN,FR")
	CHAPTER, //
	@Meta(def = "Chapter", description = "Chapter identification String: English", info = "used to identify a starting chapter in text mode")
	CHAPTER_EN, //
	@Meta(def = "Chapitre", description = "Chapter identification String: French", info = "used to identify a starting chapter in text mode")
	CHAPTER_FR, //
	@Meta(description = "Login information (username) for YiffStar to have access to all the stories (should not be necessary anymore)")
	LOGIN_YIFFSTAR_USER, //
	@Meta(description = "Login information (password) for YiffStar to have access to all the stories (should not be necessary anymore)", format = Format.PASSWORD)
	LOGIN_YIFFSTAR_PASS, //
	@Meta(description = "If the last update check was done at least that many days, check for updates at startup (-1 for 'no checks' -- default is 1 day)", format = Format.INT)
	UPDATE_INTERVAL, //
	@Meta(description = "An API key required to create a token from FimFiction", format = Format.STRING)
	LOGIN_FIMFICTION_APIKEY_CLIENT_ID, //
	@Meta(description = "An API key required to create a token from FimFiction", format = Format.PASSWORD)
	LOGIN_FIMFICTION_APIKEY_CLIENT_SECRET, //
	@Meta(description = "Do not use the new API, even if we have a token, and force HTML scraping (default is false, use API if token or ID present)", format = Format.BOOLEAN)
	LOGIN_FIMFICTION_APIKEY_FORCE_HTML, //
	@Meta(def = "Bearer WnZ5oHlzQoDocv1GcgHfcoqctHkSwL-D", description = "A token is required to use the beta APIv2 from FimFiction (see APIKEY_CLIENT_*)", format = Format.PASSWORD)
	LOGIN_FIMFICTION_APIKEY_TOKEN, //
}
