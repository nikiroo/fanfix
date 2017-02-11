package be.nikiroo.fanfix;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.BasicSupport.SupportType;

/**
 * Main program entry point.
 * 
 * @author niki
 */
public class Main {
	/**
	 * Main program entry point.
	 * <p>
	 * Known environment variables:
	 * <ul>
	 * <li>NOUTF: if set to 1, the program will prefer non-unicode
	 * {@link String}s when possible</li>
	 * <li>CONFIG_DIR: a path where to look for the <tt>.properties</tt> files
	 * before taking the included ones; they will also be saved/updated into
	 * this path when the program starts</li>
	 * </ul>
	 * 
	 * @param args
	 *            <ol>
	 *            <li>--import [URL]: import into library</li> <li>--export [id]
	 *            [output_type] [target]: export story to target</li> <li>
	 *            --convert [URL] [output_type] [target]: convert URL into
	 *            target</li> <li>--read [id]: read the given story from the
	 *            library</li> <li>--read-url [URL]: convert on the fly and read
	 *            the story, without saving it</li> <li>--list: list the stories
	 *            present in the library</li>
	 *            </ol>
	 */
	public static void main(String[] args) {
		int exitCode = 255;

		if (args.length > 0) {
			String action = args[0];
			if (action.equals("--import")) {
				if (args.length > 1) {
					exitCode = imprt(args[1]);
				}
			} else if (action.equals("--export")) {
				if (args.length > 3) {
					exitCode = export(args[1], args[2], args[3]);
				}
			} else if (action.equals("--convert")) {
				if (args.length > 3) {
					exitCode = convert(
							args[1],
							args[2],
							args[3],
							args.length > 4 ? args[4].toLowerCase().equals(
									"+info") : false);
				}
			} else if (action.equals("--list")) {
				exitCode = list(args.length > 1 ? args[1] : null);
			} else if (action.equals("--read-url")) {
				if (args.length > 1) {
					exitCode = read(args[1], args.length > 2 ? args[2] : null,
							false);
				}
			} else if (action.equals("--read")) {
				if (args.length > 1) {
					exitCode = read(args[1], args.length > 2 ? args[2] : null,
							true);
				}
			}
		}

		if (exitCode == 255) {
			syntax();
		}

		if (exitCode != 0) {
			System.exit(exitCode);
		}
	}

	/**
	 * Return an {@link URL} from this {@link String}, be it a file path or an
	 * actual {@link URL}.
	 * 
	 * @param sourceString
	 *            the source
	 * 
	 * @return the corresponding {@link URL}
	 * 
	 * @throws MalformedURLException
	 *             if this is neither a file nor a conventional {@link URL}
	 */
	private static URL getUrl(String sourceString) throws MalformedURLException {
		if (sourceString == null || sourceString.isEmpty()) {
			throw new MalformedURLException("Empty url");
		}

		URL source = null;
		try {
			source = new URL(sourceString);
		} catch (MalformedURLException e) {
			File sourceFile = new File(sourceString);
			source = sourceFile.toURI().toURL();
		}

		return source;
	}

	/**
	 * Import the given resource into the {@link Library}.
	 * 
	 * @param sourceString
	 *            the resource to import
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int imprt(String sourceString) {
		try {
			Story story = Instance.getLibrary().imprt(getUrl(sourceString));
			System.out.println(story.getMeta().getLuid() + ": \""
					+ story.getMeta().getTitle() + "\" imported.");
		} catch (IOException e) {
			Instance.syserr(e);
			return 1;
		}

		return 0;
	}

	/**
	 * Export the {@link Story} from the {@link Library} to the given target.
	 * 
	 * @param sourceString
	 *            the story LUID
	 * @param typeString
	 *            the {@link OutputType} to use
	 * @param target
	 *            the target
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int export(String sourceString, String typeString,
			String target) {
		OutputType type = OutputType.valueOfNullOkUC(typeString);
		if (type == null) {
			Instance.syserr(new Exception(trans(StringId.OUTPUT_DESC,
					typeString)));
			return 1;
		}

		try {
			Story story = Instance.getLibrary().imprt(new URL(sourceString));
			Instance.getLibrary().export(story.getMeta().getLuid(), type,
					target);
		} catch (IOException e) {
			Instance.syserr(e);
			return 4;
		}

		return 0;
	}

	/**
	 * List the stories of the given type from the {@link Library} (unless NULL
	 * is passed, in which case all stories will be listed).
	 * 
	 * @param typeString
	 *            the {@link SupportType} to list the known stories of, or NULL
	 *            to list all stories
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int list(String typeString) {
		SupportType type = null;
		try {
			type = SupportType.valueOfNullOkUC(typeString);
		} catch (Exception e) {
			Instance.syserr(new Exception(
					trans(StringId.INPUT_DESC, typeString), e));
			return 1;
		}

		BasicReader.getReader().start(type);

		return 0;
	}

	/**
	 * Start the CLI reader for this {@link Story}.
	 * 
	 * @param story
	 *            the LUID of the {@link Story} in the {@link Library} <b>or</b>
	 *            the {@link Story} {@link URL}
	 * @param chap
	 *            which {@link Chapter} to read (starting at 1), or NULL to get
	 *            the {@link Story} description
	 * @param library
	 *            TRUE if the source is the {@link Story} LUID, FALSE if it is a
	 *            {@link URL}
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int read(String story, String chap, boolean library) {
		try {
			BasicReader reader = BasicReader.getReader();
			if (library) {
				reader.setStory(story);
			} else {
				reader.setStory(getUrl(story));
			}

			if (chap != null) {
				reader.read(Integer.parseInt(chap));
			} else {
				reader.read();
			}
		} catch (IOException e) {
			Instance.syserr(e);
			return 1;
		}

		return 0;
	}

	/**
	 * Convert the {@link Story} into another format.
	 * 
	 * @param sourceString
	 *            the source {@link Story} to convert
	 * @param typeString
	 *            the {@link OutputType} to convert to
	 * @param filename
	 *            the target file
	 * @param infoCover
	 *            TRUE to also export the cover and info file, even if the given
	 *            {@link OutputType} does not usually save them
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int convert(String sourceString, String typeString,
			String filename, boolean infoCover) {
		int exitCode = 0;

		String sourceName = sourceString;
		try {
			URL source = getUrl(sourceString);
			sourceName = source.toString();
			if (source.toString().startsWith("file://")) {
				sourceName = sourceName.substring("file://".length());
			}

			OutputType type = OutputType.valueOfAllOkUC(typeString);
			if (type == null) {
				Instance.syserr(new IOException(trans(
						StringId.ERR_BAD_OUTPUT_TYPE, typeString)));

				exitCode = 2;
			} else {
				try {
					BasicSupport support = BasicSupport.getSupport(source);
					if (support != null) {
						Story story = support.process(source);

						try {
							filename = new File(filename).getAbsolutePath();
							BasicOutput.getOutput(type, infoCover).process(
									story, filename);
						} catch (IOException e) {
							Instance.syserr(new IOException(trans(
									StringId.ERR_SAVING, filename), e));
							exitCode = 5;
						}
					} else {
						Instance.syserr(new IOException(trans(
								StringId.ERR_NOT_SUPPORTED, source)));

						exitCode = 4;
					}
				} catch (IOException e) {
					Instance.syserr(new IOException(trans(StringId.ERR_LOADING,
							sourceName), e));
					exitCode = 3;
				}
			}
		} catch (MalformedURLException e) {
			Instance.syserr(new IOException(trans(StringId.ERR_BAD_URL,
					sourceName), e));
			exitCode = 1;
		}

		return exitCode;
	}

	/**
	 * Simple shortcut method to call {link Instance#getTrans()#getString()}.
	 * 
	 * @param id
	 *            the ID to translate
	 * 
	 * @return the translated result
	 */
	private static String trans(StringId id, Object... params) {
		return Instance.getTrans().getString(id, params);
	}

	/**
	 * Display the correct syntax of the program to the user.
	 */
	private static void syntax() {
		StringBuilder builder = new StringBuilder();
		for (SupportType type : SupportType.values()) {
			builder.append(trans(StringId.ERR_SYNTAX_TYPE, type.toString(),
					type.getDesc()));
			builder.append('\n');
		}

		String typesIn = builder.toString();
		builder.setLength(0);

		for (OutputType type : OutputType.values()) {
			builder.append(trans(StringId.ERR_SYNTAX_TYPE, type.toString(),
					type.getDesc()));
			builder.append('\n');
		}

		String typesOut = builder.toString();

		System.err.println(trans(StringId.ERR_SYNTAX, typesIn, typesOut));
	}
}
