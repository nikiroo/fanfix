package be.nikiroo.fanfix.bundles;

import be.nikiroo.utils.resources.Meta;

/**
 * The configuration options.
 * 
 * @author niki
 */
public enum UiConfig {
	@Meta(what = "directory", where = "", format = "absolute path, $HOME variable supported, / is always accepted as dir separator", info = "The directory where to store temporary files, defaults to a directory 'fanfic-reader' in the system default temporary directory")
	CACHE_DIR_LOCAL_READER, //
	@Meta(what = "Output type", where = "Local Reader", format = "One of the known output type", info = "The type of output for the Local Reader for non-images documents")
	LOCAL_READER_NON_IMAGES_DOCUMENT_TYPE, //
	@Meta(what = "Output type", where = "Local Reader", format = "One of the known output type", info = "The type of output for the Local Reader for images documents")
	LOCAL_READER_IMAGES_DOCUMENT_TYPE, //
	@Meta(what = "A background colour", where = "Local Reader Frame", format = "#rrggbb", info = "The background colour if you don't want the default system one")
	BACKGROUND_COLOR, //
}
