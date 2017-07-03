package be.nikiroo.fanfix;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.library.RemoteLibrary;
import be.nikiroo.fanfix.library.RemoteLibraryServer;
import be.nikiroo.fanfix.output.BasicOutput;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.Reader;
import be.nikiroo.fanfix.reader.Reader.ReaderType;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.BasicSupport.SupportType;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.serial.Server;

/**
 * Main program entry point.
 * 
 * @author niki
 */
public class Main {
	private enum MainAction {
		IMPORT, EXPORT, CONVERT, READ, READ_URL, LIST, HELP, SET_READER, START, VERSION, SERVER, REMOTE,
	}

	/**
	 * Main program entry point.
	 * <p>
	 * Known environment variables:
	 * <ul>
	 * <li>NOUTF: if set to 1 or 'true', the program will prefer non-unicode
	 * {@link String}s when possible</li>
	 * <li>CONFIG_DIR: a path where to look for the <tt>.properties</tt> files
	 * before taking the usual ones; they will also be saved/updated into this
	 * path when the program starts</li>
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
	 * <li>--read-url [URL] ([chapter number]): convert on the fly and read the
	 * story, without saving it</li>
	 * <li>--list ([type]): list the stories present in the library</li>
	 * <li>--set-reader [reader type]: set the reader type to CLI, TUI or LOCAL
	 * for this command</li>
	 * <li>--version: get the version of the program</li>
	 * <li>--server [port]: start a server on this port</li>
	 * <li>--remote [host] [port]: use a the given remote library</li>
	 * </ul>
	 * 
	 * @param args
	 *            see method description
	 */
	public static void main(String[] args) {
		String urlString = null;
		String luid = null;
		String sourceString = null;
		String chapString = null;
		String target = null;
		MainAction action = MainAction.START;
		Boolean plusInfo = null;
		String host = null;
		Integer port = null;

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
				} else if (sourceString == null) {
					sourceString = args[i];
				} else if (target == null) {
					target = args[i];
				} else {
					exitCode = 255;
				}
				break;
			case CONVERT:
				if (urlString == null) {
					urlString = args[i];
				} else if (sourceString == null) {
					sourceString = args[i];
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
				if (sourceString == null) {
					sourceString = args[i];
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
				action = MainAction.START;
				break;
			case START:
				exitCode = 255; // not supposed to be selected by user
				break;
			case VERSION:
				exitCode = 255; // no arguments for this option
				break;
			case SERVER:
				if (port == null) {
					port = Integer.parseInt(args[i]);
				} else {
					exitCode = 255;
				}
				break;
			case REMOTE:
				if (host == null) {
					host = args[i];
				} else if (port == null) {
					port = Integer.parseInt(args[i]);
					BasicReader
							.setDefaultLibrary(new RemoteLibrary(host, port));
					action = MainAction.START;
				} else {
					exitCode = 255;
				}
				break;
			}
		}

		final Progress mainProgress = new Progress(0, 80);
		mainProgress.addProgressListener(new Progress.ProgressListener() {
			private int current = mainProgress.getMin();

			public void progress(Progress progress, String name) {
				int diff = progress.getProgress() - current;
				current += diff;

				StringBuilder builder = new StringBuilder();
				for (int i = 0; i < diff; i++) {
					builder.append('.');
				}

				System.err.print(builder.toString());

				if (progress.isDone()) {
					System.err.println("");
				}
			}
		});
		Progress pg = new Progress();
		mainProgress.addProgress(pg, mainProgress.getMax());

		VersionCheck updates = VersionCheck.check();
		if (updates.isNewVersionAvailable()) {
			// Sent to syserr so not to cause problem if one tries to capture a
			// story content in text mode
			System.err
					.println("A new version of the program is available at https://github.com/nikiroo/fanfix/releases");
			System.err.println("");
			for (Version v : updates.getNewer()) {
				System.err.println("\tVersion " + v);
				System.err.println("\t-------------");
				System.err.println("");
				for (String item : updates.getChanges().get(v)) {
					System.err.println("\t- " + item);
				}
				System.err.println("");
			}
		}

		if (exitCode != 255) {
			switch (action) {
			case IMPORT:
				exitCode = imprt(urlString, pg);
				updates.ok(); // we consider it read
				break;
			case EXPORT:
				exitCode = export(luid, sourceString, target, pg);
				updates.ok(); // we consider it read
				break;
			case CONVERT:
				exitCode = convert(urlString, sourceString, target,
						plusInfo == null ? false : plusInfo, pg);
				updates.ok(); // we consider it read
				break;
			case LIST:
				exitCode = list(sourceString);
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
				exitCode = 255;
				break;
			case VERSION:
				System.out
						.println(String.format("Fanfix version %s"
								+ "\nhttps://github.com/nikiroo/fanfix/"
								+ "\n\tWritten by Nikiroo",
								Version.getCurrentVersion()));
				updates.ok(); // we consider it read
				break;
			case START:
				BasicReader.getReader().browse(null);
				break;
			case SERVER:
				if (port == null) {
					exitCode = 255;
					break;
				}
				try {
					Server server = new RemoteLibraryServer(port);
					server.start();
					System.out.println("Remote server started on: " + port);
				} catch (IOException e) {
					Instance.syserr(e);
				}
				return;
			case REMOTE:
				exitCode = 255;
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
	 * Import the given resource into the {@link LocalLibrary}.
	 * 
	 * @param urlString
	 *            the resource to import
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the exit return code (0 = success)
	 */
	public static int imprt(String urlString, Progress pg) {
		try {
			Story story = Instance.getLibrary().imprt(
					BasicReader.getUrl(urlString), pg);
			System.out.println(story.getMeta().getLuid() + ": \""
					+ story.getMeta().getTitle() + "\" imported.");
		} catch (IOException e) {
			Instance.syserr(e);
			return 1;
		}

		return 0;
	}

	/**
	 * Export the {@link Story} from the {@link LocalLibrary} to the given
	 * target.
	 * 
	 * @param luid
	 *            the story LUID
	 * @param typeString
	 *            the {@link OutputType} to use
	 * @param target
	 *            the target
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the exit return code (0 = success)
	 */
	public static int export(String luid, String typeString, String target,
			Progress pg) {
		OutputType type = OutputType.valueOfNullOkUC(typeString);
		if (type == null) {
			Instance.syserr(new Exception(trans(StringId.OUTPUT_DESC,
					typeString)));
			return 1;
		}

		try {
			Instance.getLibrary().export(luid, type, target, pg);
		} catch (IOException e) {
			Instance.syserr(e);
			return 4;
		}

		return 0;
	}

	/**
	 * List the stories of the given source from the {@link LocalLibrary}
	 * (unless NULL is passed, in which case all stories will be listed).
	 * 
	 * @param source
	 *            the source to list the known stories of, or NULL to list all
	 *            stories
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int list(String source) {
		BasicReader.getReader().browse(source);
		return 0;
	}

	/**
	 * Start the CLI reader for this {@link Story}.
	 * 
	 * @param story
	 *            the LUID of the {@link Story} in the {@link LocalLibrary}
	 *            <b>or</b> the {@link Story} {@link URL}
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
			Reader reader = BasicReader.getReader();
			if (library) {
				reader.setStory(story, null);
			} else {
				reader.setStory(BasicReader.getUrl(story), null);
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
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the exit return code (0 = success)
	 */
	private static int convert(String urlString, String typeString,
			String target, boolean infoCover, Progress pg) {
		int exitCode = 0;

		String sourceName = urlString;
		try {
			URL source = BasicReader.getUrl(urlString);
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
						Progress pgIn = new Progress();
						Progress pgOut = new Progress();
						if (pg != null) {
							pg.setMax(2);
							pg.addProgress(pgIn, 1);
							pg.addProgress(pgOut, 1);
						}

						Story story = support.process(source, pgIn);
						try {
							target = new File(target).getAbsolutePath();
							BasicOutput.getOutput(type, infoCover).process(
									story, target, pgOut);
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
						type.getDesc(true)));
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
