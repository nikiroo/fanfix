package be.nikiroo.fanfix;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;

import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.StringId;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.CacheLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.library.RemoteLibrary;
import be.nikiroo.fanfix.library.RemoteLibraryServer;
import be.nikiroo.fanfix.output.BasicOutput;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.CliReader;
import be.nikiroo.fanfix.searchable.BasicSearchable;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.SupportType;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.VersionCheck;
import be.nikiroo.utils.serial.server.ServerObject;

/**
 * Main program entry point.
 * 
 * @author niki
 */
public class Main {
	private enum MainAction {
		IMPORT, EXPORT, CONVERT, READ, READ_URL, LIST, HELP, START, VERSION, SERVER, STOP_SERVER, REMOTE, SET_SOURCE, SET_TITLE, SET_AUTHOR, SEARCH, SEARCH_TAG
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
	 * <li>--search: list the supported websites (where)</li>
	 * <li>--search [where] [keywords] (page [page]) (item [item]): search on
	 * the supported website and display the given results page of stories it
	 * found, or the story details if asked</li>
	 * <li>--search-tag [where]: list all the tags supported by this website</li>
	 * <li>--search-tag [index 1]... (page [page]) (item [item]): search for the
	 * given stories or subtags, tag by tag, and display information about a
	 * specific page of results or about a specific item if requested</li>
	 * <li>--list ([type]): list the stories present in the library</li>
	 * <li>--set-source [id] [new source]: change the source of the given story</li>
	 * <li>--set-title [id] [new title]: change the title of the given story</li>
	 * <li>--set-author [id] [new author]: change the author of the given story</li>
	 * <li>--version: get the version of the program</li>
	 * <li>--server: start the server mode (see config file for parameters)</li>
	 * <li>--stop-server: stop the running server on this port if any</li>
	 * <li>--remote [key] [host] [port]: use a the given remote library</li>
	 * </ul>
	 * 
	 * @param args
	 *            see method description
	 */
	public static void main(String[] args) {
		new Main().start(args);
	}

	/**
	 * Start the default handling for the application.
	 * <p>
	 * If specific actions were asked (with correct parameters), they will be
	 * forwarded to the different protected methods that you can override.
	 * <p>
	 * At the end of the method, {@link Main#exit(int)} will be called; by
	 * default, it calls {@link System#exit(int)} if the status is not 0.
	 * 
	 * @param args
	 *            the arguments received from the system
	 */
	public void start(String [] args) {
		// Only one line, but very important:
		Instance.init();

		String urlString = null;
		String luid = null;
		String sourceString = null;
		String titleString = null;
		String authorString = null;
		String chapString = null;
		String target = null;
		String key = null;
		MainAction action = MainAction.START;
		Boolean plusInfo = null;
		String host = null;
		Integer port = null;
		SupportType searchOn = null;
		String search = null;
		List<Integer> tags = new ArrayList<Integer>();
		Integer page = null;
		Integer item = null;

		boolean noMoreActions = false;

		int exitCode = 0;
		for (int i = 0; exitCode == 0 && i < args.length; i++) {
			if (args[i] == null)
				continue;

			// Action (--) handling:
			if (!noMoreActions && args[i].startsWith("--")) {
				if (args[i].equals("--")) {
					noMoreActions = true;
				} else {
					try {
						action = MainAction.valueOf(args[i].substring(2)
								.toUpperCase().replace("-", "_"));
					} catch (Exception e) {
						Instance.getInstance().getTraceHandler()
								.error(new IllegalArgumentException("Unknown action: " + args[i], e));
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
			case SET_SOURCE:
				if (luid == null) {
					luid = args[i];
				} else if (sourceString == null) {
					sourceString = args[i];
				} else {
					exitCode = 255;
				}
				break;
			case SET_TITLE:
				if (luid == null) {
					luid = args[i];
				} else if (sourceString == null) {
					titleString = args[i];
				} else {
					exitCode = 255;
				}
				break;
			case SET_AUTHOR:
				if (luid == null) {
					luid = args[i];
				} else if (sourceString == null) {
					authorString = args[i];
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
			case SEARCH:
				if (searchOn == null) {
					searchOn = SupportType.valueOfAllOkUC(args[i]);

					if (searchOn == null) {
						Instance.getInstance().getTraceHandler().error("Website not known: <" + args[i] + ">");
						exitCode = 41;
						break;
					}

					if (BasicSearchable.getSearchable(searchOn) == null) {
						Instance.getInstance().getTraceHandler().error("Website not supported: " + searchOn);
						exitCode = 42;
						break;
					}
				} else if (search == null) {
					search = args[i];
				} else if (page != null && page == -1) {
					try {
						page = Integer.parseInt(args[i]);
					} catch (Exception e) {
						page = -2;
					}
				} else if (item != null && item == -1) {
					try {
						item = Integer.parseInt(args[i]);
					} catch (Exception e) {
						item = -2;
					}
				} else if (page == null || item == null) {
					if (page == null && "page".equals(args[i])) {
						page = -1;
					} else if (item == null && "item".equals(args[i])) {
						item = -1;
					} else {
						exitCode = 255;
					}
				} else {
					exitCode = 255;
				}
				break;
			case SEARCH_TAG:
				if (searchOn == null) {
					searchOn = SupportType.valueOfAllOkUC(args[i]);

					if (searchOn == null) {
						Instance.getInstance().getTraceHandler().error("Website not known: <" + args[i] + ">");
						exitCode = 255;
					}

					if (BasicSearchable.getSearchable(searchOn) == null) {
						Instance.getInstance().getTraceHandler().error("Website not supported: " + searchOn);
						exitCode = 255;
					}
				} else if (page == null && item == null) {
					if ("page".equals(args[i])) {
						page = -1;
					} else if ("item".equals(args[i])) {
						item = -1;
					} else {
						try {
							int index = Integer.parseInt(args[i]);
							tags.add(index);
						} catch (NumberFormatException e) {
							Instance.getInstance().getTraceHandler().error("Invalid tag index: " + args[i]);
							exitCode = 255;
						}
					}
				} else if (page != null && page == -1) {
					try {
						page = Integer.parseInt(args[i]);
					} catch (Exception e) {
						page = -2;
					}
				} else if (item != null && item == -1) {
					try {
						item = Integer.parseInt(args[i]);
					} catch (Exception e) {
						item = -2;
					}
				} else if (page == null || item == null) {
					if (page == null && "page".equals(args[i])) {
						page = -1;
					} else if (item == null && "item".equals(args[i])) {
						item = -1;
					} else {
						exitCode = 255;
					}
				} else {
					exitCode = 255;
				}
				break;
			case HELP:
				exitCode = 255;
				break;
			case START:
				exitCode = 255; // not supposed to be selected by user
				break;
			case VERSION:
				exitCode = 255; // no arguments for this option
				break;
			case SERVER:
				exitCode = 255; // no arguments for this option
				break;
			case STOP_SERVER:
				exitCode = 255; // no arguments for this option
				break;
			case REMOTE:
				if (key == null) {
					key = args[i];
				} else if (host == null) {
					host = args[i];
				} else if (port == null) {
					port = Integer.parseInt(args[i]);

					BasicLibrary lib = new RemoteLibrary(key, host, port);
					lib = new CacheLibrary(
							Instance.getInstance().getRemoteDir(host), lib,
							Instance.getInstance().getUiConfig());

					Instance.getInstance().setLibrary(lib);

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

			@Override
			public void progress(Progress progress, String name) {
				int diff = progress.getProgress() - current;
				current += diff;

				if (diff <= 0)
					return;

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

		VersionCheck updates = checkUpdates();

		if (exitCode == 0) {
			switch (action) {
			case IMPORT:
				if (updates != null) {
					// we consider it read
					Instance.getInstance().setVersionChecked(); 
				}
				
				try {
					exitCode = imprt(BasicReader.getUrl(urlString), pg);
				} catch (MalformedURLException e) {
					Instance.getInstance().getTraceHandler().error(e);
					exitCode = 1;
				}
				
				break;
			case EXPORT:
				if (updates != null) {
					// we consider it read
					Instance.getInstance().setVersionChecked(); 
				}
				
				OutputType exportType = OutputType.valueOfNullOkUC(sourceString, null);
				if (exportType == null) {
					Instance.getInstance().getTraceHandler().error(new Exception(trans(StringId.OUTPUT_DESC, sourceString)));
					exitCode = 1;
					break;
				}
				
				exitCode = export(luid, exportType, target, pg);
				
				break;
			case CONVERT:
				if (updates != null) {
					// we consider it read
					Instance.getInstance().setVersionChecked(); 
				}
				
				OutputType convertType = OutputType.valueOfAllOkUC(sourceString, null);
				if (convertType == null) {
					Instance.getInstance().getTraceHandler()
							.error(new IOException(trans(StringId.ERR_BAD_OUTPUT_TYPE, sourceString)));

					exitCode = 2;
					break;
				}
				
				exitCode = convert(urlString, convertType, target,
						plusInfo == null ? false : plusInfo, pg);
				
				break;
			case LIST:
				exitCode = list(sourceString);
				break;
			case SET_SOURCE:
				try {
					Instance.getInstance().getLibrary().changeSource(luid, sourceString, pg);
				} catch (IOException e1) {
					Instance.getInstance().getTraceHandler().error(e1);
					exitCode = 21;
				}
				break;
			case SET_TITLE:
				try {
					Instance.getInstance().getLibrary().changeTitle(luid, titleString, pg);
				} catch (IOException e1) {
					Instance.getInstance().getTraceHandler().error(e1);
					exitCode = 22;
				}
				break;
			case SET_AUTHOR:
				try {
					Instance.getInstance().getLibrary().changeAuthor(luid, authorString, pg);
				} catch (IOException e1) {
					Instance.getInstance().getTraceHandler().error(e1);
					exitCode = 23;
				}
				break;
			case READ:
				if (luid == null || luid.isEmpty()) {
					syntax(false);
					exitCode = 255;
					break;
				}

				try {
					Integer chap = null;
					if (chapString != null) {
						try {
							chap = Integer.parseInt(chapString);
						} catch (NumberFormatException e) {
							Instance.getInstance().getTraceHandler().error(new IOException(
									"Chapter number cannot be parsed: " + chapString, e));
							exitCode = 2;
							break;
						}
					}
					
					BasicLibrary lib = Instance.getInstance().getLibrary();
					exitCode = read(lib.getStory(luid, null), chap);
				} catch (IOException e) {
					Instance.getInstance().getTraceHandler()
							.error(new IOException("Failed to read book", e));
					exitCode = 2;
				}

				break;
			case READ_URL:
				if (urlString == null || urlString.isEmpty()) {
					syntax(false);
					exitCode = 255;
					break;
				}

				try {
					Integer chap = null;
					if (chapString != null) {
						try {
							chap = Integer.parseInt(chapString);
						} catch (NumberFormatException e) {
							Instance.getInstance().getTraceHandler().error(new IOException(
									"Chapter number cannot be parsed: " + chapString, e));
							exitCode = 2;
							break;
						}
					}
					
					BasicSupport support = BasicSupport
							.getSupport(BasicReader.getUrl(urlString));
					if (support == null) {
						Instance.getInstance().getTraceHandler()
								.error("URL not supported: " + urlString);
						exitCode = 2;
						break;
					}

					exitCode = read(support.process(null), chap);
				} catch (IOException e) {
					Instance.getInstance().getTraceHandler()
							.error(new IOException("Failed to read book", e));
					exitCode = 2;
				}

				break;
			case SEARCH:
				page = page == null ? 1 : page;
				if (page < 0) {
					Instance.getInstance().getTraceHandler().error("Incorrect page number");
					exitCode = 255;
					break;
				}

				item = item == null ? 0 : item;
				if (item < 0) {
					Instance.getInstance().getTraceHandler().error("Incorrect item number");
					exitCode = 255;
					break;
				}

				if (searchOn == null) {
					try {
						search();
					} catch (IOException e) {
						Instance.getInstance().getTraceHandler().error(e);
						exitCode = 1;
					}
				} else if (search != null) {
					try {
						searchKeywords(searchOn, search, page, item);
					} catch (IOException e) {
						Instance.getInstance().getTraceHandler().error(e);
						exitCode = 20;
					}
				} else {
					exitCode = 255;
				}

				break;
			case SEARCH_TAG:
				if (searchOn == null) {
					exitCode = 255;
					break;
				}

				page = page == null ? 1 : page;
				if (page < 0) {
					Instance.getInstance().getTraceHandler().error("Incorrect page number");
					exitCode = 255;
					break;
				}

				item = item == null ? 0 : item;
				if (item < 0) {
					Instance.getInstance().getTraceHandler().error("Incorrect item number");
					exitCode = 255;
					break;
				}

				try {
					searchTags(searchOn, page, item,
					tags.toArray(new Integer[] {}));
				} catch (IOException e) {
					Instance.getInstance().getTraceHandler().error(e);
				}

				break;
			case HELP:
				syntax(true);
				exitCode = 0;
				break;
			case VERSION:
				if (updates != null) {
					// we consider it read
					Instance.getInstance().setVersionChecked(); 
				}
				
				System.out
						.println(String.format("Fanfix version %s"
								+ "%nhttps://github.com/nikiroo/fanfix/"
								+ "%n\tWritten by Nikiroo",
								Version.getCurrentVersion()));
				break;
			case START:
				try {
					start();
				} catch (IOException e) {
					Instance.getInstance().getTraceHandler().error(e);
					exitCode = 66;
				}
				break;
			case SERVER:
				key = Instance.getInstance().getConfig().getString(Config.SERVER_KEY);
				port = Instance.getInstance().getConfig().getInteger(Config.SERVER_PORT);
				if (port == null) {
					System.err.println("No port configured in the config file");
					exitCode = 15;
					break;
				}
				try {
					startServer(key, port);
				} catch (IOException e) {
					Instance.getInstance().getTraceHandler().error(e);
				}
				
				break;
			case STOP_SERVER:
				// Can be given via "--remote XX XX XX"
				if (key == null)
					key = Instance.getInstance().getConfig().getString(Config.SERVER_KEY);
				if (port == null)
					port = Instance.getInstance().getConfig().getInteger(Config.SERVER_PORT);

				if (port == null) {
					System.err.println("No port given nor configured in the config file");
					exitCode = 15;
					break;
				}
				try {
					stopServer(key, host, port);
				} catch (SSLException e) {
					Instance.getInstance().getTraceHandler().error(
							"Bad access key for remote library");
					exitCode = 43;
				} catch (IOException e) {
					Instance.getInstance().getTraceHandler().error(e);
					exitCode = 44;
				}

				break;
			case REMOTE:
				exitCode = 255; // should not be reachable (REMOTE -> START)
				break;
			}
		}

		try {
			Instance.getInstance().getTempFiles().close();
		} catch (IOException e) {
			Instance.getInstance().getTraceHandler().error(new IOException(
					"Cannot dispose of the temporary files", e));
		}

		if (exitCode == 255) {
			syntax(false);
		}

		exit(exitCode);
	}
	
	/**
	 * A normal invocation of the program (without parameters or at least
	 * without "action" parameters).
	 * <p>
	 * You will probably want to override that one if you offer a user
	 * interface.
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected void start() throws IOException {
		new CliReader().listBooks(null);
	}

	/**
	 * Will check if updates are available, synchronously.
	 * <p>
	 * For this, it will simply forward the call to
	 * {@link Main#checkUpdates(String)} with a value of "nikiroo/fanfix".
	 * <p>
	 * You may want to override it so you call the forward method with the right
	 * parameters (or also if you want it to be asynchronous).
	 * 
	 * @return the newer version information or NULL if nothing new
	 */
	protected VersionCheck checkUpdates() {
		return checkUpdates("nikiroo/fanfix");
	}

	/**
	 * Will check if updates are available on a specific GitHub project.
	 * <p>
	 * Will be called by {@link Main#checkUpdates()}, but if you override that
	 * one you mall call it with another project.
	 * 
	 * @param githubProject
	 *            the GitHub project, for instance "nikiroo/fanfix"
	 * 
	 * @return the newer version information or NULL if nothing new
	 */
	protected VersionCheck checkUpdates(String githubProject) {
		try {
			VersionCheck updates = VersionCheck.check(githubProject,
					Instance.getInstance().getTrans().getLocale());
			if (updates.isNewVersionAvailable()) {
				notifyUpdates(updates);
				return updates;
			}
		} catch (IOException e) {
			// Maybe no internet. Do not report any update.
		}

		return null;
	}

	/**
	 * Notify the user about available updates.
	 * <p>
	 * Will only be called when a version is available.
	 * <p>
	 * Note that you can call {@link Instance#setVersionChecked()} on it if the
	 * user has read the information (by default, it is marked read only on
	 * certain other actions).
	 * 
	 * @param updates
	 *            the new version information
	 */
	protected void notifyUpdates(VersionCheck updates) {
		// Sent to syserr so not to cause problem if one tries to capture a
		// story content in text mode
		System.err.println(
				"A new version of the program is available at https://github.com/nikiroo/fanfix/releases");
		System.err.println("");
		for (Version v : updates.getNewer()) {
			System.err.println("\tVersion " + v);
			System.err.println("\t-------------");
			System.err.println("");
			for (String it : updates.getChanges().get(v)) {
				System.err.println("\t- " + it);
			}
			System.err.println("");
		}
	}
	
	/**
	 * Import the given resource into the {@link LocalLibrary}.
	 * 
	 * @param url
	 *            the resource to import
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the exit return code (0 = success)
	 */
	protected static int imprt(URL url, Progress pg) {
		try {
			MetaData meta = Instance.getInstance().getLibrary().imprt(url, pg);
			System.out.println(meta.getLuid() + ": \"" + meta.getTitle() + "\" imported.");
		} catch (IOException e) {
			Instance.getInstance().getTraceHandler().error(e);
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
	 * @param type
	 *            the {@link OutputType} to use
	 * @param target
	 *            the target
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the exit return code (0 = success)
	 */
	protected static int export(String luid, OutputType type, String target,
			Progress pg) {
		try {
			Instance.getInstance().getLibrary().export(luid, type, target, pg);
		} catch (IOException e) {
			Instance.getInstance().getTraceHandler().error(e);
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
	protected int list(String source) {
		try {
			new CliReader().listBooks(source);
		} catch (IOException e) {
			Instance.getInstance().getTraceHandler().error(e);
			return 66;
		}

		return 0;
	}

	/**
	 * Start the current reader for this {@link Story}.
	 * 
	 * @param story
	 *            the story to read
	 * @param chap
	 *            which {@link Chapter} to read (starting at 1), or NULL to get
	 *            the {@link Story} description
	 * 
	 * @return the exit return code (0 = success)
	 */
	protected int read(Story story, Integer chap) {
		if (story != null) {
			try {
				if (chap == null) {
					new CliReader().listChapters(story);
				} else {
					new CliReader().printChapter(story, chap);
				}
			} catch (IOException e) {
				Instance.getInstance().getTraceHandler()
						.error(new IOException("Failed to read book", e));
				return 2;
			}
		} else {
			Instance.getInstance().getTraceHandler()
					.error("Cannot find book: " + story);
			return 2;
		}

		return 0;
	}

	/**
	 * Convert the {@link Story} into another format.
	 * 
	 * @param urlString
	 *            the source {@link Story} to convert
	 * @param type
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
	protected int convert(String urlString, OutputType type,
			String target, boolean infoCover, Progress pg) {
		int exitCode = 0;

		Instance.getInstance().getTraceHandler().trace("Convert: " + urlString);
		String sourceName = urlString;
		try {
			URL source = BasicReader.getUrl(urlString);
			sourceName = source.toString();
			if (sourceName.startsWith("file://")) {
				sourceName = sourceName.substring("file://".length());
			}

			try {
				BasicSupport support = BasicSupport.getSupport(source);

				if (support != null) {
					Instance.getInstance().getTraceHandler()
							.trace("Support found: " + support.getClass());
					Progress pgIn = new Progress();
					Progress pgOut = new Progress();
					if (pg != null) {
						pg.setMax(2);
						pg.addProgress(pgIn, 1);
						pg.addProgress(pgOut, 1);
					}

					Story story = support.process(pgIn);
					try {
						target = new File(target).getAbsolutePath();
						BasicOutput.getOutput(type, infoCover, infoCover)
								.process(story, target, pgOut);
					} catch (IOException e) {
						Instance.getInstance().getTraceHandler()
								.error(new IOException(
										trans(StringId.ERR_SAVING, target), e));
						exitCode = 5;
					}
				} else {
					Instance.getInstance().getTraceHandler()
							.error(new IOException(
									trans(StringId.ERR_NOT_SUPPORTED, source)));

					exitCode = 4;
				}
			} catch (IOException e) {
				Instance.getInstance().getTraceHandler().error(new IOException(
						trans(StringId.ERR_LOADING, sourceName), e));
				exitCode = 3;
			}
		} catch (MalformedURLException e) {
			Instance.getInstance().getTraceHandler().error(new IOException(trans(StringId.ERR_BAD_URL, sourceName), e));
			exitCode = 1;
		}

		return exitCode;
	}

	/**
	 * Display the correct syntax of the program to the user to stdout, or an
	 * error message if the syntax used was wrong on stderr.
	 * 
	 * @param showHelp
	 *            TRUE to show the syntax help, FALSE to show "syntax error"
	 */
	protected void syntax(boolean showHelp) {
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
	 * Starts a search operation (i.e., list the available web sites we can
	 * search on).
	 * 
	 * @throws IOException
	 *             in case of I/O errors
	 */
	protected void search() throws IOException {
		new CliReader().listSearchables();
	}

	/**
	 * Search for books by keywords on the given supported web site.
	 * 
	 * @param searchOn
	 *            the web site to search on
	 * @param search
	 *            the keyword to look for
	 * @param page
	 *            the page of results to get, or 0 to inquire about the number
	 *            of pages
	 * @param item
	 *            the index of the book we are interested by, or 0 to query
	 *            about how many books are in that page of results
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected void searchKeywords(SupportType searchOn, String search,
			int page, Integer item) throws IOException {
		new CliReader().searchBooksByKeyword(searchOn, search, page, item);
	}

	/**
	 * Search for books by tags on the given supported web site.
	 * 
	 * @param searchOn
	 *            the web site to search on
	 * @param page
	 *            the page of results to get, or 0 to inquire about the number
	 *            of pages
	 * @param item
	 *            the index of the book we are interested by, or 0 to query
	 *            about how many books are in that page of results
	 * @param tags
	 *            the tags to look for
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected void searchTags(SupportType searchOn, Integer page, Integer item,
			Integer[] tags) throws IOException {
		new CliReader().searchBooksByTag(searchOn, page, item, tags);
	}

	/**
	 * Start a Fanfix server.
	 * 
	 * @param key
	 *            the key taht will be needed to contact the Fanfix server
	 * @param port
	 *            the port on which to run
	 * 
	 * @throws IOException
	 *             in case of I/O errors
	 * @throws SSLException
	 *             when the key was not accepted
	 */
	private void startServer(String key, int port) throws IOException {
		ServerObject server = new RemoteLibraryServer(key, port);
		server.setTraceHandler(Instance.getInstance().getTraceHandler());
		server.run();
	}

	/**
	 * Stop a running Fanfix server.
	 * 
	 * @param key
	 *            the key to contact the Fanfix server
	 * @param host
	 *            the host on which it runs (NULL means localhost)
	 * @param port
	 *            the port on which it runs
	 *            
	 * @throws IOException
	 *             in case of I/O errors
	 * @throws SSLException
	 *             when the key was not accepted
	 */
	private void stopServer(
			String key, String host, Integer port)
			throws IOException, SSLException {
		new RemoteLibrary(key, host, port).exit();
	}

	/**
	 * We are done and ready to exit.
	 * <p>
	 * By default, it will call {@link System#exit(int)} if the status is not 0.
	 * 
	 * @param status
	 *            the exit status
	 */
	protected void exit(int status) {
		if (status != 0) {
			System.exit(status);
		}
	}
	
	/**
	 * Simple shortcut method to call {link Instance#getTrans()#getString()}.
	 * 
	 * @param id
	 *            the ID to translate
	 * 
	 * @return the translated result
	 */
	static private String trans(StringId id, Object... params) {
		return Instance.getInstance().getTrans().getString(id, params);
	}
}
