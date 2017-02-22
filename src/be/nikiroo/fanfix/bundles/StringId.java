package be.nikiroo.fanfix.bundles;

import java.io.IOException;
import java.io.Writer;

import be.nikiroo.utils.resources.Bundle;
import be.nikiroo.utils.resources.Meta;

/**
 * The {@link Enum} representing textual information to be translated to the
 * user as a key.
 * 
 * Note that each key that should be translated <b>must</b> be annotated with a
 * {@link Meta} annotation.
 * 
 * @author niki
 */
public enum StringId {
	/**
	 * A special key used for technical reasons only, without annotations so it
	 * is not visible in <tt>.properties</tt> files.
	 * <p>
	 * Use it when you need NO translation.
	 */
	NULL, //
	/**
	 * A special key used for technical reasons only, without annotations so it
	 * is not visible in <tt>.properties</tt> files.
	 * <p>
	 * Use it when you need a real translation but still don't have a key.
	 */
	DUMMY, //
	@Meta(what = "help message", where = "cli", format = "%s = supported input, %s = supported output", info = "help message for the syntax")
	HELP_SYNTAX, //
	@Meta(what = "error message", where = "cli", format = "", info = "syntax error message")
	ERR_SYNTAX, //
	@Meta(what = "error message", where = "cli", format = "%s = support name, %s = support desc", info = "an input or output support type description")
	ERR_SYNTAX_TYPE, //
	@Meta(what = "error message", where = "cli", format = "%s = input string", info = "Error when retrieving data")
	ERR_LOADING, //
	@Meta(what = "error message", where = "cli", format = "%s = save target", info = "Error when saving to given target")
	ERR_SAVING, //
	@Meta(what = "error message", where = "cli", format = "%s = bad output format", info = "Error when unknown output format")
	ERR_BAD_OUTPUT_TYPE, //
	@Meta(what = "error message", where = "cli", format = "%s = input string", info = "Error when converting input to URL/File")
	ERR_BAD_URL, //
	@Meta(what = "error message", where = "cli", format = "%s = input url", info = "URL/File not supported")
	ERR_NOT_SUPPORTED, //
	@Meta(what = "error message", where = "BasicSupport", format = "%s = cover URL", info = "Failed to download cover : %s")
	ERR_BS_NO_COVER, //
	@Meta(what = "char", where = "LaTeX/BasicSupport", format = "single char", info = "Canonical OPEN SINGLE QUOTE char (for instance: `)")
	OPEN_SINGLE_QUOTE, //
	@Meta(what = "char", where = "LaTeX/BasicSupport", format = "single char", info = "Canonical CLOSE SINGLE QUOTE char (for instance: ‘)")
	CLOSE_SINGLE_QUOTE, //
	@Meta(what = "char", where = "LaTeX/BasicSupport", format = "single char", info = "Canonical OPEN DOUBLE QUOTE char (for instance: “)")
	OPEN_DOUBLE_QUOTE, //
	@Meta(what = "char", where = "LaTeX/BasicSupport", format = "single char", info = "Canonical CLOSE DOUBLE QUOTE char (for instance: ”)")
	CLOSE_DOUBLE_QUOTE, //
	@Meta(what = "chapter name", where = "BasicSupport", format = "", info = "Name of the description fake chapter")
	DESCRIPTION, //
	@Meta(what = "chapter name", where = "", format = "%d = number, %s = name", info = "Name of a chapter with a name")
	CHAPTER_NAMED, //
	@Meta(what = "chapter name", where = "", format = "%d = number, %s = name", info = "Name of a chapter without name")
	CHAPTER_UNNAMED, //
	@Meta(what = "input format description", where = "SupportType", format = "%s = type", info = "Default description when the type is not known by i18n")
	INPUT_DESC, //
	@Meta(what = "input format description", where = "SupportType", format = "", info = "Description of this input type")
	INPUT_DESC_EPUB, //
	@Meta(what = "input format description", where = "SupportType", format = "", info = "Description of this input type")
	INPUT_DESC_TEXT, //
	@Meta(what = "input format description", where = "SupportType", format = "", info = "Description of this input type")
	INPUT_DESC_INFO_TEXT, //
	@Meta(what = "input format description", where = "SupportType", format = "", info = "Description of this input type")
	INPUT_DESC_FANFICTION, //
	@Meta(what = "input format description", where = "SupportType", format = "", info = "Description of this input type")
	INPUT_DESC_FIMFICTION, //
	@Meta(what = "input format description", where = "SupportType", format = "", info = "Description of this input type")
	INPUT_DESC_MANGAFOX, //
	@Meta(what = "input format description", where = "SupportType", format = "", info = "Description of this input type")
	INPUT_DESC_E621, //
	@Meta(what = "output format description", where = "OutputType", format = "%s = type", info = "Default description when the type is not known by i18n")
	OUTPUT_DESC, //
	@Meta(what = "output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_EPUB, //
	@Meta(what = "output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_TEXT, //
	@Meta(what = "output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_INFO_TEXT, //
	@Meta(what = "output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_CBZ, //
	@Meta(what = "output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_LATEX, //
	@Meta(what = "short output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_SYSOUT, //
	OUTPUT_DESC_SHORT, //
	@Meta(what = "short output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_SHORT_EPUB, //
	@Meta(what = "short output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_SHORT_TEXT, //
	@Meta(what = "short output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_SHORT_INFO_TEXT, //
	@Meta(what = "short output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_SHORT_CBZ, //
	@Meta(what = "short output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_SHORT_LATEX, //
	@Meta(what = "short output format description", where = "OutputType", format = "", info = "Description of this output type")
	OUTPUT_DESC_SHORT_SYSOUT, //
	@Meta(what = "error message", where = "LaTeX", format = "%s = the unknown 2-code language", info = "Error message for unknown 2-letter LaTeX language code")
	LATEX_LANG_UNKNOWN, //
	@Meta(what = "'by' prefix before author name", where = "", format = "", info = "used to output the author, make sure it is covered by Config.BYS for input detection")
	BY, //

	;

	/**
	 * Write the header found in the configuration <tt>.properties</tt> file of
	 * this {@link Bundle}.
	 * 
	 * @param writer
	 *            the {@link Writer} to write the header in
	 * @param name
	 *            the file name
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public void writeHeader(Writer writer, String name)
			throws IOException {
		writer.write("# " + name + " translation file (UTF-8)\n");
		writer.write("# \n");
		writer.write("# Note that any key can be doubled with a _NOUTF suffix\n");
		writer.write("# to use when the NOUTF env variable is set to 1\n");
		writer.write("# \n");
		writer.write("# Also, the comments always refer to the key below them.\n");
		writer.write("# \n");
	}
};
