package be.nikiroo.fanfix.bundles;

import be.nikiroo.utils.resources.Meta;

/**
 * The configuration options.
 * 
 * @author niki
 */
public enum UiConfig {
	@Meta(what = "directory", where = "", format = "absolute path, $HOME variable supported, / is always accepted as dir separator", info = "The directory where to store temporary files, defaults to directory 'tmp.reader' in the conig directory (usually $HOME/.fanfix)")
	CACHE_DIR_LOCAL_READER, //
	@Meta(what = "Output type", where = "Local Reader", format = "One of the known output type", info = "The type of output for the Local Reader for non-images documents")
	NON_IMAGES_DOCUMENT_TYPE, //
	@Meta(what = "Output type", where = "Local Reader", format = "One of the known output type", info = "The type of output for the Local Reader for images documents")
	IMAGES_DOCUMENT_TYPE, //
	@Meta(what = "Program", where = "Local Reader", format = "A command to start", info = "The command launched for images documents -- default to the system default for the current file type")
	IMAGES_DOCUMENT_READER, //
	@Meta(what = "Program", where = "Local Reader", format = "A command to start", info = "The command launched for non images documents -- default to the system default for the current file type")
	NON_IMAGES_DOCUMENT_READER, //
	@Meta(what = "A background colour", where = "Local Reader Frame", format = "#rrggbb", info = "The background colour if you don't want the default system one")
	BACKGROUND_COLOR, //
}
