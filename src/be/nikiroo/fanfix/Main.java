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
import be.nikiroo.fanfix.reader.BasicReader.ReaderType;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.BasicSupport.SupportType;

/**
 * Main program entry point.
 * 
 * @author niki
 */
public class Main {
	private enum MainAction {
		IMPORT, EXPORT, CONVERT, READ, READ_URL, LIST, HELP, SET_READER
	}

	/**
	 * Main program entry point.
	 * <p>
	 * Known environment variables:
	 * <ul>
	 * <li>NOUTF: if set to 1 or 'true', the program will prefer non-unicode
	 * {@link String}s when possible</li>
	 * <li>CONFIG_DIR: a path where to look for the <tt>.properties</tt> files
	 * before taking the included ones; they will also be saved/updated into
	 * this path when the program starts</li>
	 * <li>DEBUG: if set to 1 or 'true', the program will override the DEBUG_ERR
	 * configuration value with 'true'</li>
	 * </ul>
	 * <p>
	 * <ul>
	 * <li>--import [URL]: import into library</li>
	 * <li>--export [id] [output_type] [target]: export story to target</li>
	 * <li>--convert [URL] [output_type] [target] (+info): convert URL into
	 * target</li>
	 * <li>--read [id] ([chapter number]): read the given story from the library
	 * </li>
	 * <li>--read-url [URL] ([cahpter number]): convert on the fly and read the
	 * story, without saving it</li>
	 * <li>--list: list the stories present in the library</li>
	 * <li>--set-reader [reader type]: set the reader type to CLI or LOCAL for
	 * this command</li>
	 * </ul>
	 * 
	 * @param args
	 *            see method description
	 */
	public static void main(String[] args) {
		String urlString = null;
		String luid = null;
		String typeString = null;
		String chapString = null;
		String target = null;
		MainAction action = MainAction.HELP;
		Boolean plusInfo = null;
		
		boolean noMoreActions = false;

		int exitCode = 0;
		for (int i = 0; exitCode == 0 && i < args.length; i++) {
			// Action (--) handling:
			if (!noMoreActions && args[i].startsWith("--")) {
				if (args[i].equals("--")) {
					noMoreActions = true;
				} else {
					try {
						action = MainAction.valueOf(args[i].substring(2)
								.toUpperCase().replace("-", "_"));
					} catch (Exception e) {
						Instance.syserr(new IllegalArgumentException(
								"Unknown action: " + args[i], e));
						exitCode = 255;
					}
				}

				continue;
			}

			switch (action) {
			case IMPORT:
				if (urlString == null) {
					urlString = args[i];
				} else {
					exitCode = 255;
				}
				break;
			case EXPORT:
				if (luid == null) {
					luid = args[i];
				} else if (typeString == null) {
					typeString = args[i];
				} else if (target == null) {
					target = args[i];
				} else {
					exitCode = 255;
				}
				break;
			case CONVERT:
				if (urlString == null) {
					urlString = args[i];
				} else if (typeString == null) {
					typeString = args[i];
				} else if (target == null) {
					target = args[i];
				} else if (plusInfo == null) {
					if ("+info".equals(args[i])) {
						plusInfo = true;
					} else {
						exitCode = 255;
					}
				} else {
					exitCode = 255;
				}
				break;
			case LIST:
				if (typeString == null) {
					typeString = args[i];
				} else {
					exitCode = 255;
				}
				break;
			case READ:
				if (luid == null) {
					luid = args[i];
				} else if (chapString == null) {
					chapString = args[i];
				} else {
					exitCode = 255;
				}
				break;
			case READ_URL:
				if (urlString == null) {
					urlString = args[i];
				} else if (chapString == null) {
					chapString = args[i];
				} else {
					exitCode = 255;
				}
				break;
			case HELP:
				exitCode = 255;
				break;
			case SET_READER:
				exitCode = setReaderType(args[i]);
				break;
			}
		}

		if (exitCode != 255) {
			switch (action) {
			case IMPORT:
				exitCode = imprt(urlString);
				break;
			case EXPORT:
				exitCode = export(urlString, typeString, target);
				break;
			case CONVERT:
				exitCode = convert(urlString, typeString, target,
						plusInfo == null ? false : plusInfo);
				break;
			case LIST:
				exitCode = list(typeString);
				break;
			case READ:
				exitCode = read(luid, chapString, true);
				break;
			case READ_URL:
				exitCode = read(urlString, chapString, false);
				break;
			case HELP:
				syntax(true);
				exitCode = 0;
				break;
			case SET_READER:
				break;
			}
		}

		if (exitCode == 255) {
			syntax(false);
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
	 * @param urlString
	 *            the resource to import
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int imprt(String urlString) {
		try {
			Story story = Instance.getLibrary().imprt(getUrl(urlString));
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
	 * @param urlString
	 *            the story LUID
	 * @param typeString
	 *            the {@link OutputType} to use
	 * @param target
	 *            the target
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int export(String urlString, String typeString, String target) {
		OutputType type = OutputType.valueOfNullOkUC(typeString);
		if (type == null) {
			Instance.syserr(new Exception(trans(StringId.OUTPUT_DESC,
					typeString)));
			return 1;
		}

		try {
			Story story = Instance.getLibrary().imprt(new URL(urlString));
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
	 * @param chapString
	 *            which {@link Chapter} to read (starting at 1), or NULL to get
	 *            the {@link Story} description
	 * @param library
	 *            TRUE if the source is the {@link Story} LUID, FALSE if it is a
	 *            {@link URL}
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int read(String story, String chapString, boolean library) {
		try {
			BasicReader reader = BasicReader.getReader();
			if (library) {
				reader.setStory(story);
			} else {
				reader.setStory(getUrl(story));
			}

			if (chapString != null) {
				try {
					reader.read(Integer.parseInt(chapString));
				} catch (NumberFormatException e) {
					Instance.syserr(new IOException(
							"Chapter number cannot be parsed: " + chapString, e));
					return 2;
				}
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
	 * @param urlString
	 *            the source {@link Story} to convert
	 * @param typeString
	 *            the {@link OutputType} to convert to
	 * @param target
	 *            the target file
	 * @param infoCover
	 *            TRUE to also export the cover and info file, even if the given
	 *            {@link OutputType} does not usually save them
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int convert(String urlString, String typeString,
			String target, boolean infoCover) {
		int exitCode = 0;

		String sourceName = urlString;
		try {
			URL source = getUrl(urlString);
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
							target = new File(target).getAbsolutePath();
							BasicOutput.getOutput(type, infoCover).process(
									story, target);
						} catch (IOException e) {
							Instance.syserr(new IOException(trans(
									StringId.ERR_SAVING, target), e));
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
	 * Display the correct syntax of the program to the user to stdout, or an
	 * error message if the syntax used was wrong on stderr.
	 * 
	 * @param showHelp
	 *            TRUE to show the syntax help, FALSE to show "syntax error"
	 */
	private static void syntax(boolean showHelp) {
		if (showHelp) {
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

			System.out.println(trans(StringId.HELP_SYNTAX, typesIn, typesOut));
		} else {
			System.err.println(trans(StringId.ERR_SYNTAX));
		}
	}

	/**
	 * Set the default reader type for this session only (it can be changed in
	 * the configuration file, too, but this value will override it).
	 * 
	 * @param readerTypeString
	 *            the type
	 */
	private static int setReaderType(String readerTypeString) {
		try {
			ReaderType readerType = ReaderType.valueOf(readerTypeString
					.toUpperCase());
			BasicReader.setDefaultReaderType(readerType);
			return 0;
		} catch (IllegalArgumentException e) {
			Instance.syserr(new IOException("Unknown reader type: "
					+ readerTypeString, e));
			return 1;
		}
	}
}
