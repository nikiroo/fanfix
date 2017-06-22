package be.nikiroo.fanfix.bundles;

import be.nikiroo.utils.resources.Meta;
import be.nikiroo.utils.resources.Meta.Format;

/**
 * The configuration options.
 * 
 * @author niki
 */
@SuppressWarnings("javadoc")
public enum UiConfig {
	@Meta(format = Format.DIRECTORY, info = "absolute path, $HOME variable supported, / is always accepted as dir separator", description = "The directory where to store temporary files, defaults to directory 'tmp.reader' in the conig directory (usually $HOME/.fanfix)")
	CACHE_DIR_LOCAL_READER, //
	@Meta(format = Format.COMBO_LIST, list = { "HTML", "CBZ" }, info = "One of the known output type", description = "The type of output for the Local Reader for non-images documents")
	NON_IMAGES_DOCUMENT_TYPE, //
	@Meta(format = Format.COMBO_LIST, list = { "HTML", "CBZ" }, description = "The type of output for the Local Reader for images documents")
	IMAGES_DOCUMENT_TYPE, //
	@Meta(info = "A command to start", description = "The command launched for images documents -- default to the system default for the current file type")
	IMAGES_DOCUMENT_READER, //
	@Meta(info = "A command to start", description = "The command launched for non images documents -- default to the system default for the current file type")
	NON_IMAGES_DOCUMENT_READER, //
	@Meta(format = Format.COLOR, description = "The background colour if you don't want the default system one")
	BACKGROUND_COLOR, //
}
