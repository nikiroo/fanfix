package be.nikiroo.fanfix.reader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.BasicLibrary.Status;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.reader.GuiReaderBook.BookActionListener;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.ui.ConfigEditor;
import be.nikiroo.utils.ui.ProgressBar;

/**
 * A {@link Frame} that will show a {@link GuiReaderBook} item for each
 * {@link Story} in the main cache ({@link Instance#getCache()}), and offer a
 * way to copy them to the {@link GuiReader} cache (
 * {@link BasicReader#getLibrary()}), read them, delete them...
 * 
 * @author niki
 */
class GuiReaderFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private GuiReader reader;
	private Map<GuiReaderGroup, String> booksByType;
	private Map<GuiReaderGroup, String> booksByAuthor;
	private JPanel pane;
	private Color color;
	private ProgressBar pgBar;
	private JMenuBar bar;
	private GuiReaderBook selectedBook;
	private boolean words; // words or authors (secondary info on books)

	/**
	 * A {@link Runnable} with a {@link Story} parameter.
	 * 
	 * @author niki
	 */
	private interface StoryRunnable {
		/**
		 * Run the action.
		 * 
		 * @param story
		 *            the story
		 */
		public void run(Story story);
	}

	/**
	 * Create a new {@link GuiReaderFrame}.
	 * 
	 * @param reader
	 *            the associated {@link GuiReader} to forward some commands and
	 *            access its {@link LocalLibrary}
	 * @param type
	 *            the type of {@link Story} to load, or NULL for all types
	 */
	public GuiReaderFrame(GuiReader reader, String type) {
		super(String.format("Fanfix %s Library", Version.getCurrentVersion()));

		this.reader = reader;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLayout(new BorderLayout());

		pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));

		color = Instance.getUiConfig().getColor(UiConfig.BACKGROUND_COLOR);
		if (color != null) {
			setBackground(color);
			pane.setBackground(color);
		}

		JScrollPane scroll = new JScrollPane(pane);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

		String message = reader.getLibrary().getLibraryName();
		if (!message.isEmpty()) {
			JLabel name = new JLabel(message, SwingConstants.CENTER);
			add(name, BorderLayout.NORTH);
		}

		pgBar = new ProgressBar();
		add(pgBar, BorderLayout.SOUTH);

		pgBar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				invalidate();
				pgBar.setProgress(null);
				validate();
				setEnabled(true);
			}
		});

		pgBar.addUpdateListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				invalidate();
				validate();
				repaint();
			}
		});

		booksByType = new HashMap<GuiReaderGroup, String>();
		booksByAuthor = new HashMap<GuiReaderGroup, String>();

		pane.setVisible(false);
		final Progress pg = new Progress();
		final String typeF = type;
		outOfUi(pg, new Runnable() {
			@Override
			public void run() {
				BasicLibrary lib = GuiReaderFrame.this.reader.getLibrary();
				Status status = lib.getStatus();

				if (status == Status.READY) {
					lib.refresh(pg);
					invalidate();
					setJMenuBar(createMenu(true));
					addBookPane(typeF, true);
					refreshBooks();
					validate();
					pane.setVisible(true);
				} else {
					invalidate();
					setJMenuBar(createMenu(false));
					validate();

					String err = lib.getLibraryName() + "\n";
					switch (status) {
					case INVALID:
						err += "Library not valid";
						break;

					case UNAUTORIZED:
						err += "You are not allowed to access this library";
						break;

					case UNAVAILABLE:
						err += "Library currently unavilable";
						break;

					default:
						err += "An error occured when contacting the library";
						break;
					}

					error(err, "Library error", null);
				}
			}
		});

		setVisible(true);
	}

	private void addSourcePanes() {
		// Sources -> i18n
		GuiReaderGroup bookPane = new GuiReaderGroup(reader, "Sources", color);

		List<MetaData> sources = new ArrayList<MetaData>();
		for (String source : reader.getLibrary().getSources()) {
			MetaData mSource = new MetaData();
			mSource.setLuid(null);
			mSource.setTitle(source);
			mSource.setSource(source);
			sources.add(mSource);
		}

		bookPane.refreshBooks(sources, false);

		this.invalidate();
		pane.invalidate();
		pane.add(bookPane);
		pane.validate();
		this.validate();

		bookPane.setActionListener(new BookActionListener() {
			@Override
			public void select(GuiReaderBook book) {
				selectedBook = book;
			}

			@Override
			public void popupRequested(GuiReaderBook book, MouseEvent e) {
				JPopupMenu popup = new JPopupMenu();
				popup.add(createMenuItemOpenBook());
				popup.show(e.getComponent(), e.getX(), e.getY());
			}

			@Override
			public void action(final GuiReaderBook book) {
				removeBookPanes();
				addBookPane(book.getMeta().getSource(), true);
				refreshBooks();
			}
		});
	}

	/**
	 * Add a new {@link GuiReaderGroup} on the frame to display the books of the
	 * selected type or author.
	 * 
	 * @param value
	 *            the author or the type, or NULL to get all the
	 *            authors-or-types
	 * @param type
	 *            TRUE for type, FALSE for author
	 */
	private void addBookPane(String value, boolean type) {
		if (value == null) {
			if (type) {
				if (Instance.getUiConfig().getBoolean(UiConfig.SOURCE_PAGE,
						false)) {
					addSourcePanes();
				} else {
					for (String tt : reader.getLibrary().getSources()) {
						if (tt != null) {
							addBookPane(tt, type);
						}
					}
				}
			} else {
				for (String tt : reader.getLibrary().getAuthors()) {
					if (tt != null) {
						addBookPane(tt, type);
					}
				}
			}

			return;
		}

		GuiReaderGroup bookPane = new GuiReaderGroup(reader, value, color);
		if (type) {
			booksByType.put(bookPane, value);
		} else {
			booksByAuthor.put(bookPane, value);
		}

		this.invalidate();
		pane.invalidate();
		pane.add(bookPane);
		pane.validate();
		this.validate();

		bookPane.setActionListener(new BookActionListener() {
			@Override
			public void select(GuiReaderBook book) {
				selectedBook = book;
			}

			@Override
			public void popupRequested(GuiReaderBook book, MouseEvent e) {
				JPopupMenu popup = new JPopupMenu();
				popup.add(createMenuItemOpenBook());
				popup.addSeparator();
				popup.add(createMenuItemExport());
				popup.add(createMenuItemMove(true));
				popup.add(createMenuItemSetCover());
				popup.add(createMenuItemClearCache());
				popup.add(createMenuItemRedownload());
				popup.addSeparator();
				popup.add(createMenuItemDelete());
				popup.show(e.getComponent(), e.getX(), e.getY());
			}

			@Override
			public void action(final GuiReaderBook book) {
				openBook(book);
			}
		});
	}

	private void removeBookPanes() {
		booksByType.clear();
		booksByAuthor.clear();
		pane.invalidate();
		this.invalidate();
		pane.removeAll();
		pane.validate();
		this.validate();
	}

	/**
	 * Refresh the list of {@link GuiReaderBook}s from disk.
	 * 
	 */
	private void refreshBooks() {
		for (GuiReaderGroup group : booksByType.keySet()) {
			List<MetaData> stories = reader.getLibrary().getListBySource(
					booksByType.get(group));
			group.refreshBooks(stories, words);
		}

		for (GuiReaderGroup group : booksByAuthor.keySet()) {
			List<MetaData> stories = reader.getLibrary().getListByAuthor(
					booksByAuthor.get(group));
			group.refreshBooks(stories, words);
		}

		pane.repaint();
		this.repaint();
	}

	/**
	 * Create the main menu bar.
	 * 
	 * @param libOk
	 *            the library can be queried
	 * 
	 * @return the bar
	 */
	private JMenuBar createMenu(boolean libOk) {
		bar = new JMenuBar();

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem imprt = new JMenuItem("Import URL...", KeyEvent.VK_U);
		imprt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imprt(true);
			}
		});
		JMenuItem imprtF = new JMenuItem("Import File...", KeyEvent.VK_F);
		imprtF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				imprt(false);
			}
		});
		JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				GuiReaderFrame.this.dispatchEvent(new WindowEvent(
						GuiReaderFrame.this, WindowEvent.WINDOW_CLOSING));
			}
		});

		file.add(createMenuItemOpenBook());
		file.add(createMenuItemExport());
		file.add(createMenuItemMove(libOk));
		file.addSeparator();
		file.add(imprt);
		file.add(imprtF);
		file.addSeparator();
		file.add(exit);

		bar.add(file);

		JMenu edit = new JMenu("Edit");
		edit.setMnemonic(KeyEvent.VK_E);

		edit.add(createMenuItemClearCache());
		edit.add(createMenuItemRedownload());
		edit.addSeparator();
		edit.add(createMenuItemDelete());

		bar.add(edit);

		JMenu view = new JMenu("View");
		view.setMnemonic(KeyEvent.VK_V);
		JMenuItem vauthors = new JMenuItem("Author");
		vauthors.setMnemonic(KeyEvent.VK_A);
		vauthors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				words = false;
				refreshBooks();
			}
		});
		view.add(vauthors);
		JMenuItem vwords = new JMenuItem("Word count");
		vwords.setMnemonic(KeyEvent.VK_W);
		vwords.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				words = true;
				refreshBooks();
			}
		});
		view.add(vwords);
		bar.add(view);

		JMenu sources = new JMenu("Sources");
		sources.setMnemonic(KeyEvent.VK_S);

		List<String> tt = new ArrayList<String>();
		if (libOk) {
			tt.addAll(reader.getLibrary().getSources());
		}
		tt.add(0, null);

		for (final String type : tt) {
			JMenuItem item = new JMenuItem(type == null ? "All" : type);
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					removeBookPanes();
					addBookPane(type, true);
					refreshBooks();
				}
			});
			sources.add(item);

			if (type == null) {
				sources.addSeparator();
			}
		}

		bar.add(sources);

		JMenu authors = new JMenu("Authors");
		authors.setMnemonic(KeyEvent.VK_A);

		List<String> aa = new ArrayList<String>();
		if (libOk) {
			aa.addAll(reader.getLibrary().getAuthors());
		}
		aa.add(0, null);
		for (final String author : aa) {
			JMenuItem item = new JMenuItem(author == null ? "All"
					: author.isEmpty() ? "[unknown]" : author);
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					removeBookPanes();
					addBookPane(author, false);
					refreshBooks();
				}
			});
			authors.add(item);

			if (author == null || author.isEmpty()) {
				authors.addSeparator();
			}
		}

		bar.add(authors);

		JMenu options = new JMenu("Options");
		options.setMnemonic(KeyEvent.VK_O);
		options.add(createMenuItemConfig());
		options.add(createMenuItemUiConfig());
		bar.add(options);

		return bar;
	}

	/**
	 * Create the Fanfix Configuration menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemConfig() {
		final String title = "Fanfix Configuration";
		JMenuItem item = new JMenuItem(title);
		item.setMnemonic(KeyEvent.VK_F);

		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfigEditor<Config> ed = new ConfigEditor<Config>(
						Config.class, Instance.getConfig(),
						"This is where you configure the options of the program.");
				JFrame frame = new JFrame(title);
				frame.add(ed);
				frame.setSize(800, 600);
				frame.setVisible(true);
			}
		});

		return item;
	}

	/**
	 * Create the UI Configuration menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemUiConfig() {
		final String title = "UI Configuration";
		JMenuItem item = new JMenuItem(title);
		item.setMnemonic(KeyEvent.VK_U);

		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfigEditor<UiConfig> ed = new ConfigEditor<UiConfig>(
						UiConfig.class, Instance.getUiConfig(),
						"This is where you configure the graphical appearence of the program.");
				JFrame frame = new JFrame(title);
				frame.add(ed);
				frame.setSize(800, 600);
				frame.setVisible(true);
			}
		});

		return item;
	}

	/**
	 * Create the export menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemExport() {
		final JFileChooser fc = new JFileChooser();
		fc.setAcceptAllFileFilterUsed(false);

		final Map<FileFilter, OutputType> filters = new HashMap<FileFilter, OutputType>();
		for (OutputType type : OutputType.values()) {
			String ext = type.getDefaultExtension(false);
			String desc = type.getDesc(false);

			if (ext == null || ext.isEmpty()) {
				filters.put(createAllFilter(desc), type);
			} else {
				filters.put(new FileNameExtensionFilter(desc, ext), type);
			}
		}

		// First the "ALL" filters, then, the extension filters
		for (Entry<FileFilter, OutputType> entry : filters.entrySet()) {
			if (!(entry.getKey() instanceof FileNameExtensionFilter)) {
				fc.addChoosableFileFilter(entry.getKey());
			}
		}
		for (Entry<FileFilter, OutputType> entry : filters.entrySet()) {
			if (entry.getKey() instanceof FileNameExtensionFilter) {
				fc.addChoosableFileFilter(entry.getKey());
			}
		}
		//

		JMenuItem export = new JMenuItem("Save as...", KeyEvent.VK_S);
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedBook != null) {
					fc.showDialog(GuiReaderFrame.this, "Save");
					if (fc.getSelectedFile() != null) {
						final OutputType type = filters.get(fc.getFileFilter());
						final String path = fc.getSelectedFile()
								.getAbsolutePath()
								+ type.getDefaultExtension(false);
						final Progress pg = new Progress();
						outOfUi(pg, new Runnable() {
							@Override
							public void run() {
								try {
									reader.getLibrary().export(
											selectedBook.getMeta().getLuid(),
											type, path, pg);
								} catch (IOException e) {
									Instance.getTraceHandler().error(e);
								}
							}
						});
					}
				}
			}
		});

		return export;
	}

	/**
	 * Create a {@link FileFilter} that accepts all files and return the given
	 * description.
	 * 
	 * @param desc
	 *            the description
	 * 
	 * @return the filter
	 */
	private FileFilter createAllFilter(final String desc) {
		return new FileFilter() {
			@Override
			public String getDescription() {
				return desc;
			}

			@Override
			public boolean accept(File f) {
				return true;
			}
		};
	}

	/**
	 * Create the refresh (delete cache) menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemClearCache() {
		JMenuItem refresh = new JMenuItem("Clear cache", KeyEvent.VK_C);
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedBook != null) {
					outOfUi(null, new Runnable() {
						@Override
						public void run() {
							reader.clearLocalReaderCache(selectedBook.getMeta()
									.getLuid());
							selectedBook.setCached(false);
							GuiReaderBook.clearIcon(selectedBook.getMeta());
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									selectedBook.repaint();
								}
							});
						}
					});
				}
			}
		});

		return refresh;
	}

	/**
	 * Create the delete menu item.
	 * 
	 * @param libOk
	 *            the library can be queried
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemMove(boolean libOk) {
		JMenu moveTo = new JMenu("Move to...");
		moveTo.setMnemonic(KeyEvent.VK_M);

		List<String> types = new ArrayList<String>();
		types.add(null);
		if (libOk) {
			types.addAll(reader.getLibrary().getSources());
		}

		for (String type : types) {
			JMenuItem item = new JMenuItem(type == null ? "New type..." : type);

			moveTo.add(item);
			if (type == null) {
				moveTo.addSeparator();
			}

			final String ftype = type;
			item.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (selectedBook != null) {
						String type = ftype;
						if (type == null) {
							Object rep = JOptionPane.showInputDialog(
									GuiReaderFrame.this, "Move to:",
									"Moving story",
									JOptionPane.QUESTION_MESSAGE, null, null,
									selectedBook.getMeta().getSource());

							if (rep == null) {
								return;
							}

							type = rep.toString();
						}

						final String ftype = type;
						outOfUi(null, new Runnable() {
							@Override
							public void run() {
								reader.changeType(selectedBook.getMeta()
										.getLuid(), ftype);

								selectedBook = null;

								SwingUtilities.invokeLater(new Runnable() {
									@Override
									public void run() {
										setJMenuBar(createMenu(true));
									}
								});
							}
						});
					}
				}
			});
		}

		return moveTo;
	}

	/**
	 * Create the redownload (then delete original) menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemRedownload() {
		JMenuItem refresh = new JMenuItem("Redownload", KeyEvent.VK_R);
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedBook != null) {
					final MetaData meta = selectedBook.getMeta();
					imprt(meta.getUrl(), new StoryRunnable() {
						@Override
						public void run(Story story) {
							reader.delete(meta.getLuid());
							GuiReaderFrame.this.selectedBook = null;
							MetaData newMeta = story.getMeta();
							if (!newMeta.getSource().equals(meta.getSource())) {
								reader.changeType(newMeta.getLuid(),
										meta.getSource());
							}
						}
					}, "Removing old copy");
				}
			}
		});

		return refresh;
	}

	/**
	 * Create the delete menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemDelete() {
		JMenuItem delete = new JMenuItem("Delete", KeyEvent.VK_D);
		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedBook != null) {
					outOfUi(null, new Runnable() {
						@Override
						public void run() {
							reader.delete(selectedBook.getMeta().getLuid());
							selectedBook = null;
						}
					});
				}
			}
		});

		return delete;
	}

	/**
	 * Create the open menu item for a book or a source (no LUID).
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemOpenBook() {
		JMenuItem open = new JMenuItem("Open", KeyEvent.VK_O);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedBook != null) {
					if (selectedBook.getMeta().getLuid() == null) {
						removeBookPanes();
						addBookPane(selectedBook.getMeta().getSource(), true);
						refreshBooks();
					} else {
						openBook(selectedBook);
					}
				}
			}
		});

		return open;
	}

	/**
	 * Create the SetCover menu item for a book to change the linked source
	 * cover.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemSetCover() {
		JMenuItem open = new JMenuItem("Set as cover for source", KeyEvent.VK_C);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (selectedBook != null) {
					reader.getLibrary().setSourceCover(
							selectedBook.getMeta().getSource(),
							selectedBook.getMeta().getLuid());
					MetaData source = selectedBook.getMeta().clone();
					source.setLuid(null);
					GuiReaderBook.clearIcon(source);
				}
			}
		});

		return open;
	}

	/**
	 * Open a {@link GuiReaderBook} item.
	 * 
	 * @param book
	 *            the {@link GuiReaderBook} to open
	 */
	private void openBook(final GuiReaderBook book) {
		final Progress pg = new Progress();
		outOfUi(pg, new Runnable() {
			@Override
			public void run() {
				try {
					reader.read(book.getMeta().getLuid(), pg);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							book.setCached(true);
						}
					});
				} catch (IOException e) {
					// TODO: error message?
					Instance.getTraceHandler().error(e);
				}
			}
		});
	}

	/**
	 * Process the given action out of the Swing UI thread and link the given
	 * {@link ProgressBar} to the action.
	 * <p>
	 * The code will make sure that the {@link ProgressBar} (if not NULL) is set
	 * to done when the action is done.
	 * 
	 * @param progress
	 *            the {@link ProgressBar} or NULL
	 * @param run
	 *            the action to run
	 */
	private void outOfUi(Progress progress, final Runnable run) {
		final Progress pg = new Progress();
		final Progress reload = new Progress("Reload books");
		if (progress == null) {
			progress = new Progress();
		}

		pg.addProgress(progress, 90);
		pg.addProgress(reload, 10);

		invalidate();
		pgBar.setProgress(pg);
		validate();
		setEnabled(false);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					run.run();
					refreshBooks();
				} finally {
					reload.done();
					if (!pg.isDone()) {
						// will trigger pgBar ActionListener:
						pg.done();
					}
				}
			}
		}, "outOfUi thread").start();
	}

	/**
	 * Import a {@link Story} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * 
	 * @param askUrl
	 *            TRUE for an {@link URL}, false for a {@link File}
	 */
	private void imprt(boolean askUrl) {
		JFileChooser fc = new JFileChooser();

		Object url;
		if (askUrl) {
			String clipboard = "";
			try {
				clipboard = ("" + Toolkit.getDefaultToolkit()
						.getSystemClipboard().getData(DataFlavor.stringFlavor))
						.trim();
			} catch (Exception e) {
				// No data will be handled
			}

			if (clipboard == null || !clipboard.startsWith("http")) {
				clipboard = "";
			}

			url = JOptionPane.showInputDialog(GuiReaderFrame.this,
					"url of the story to import?", "Importing from URL",
					JOptionPane.QUESTION_MESSAGE, null, null, clipboard);
		} else if (fc.showOpenDialog(this) != JFileChooser.CANCEL_OPTION) {
			url = fc.getSelectedFile().getAbsolutePath();
		} else {
			url = null;
		}

		if (url != null && !url.toString().isEmpty()) {
			imprt(url.toString(), null, null);
		}
	}

	/**
	 * Actually import the {@link Story} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * 
	 * @param url
	 *            the {@link Story} to import by {@link URL}
	 * @param onSuccess
	 *            Action to execute on success
	 */
	private void imprt(final String url, final StoryRunnable onSuccess,
			String onSuccessPgName) {
		final Progress pg = new Progress();
		final Progress pgImprt = new Progress();
		final Progress pgOnSuccess = new Progress(onSuccessPgName);
		pg.addProgress(pgImprt, 95);
		pg.addProgress(pgOnSuccess, 5);

		outOfUi(pg, new Runnable() {
			@Override
			public void run() {
				Exception ex = null;
				Story story = null;
				try {
					story = reader.getLibrary().imprt(BasicReader.getUrl(url),
							pgImprt);
				} catch (IOException e) {
					ex = e;
				}

				final Exception e = ex;

				final boolean ok = (e == null);

				pgOnSuccess.setProgress(0);
				if (!ok) {
					if (e instanceof UnknownHostException) {
						error("URL not supported: " + url, "Cannot import URL",
								null);
					} else {
						error("Failed to import " + url + ": \n"
								+ e.getMessage(), "Cannot import URL", e);
					}
				} else {
					if (onSuccess != null) {
						onSuccess.run(story);
					}
				}
				pgOnSuccess.done();
			}
		});
	}

	/**
	 * Enables or disables this component, depending on the value of the
	 * parameter <code>b</code>. An enabled component can respond to user input
	 * and generate events. Components are enabled initially by default.
	 * <p>
	 * Disabling this component will also affect its children.
	 * 
	 * @param b
	 *            If <code>true</code>, this component is enabled; otherwise
	 *            this component is disabled
	 */
	@Override
	public void setEnabled(boolean b) {
		if (bar != null) {
			bar.setEnabled(b);
		}

		for (GuiReaderGroup group : booksByType.keySet()) {
			group.setEnabled(b);
		}
		for (GuiReaderGroup group : booksByAuthor.keySet()) {
			group.setEnabled(b);
		}
		super.setEnabled(b);
		repaint();
	}

	/**
	 * Display an error message and log the linked {@link Exception}.
	 * 
	 * @param message
	 *            the message
	 * @param title
	 *            the title of the error message
	 * @param e
	 *            the exception to log if any
	 */
	private void error(final String message, final String title, Exception e) {
		Instance.getTraceHandler().error(title + ": " + message);
		if (e != null) {
			Instance.getTraceHandler().error(e);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(GuiReaderFrame.this, message,
						title, JOptionPane.ERROR_MESSAGE);
			}
		});
	}
}
