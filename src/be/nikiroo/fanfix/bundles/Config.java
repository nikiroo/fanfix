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
	@Meta(description = "The language (example: en-GB, fr-BE...) or nothing for default system language (can be overwritten with the variable $LANG)",//
	format = Format.LOCALE)
	LANG, //
	@Meta(description = "The default reader type to use to read stories:\nCLI = simple output to console\nTUI = a Text User Interface with menus and windows, based upon Jexer\nGUI = a GUI with locally stored files, based upon Swing", //
	format = Format.FIXED_LIST, list = { "CLI", "GUI", "TUI" }, def = "GUI")
	READER_TYPE, //
	@Meta(description = "The type of output for the Local Reader for non-images documents",//
	format = Format.FIXED_LIST, list = { "INFO_TEXT", "EPUB", "HTML", "TEXT" }, def = "INFO_TEXT")
	NON_IMAGES_DOCUMENT_TYPE, //
	@Meta(description = "The type of output for the Local Reader for non-images documents",//
	format = Format.FIXED_LIST, list = { "CBZ", "HTML" }, def = "CBZ")
	IMAGES_DOCUMENT_TYPE, //
	@Meta(description = "The directory where to store temporary files; any relative path uses the applciation config directory as base, $HOME notation is supported, / is always accepted as directory separator",//
	format = Format.DIRECTORY, def = "tmp/")
	CACHE_DIR, //
	@Meta(description = "The delay in hours after which a cached resource that is thought to change ~often is considered too old and triggers a refresh delay (or 0 for no cache, or -1 for infinite time)", //
	format = Format.INT, def = "24")
	CACHE_MAX_TIME_CHANGING, //
	@Meta(description = "The delay in hours after which a cached resource that is thought to change rarely is considered too old and triggers a refresh delay (or 0 for no cache, or -1 for infinite time)", //
	format = Format.INT, def = "720")
	CACHE_MAX_TIME_STABLE, //
	@Meta(description = "The user-agent to use to download files",//
	def = "Mozilla/5.0 (X11; Linux x86_64; rv:44.0) Gecko/20100101 Firefox/44.0 -- ELinks/0.9.3 (Linux 2.6.11 i686; 80x24) -- Fanfix (https://github.com/nikiroo/fanfix/)")
	USER_AGENT, //
	@Meta(description = "The directory where to get the default story covers; any relative path uses the applciation config directory as base, $HOME notation is supported, / is always accepted as directory separator",//
	format = Format.DIRECTORY, def = "covers/")
	DEFAULT_COVERS_DIR, //
	@Meta(description = "The default library to use (KEY:SERVER:PORT), or empty for the local library",//
	format = Format.STRING, def = "")
	DEFAULT_LIBRARY, //
	@Meta(description = "The port on which we can start the server (must be a valid port, from 1 to 65535)", //
	format = Format.INT, def = "58365")
	SERVER_PORT, //
	@Meta(description = "The encryption key for the server (NOT including a subkey), it cannot contain the pipe character \"|\" but can be empty (it is *still* encrypted, but with an empty, easy to guess key)",//
	format = Format.PASSWORD, def = "")
	SERVER_KEY, //
	@Meta(description = "Allow write access to the clients (download story, move story...) without RW subkeys", //
	format = Format.BOOLEAN, def = "true")
	SERVER_RW, //
	@Meta(description = "If not empty, only the EXACT listed sources will be available for clients without BL subkeys",//
	array = true, format = Format.STRING, def = "")
	SERVER_WHITELIST, //
	@Meta(description = "The subkeys that the server will allow, including the modes", //
	array = true, format = Format.STRING, def = "")
	SERVER_ALLOWED_SUBKEYS, //
	@Meta(description = "The directory where to store the library (can be overriden by the envvironment variable \"BOOKS_DIR\"; any relative path uses the applciation config directory as base, $HOME notation is supported, / is always accepted as directory separator",//
	format = Format.DIRECTORY, def = "$HOME/Books/")
	LIBRARY_DIR, //
	@Meta(description = "Show debug information on errors",//
	format = Format.BOOLEAN, def = "false")
	DEBUG_ERR, //
	@Meta(description = "Show debug trace information",//
	format = Format.BOOLEAN, def = "false")
	DEBUG_TRACE, //
	@Meta(description = "Image format to use for cover images",//
	format = Format.FIXED_LIST, list = { "PNG", "JPG", "BMP" }, def = "PNG")
	IMAGE_FORMAT_COVER, //
	@Meta(description = "Image format to use for content images",//
	format = Format.FIXED_LIST, list = { "PNG", "JPG", "BMP" }, def = "jpg")
	IMAGE_FORMAT_CONTENT, //
	@Meta(group = true)
	LATEX_LANG, //
	@Meta(description = "LaTeX output language (full name) for \"English\"",//
	format = Format.STRING, def = "english")
	LATEX_LANG_EN, //
	@Meta(description = "LaTeX output language (full name) for \"French\"",//
	format = Format.STRING, def = "french")
	LATEX_LANG_FR, //
	@Meta(description = "other 'by' prefixes before author name, used to identify the author",//
	array = true, format = Format.STRING, def = "\"by\",\"par\",\"de\",\"©\",\"(c)\"")
	BYS, //
	@Meta(description = "List of languages codes used for chapter identification (should not be changed)", //
	array = true, format = Format.STRING, def = "\"EN\",\"FR\"")
	CHAPTER, //
	@Meta(description = "Chapter identification string in English, used to identify a starting chapter in text mode",//
	format = Format.STRING, def = "Chapter")
	CHAPTER_EN, //
	@Meta(description = "Chapter identification string in French, used to identify a starting chapter in text mode",//
	format = Format.STRING, def = "Chapitre")
	CHAPTER_FR, //
	@Meta(description = "Login for YiffStar to have access to all the stories (should not be necessary anymore, but can still be used)",//
	format = Format.STRING)
	LOGIN_YIFFSTAR_USER, //
	@Meta(description = "Password for YiffStar to have access to all the stories (should not be necessary anymore, but can still be used)",//
	format = Format.PASSWORD)
	LOGIN_YIFFSTAR_PASS, //
	@Meta(description = "If the last update check was done at least that many days ago, check for updates at startup (-1 for 'no checks')", //
	format = Format.INT, def = "1")
	UPDATE_INTERVAL, //
	@Meta(description = "The proxy server to use under the format 'user:pass@proxy:port', 'user@proxy:port', 'proxy:port' or ':' alone (system proxy); an empty String means no proxy",//
	format = Format.STRING, def = "")
	USE_PROXY, //
	@Meta(description = "FimFiction APIKEY credentials\nFimFiction can be queried via an API, but requires an API key to do that. One has been created for this program, but if you have another API key you can set it here. You can also set a login and password instead, in that case, a new API key will be generated (and stored) if you still haven't set one.",//
	group = true)
	LOGIN_FIMFICTION_APIKEY, //
	@Meta(description = "The login of the API key used to create a new token from FimFiction", //
	format = Format.STRING)
	LOGIN_FIMFICTION_APIKEY_CLIENT_ID, //
	@Meta(description = "The password of the API key used to create a new token from FimFiction", //
	format = Format.PASSWORD)
	LOGIN_FIMFICTION_APIKEY_CLIENT_SECRET, //
	@Meta(description = "Do not use the new API, even if we have a token, and force HTML scraping",//
	format = Format.BOOLEAN, def = "false")
	LOGIN_FIMFICTION_APIKEY_FORCE_HTML, //
	@Meta(description = "The token required to use the beta APIv2 from FimFiction (see APIKEY_CLIENT_* if you want to generate a new one from your own API key)", //
	format = Format.PASSWORD, def = "Bearer WnZ5oHlzQoDocv1GcgHfcoqctHkSwL-D")
	LOGIN_FIMFICTION_APIKEY_TOKEN, //
}
