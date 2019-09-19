package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.BasicLibrary.Status;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.ui.GuiReaderBook.BookActionListener;
import be.nikiroo.fanfix.reader.ui.GuiReaderBookInfo.Type;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.ui.ProgressBar;

/**
 * A {@link Frame} that will show a {@link GuiReaderBook} item for each
 * {@link Story} in the main cache ({@link Instance#getCache()}), and offer a
 * way to copy them to the {@link GuiReader} cache (
 * {@link BasicReader#getLibrary()}), read them, delete them...
 * 
 * @author niki
 */
class GuiReaderMainPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private FrameHelper helper;
	private Map<String, GuiReaderGroup> books;
	private GuiReaderGroup bookPane; // for more "All"
	private JPanel pane;
	private Color color;
	private ProgressBar pgBar;
	private JMenuBar bar;
	private GuiReaderBook selectedBook;
	private boolean words; // words or authors (secondary info on books)
	private boolean currentType; // type/source or author mode (All and Listing)

	/**
	 * An object that offers some helper methods to access the frame that host
	 * it and the Fanfix-related functions.
	 * 
	 * @author niki
	 */
	public interface FrameHelper {
		/**
		 * Return the reader associated to this {@link FrameHelper}.
		 * 
		 * @return the reader
		 */
		public GuiReader getReader();

		/**
		 * Create the main menu bar.
		 * <p>
		 * Will invalidate the layout.
		 * 
		 * @param status
		 *            the library status, <b>must not</b> be NULL
		 */
		public void createMenu(Status status);

		/**
		 * Create a popup menu for a {@link GuiReaderBook} that represents a
		 * story.
		 * 
		 * @return the popup menu to display
		 */
		public JPopupMenu createBookPopup();

		/**
		 * Create a popup menu for a {@link GuiReaderBook} that represents a
		 * source/type or an author.
		 * 
		 * @return the popup menu to display
		 */
		public JPopupMenu createSourceAuthorPopup();
	}

	/**
	 * A {@link Runnable} with a {@link MetaData} parameter.
	 * 
	 * @author niki
	 */
	public interface MetaDataRunnable {
		/**
		 * Run the action.
		 * 
		 * @param meta
		 *            the meta of the story
		 */
		public void run(MetaData meta);
	}

	/**
	 * Create a new {@link GuiReaderMainPanel}.
	 * 
	 * @param parent
	 *            the associated {@link FrameHelper} to forward some commands
	 *            and access its {@link LocalLibrary}
	 * @param type
	 *            the type of {@link Story} to load, or NULL for all types
	 */
	public GuiReaderMainPanel(FrameHelper parent, String type) {
		super(new BorderLayout(), true);

		this.helper = parent;

		pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		JScrollPane scroll = new JScrollPane(pane);

		Integer icolor = Instance.getUiConfig().getColor(
				UiConfig.BACKGROUND_COLOR);
		if (icolor != null) {
			color = new Color(icolor);
			setBackground(color);
			pane.setBackground(color);
			scroll.setBackground(color);
		}

		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

		String message = parent.getReader().getLibrary().getLibraryName();
		if (!message.isEmpty()) {
			JLabel name = new JLabel(message, SwingConstants.CENTER);
			add(name, BorderLayout.NORTH);
		}

		pgBar = new ProgressBar();
		add(pgBar, BorderLayout.SOUTH);

		pgBar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pgBar.invalidate();
				pgBar.setProgress(null);
				setEnabled(true);
				validate();
			}
		});

		pgBar.addUpdateListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pgBar.invalidate();
				validate();
				repaint();
			}
		});

		books = new TreeMap<String, GuiReaderGroup>();

		addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				focus();
			}
		});

		pane.setVisible(false);
		final Progress pg = new Progress();
		final String typeF = type;
		outOfUi(pg, true, new Runnable() {
			@Override
			public void run() {
				final BasicLibrary lib = helper.getReader().getLibrary();
				final Status status = lib.getStatus();

				if (status == Status.READ_WRITE) {
					lib.refresh(pg);
				}

				inUi(new Runnable() {
					@Override
					public void run() {
						if (status.isReady()) {
							helper.createMenu(status);
							pane.setVisible(true);
							if (typeF == null) {
								try {
									addBookPane(true, false);
								} catch (IOException e) {
									error(e.getLocalizedMessage(),
											"IOException", e);
								}
							} else {
								addBookPane(typeF, true);
							}
						} else {
							helper.createMenu(status);
							validate();

							String desc = Instance.getTransGui().getStringX(
									StringIdGui.ERROR_LIB_STATUS,
									status.toString());
							if (desc == null) {
								desc = GuiReader
										.trans(StringIdGui.ERROR_LIB_STATUS);
							}

							String err = lib.getLibraryName() + "\n" + desc;
							error(err, GuiReader
									.trans(StringIdGui.TITLE_ERROR_LIBRARY),
									null);
						}
					}
				});
			}
		});
	}

	public boolean getCurrentType() {
		return currentType;
	}

	/**
	 * Add a new {@link GuiReaderGroup} on the frame to display all the
	 * sources/types or all the authors, or a listing of all the books sorted
	 * either by source or author.
	 * <p>
	 * A display of all the sources/types or all the authors will show one icon
	 * per source/type or author.
	 * <p>
	 * A listing of all the books sorted by source/type or author will display
	 * all the books.
	 * 
	 * @param type
	 *            TRUE for type/source, FALSE for author
	 * @param listMode
	 *            TRUE to get a listing of all the sources or authors, FALSE to
	 *            get one icon per source or author
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void addBookPane(boolean type, boolean listMode) throws IOException {
		this.currentType = type;
		BasicLibrary lib = helper.getReader().getLibrary();
		if (type) {
			if (!listMode) {
				addListPane(GuiReader.trans(StringIdGui.MENU_SOURCES),
						lib.getSources(), type);
			} else {
				for (String tt : lib.getSources()) {
					if (tt != null) {
						addBookPane(tt, type);
					}
				}
			}
		} else {
			if (!listMode) {
				addListPane(GuiReader.trans(StringIdGui.MENU_AUTHORS),
						lib.getAuthors(), type);
			} else {
				for (String tt : lib.getAuthors()) {
					if (tt != null) {
						addBookPane(tt, type);
					}
				}
			}
		}
	}

	/**
	 * Add a new {@link GuiReaderGroup} on the frame to display the books of the
	 * selected type or author.
	 * <p>
	 * Will invalidate the layout.
	 * 
	 * @param value
	 *            the author or the type, or NULL to get all the
	 *            authors-or-types
	 * @param type
	 *            TRUE for type/source, FALSE for author
	 */
	public void addBookPane(String value, boolean type) {
		this.currentType = type;

		GuiReaderGroup bookPane = new GuiReaderGroup(helper.getReader(), value,
				color);

		books.put(value, bookPane);

		pane.invalidate();
		pane.add(bookPane);

		bookPane.setActionListener(new BookActionListener() {
			@Override
			public void select(GuiReaderBook book) {
				selectedBook = book;
			}

			@Override
			public void popupRequested(GuiReaderBook book, Component target,
					int x, int y) {
				JPopupMenu popup = helper.createBookPopup();
				popup.show(target, x, y);
			}

			@Override
			public void action(final GuiReaderBook book) {
				openBook(book);
			}
		});

		focus();
	}

	/**
	 * Clear the pane from any book that may be present, usually prior to adding
	 * new ones.
	 * <p>
	 * Will invalidate the layout.
	 */
	public void removeBookPanes() {
		books.clear();
		pane.invalidate();
		pane.removeAll();
	}

	/**
	 * Refresh the list of {@link GuiReaderBook}s from disk.
	 * <p>
	 * Will validate the layout, as it is a "refresh" operation.
	 */
	public void refreshBooks() {
		BasicLibrary lib = helper.getReader().getLibrary();
		for (String value : books.keySet()) {
			List<GuiReaderBookInfo> infos = new ArrayList<GuiReaderBookInfo>();

			List<MetaData> metas;
			try {
				if (currentType) {
					metas = lib.getListBySource(value);
				} else {
					metas = lib.getListByAuthor(value);
				}
			} catch (IOException e) {
				error(e.getLocalizedMessage(), "IOException", e);
				metas = new ArrayList<MetaData>();
			}

			for (MetaData meta : metas) {
				infos.add(GuiReaderBookInfo.fromMeta(meta));
			}

			books.get(value).refreshBooks(infos, words);
		}

		if (bookPane != null) {
			bookPane.refreshBooks(words);
		}

		this.validate();
	}

	/**
	 * Open a {@link GuiReaderBook} item.
	 * 
	 * @param book
	 *            the {@link GuiReaderBook} to open
	 */
	public void openBook(final GuiReaderBook book) {
		final Progress pg = new Progress();
		outOfUi(pg, false, new Runnable() {
			@Override
			public void run() {
				try {
					helper.getReader().read(book.getInfo().getMeta().getLuid(),
							false, pg);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							book.setCached(true);
						}
					});
				} catch (IOException e) {
					Instance.getTraceHandler().error(e);
					error(GuiReader.trans(StringIdGui.ERROR_CANNOT_OPEN),
							GuiReader.trans(StringIdGui.TITLE_ERROR), e);
				}
			}
		});
	}
	
	/**
	 * Prefetch a {@link GuiReaderBook} item (which can be a group, in which
	 * case we prefetch all its members).
	 * 
	 * @param book
	 *            the {@link GuiReaderBook} to open
	 */
	public void prefetchBook(final GuiReaderBook book) {
		final List<String> luids = new LinkedList<String>();
		try {
			switch (book.getInfo().getType()) {
			case STORY:
				luids.add(book.getInfo().getMeta().getLuid());
				break;
			case SOURCE:
				for (MetaData meta : helper.getReader().getLibrary()
						.getListBySource(book.getInfo().getMainInfo())) {
					luids.add(meta.getLuid());
				}
				break;
			case AUTHOR:
				for (MetaData meta : helper.getReader().getLibrary()
						.getListByAuthor(book.getInfo().getMainInfo())) {
					luids.add(meta.getLuid());
				}
				break;
			}
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}

		final Progress pg = new Progress();
		pg.setMax(luids.size());

		outOfUi(pg, false, new Runnable() {
			@Override
			public void run() {
				try {
					for (String luid : luids) {
						Progress pgStep = new Progress();
						pg.addProgress(pgStep, 1);

						helper.getReader().prefetch(luid, pgStep);
					}

					// TODO: also set the green button on sources/authors?
					// requires to do the same when all stories inside are green
					if (book.getInfo().getType() == Type.STORY) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								book.setCached(true);
							}
						});
					}
				} catch (IOException e) {
					Instance.getTraceHandler().error(e);
					error(GuiReader.trans(StringIdGui.ERROR_CANNOT_OPEN),
							GuiReader.trans(StringIdGui.TITLE_ERROR), e);
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
	 * @param refreshBooks
	 *            TRUE to refresh the books after
	 * @param run
	 *            the action to run
	 */
	public void outOfUi(Progress progress, final boolean refreshBooks,
			final Runnable run) {
		final Progress pg = new Progress();
		final Progress reload = new Progress(
				GuiReader.trans(StringIdGui.PROGRESS_OUT_OF_UI_RELOAD_BOOKS));

		if (progress == null) {
			progress = new Progress();
		}

		if (refreshBooks) {
			pg.addProgress(progress, 100);
		} else {
			pg.addProgress(progress, 90);
			pg.addProgress(reload, 10);
		}

		invalidate();
		pgBar.setProgress(pg);
		validate();
		setEnabled(false);

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					run.run();
					if (refreshBooks) {
						refreshBooks();
					}
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
	 * Process the given action in the main Swing UI thread.
	 * <p>
	 * The code will make sure the current thread is the main UI thread and, if
	 * not, will switch to it before executing the runnable.
	 * <p>
	 * Synchronous operation.
	 * 
	 * @param run
	 *            the action to run
	 */
	public void inUi(final Runnable run) {
		if (EventQueue.isDispatchThread()) {
			run.run();
		} else {
			try {
				EventQueue.invokeAndWait(run);
			} catch (InterruptedException e) {
				Instance.getTraceHandler().error(e);
			} catch (InvocationTargetException e) {
				Instance.getTraceHandler().error(e);
			}
		}
	}

	/**
	 * Import a {@link Story} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * 
	 * @param askUrl
	 *            TRUE for an {@link URL}, false for a {@link File}
	 */
	public void imprt(boolean askUrl) {
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

			if (clipboard == null || !(clipboard.startsWith("http://") || //
					clipboard.startsWith("https://"))) {
				clipboard = "";
			}

			url = JOptionPane.showInputDialog(GuiReaderMainPanel.this,
					GuiReader.trans(StringIdGui.SUBTITLE_IMPORT_URL),
					GuiReader.trans(StringIdGui.TITLE_IMPORT_URL),
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
	 * @param onSuccessPgName
	 *            the name to use for the onSuccess progress bar
	 */
	public void imprt(final String url, final MetaDataRunnable onSuccess,
			String onSuccessPgName) {
		final Progress pg = new Progress();
		final Progress pgImprt = new Progress();
		final Progress pgOnSuccess = new Progress(onSuccessPgName);
		pg.addProgress(pgImprt, 95);
		pg.addProgress(pgOnSuccess, 5);

		outOfUi(pg, true, new Runnable() {
			@Override
			public void run() {
				Exception ex = null;
				MetaData meta = null;
				try {
					meta = helper.getReader().getLibrary()
							.imprt(BasicReader.getUrl(url), pgImprt);
				} catch (IOException e) {
					ex = e;
				}

				final Exception e = ex;

				final boolean ok = (e == null);

				pgOnSuccess.setProgress(0);
				if (!ok) {
					if (e instanceof UnknownHostException) {
						error(GuiReader.trans(
								StringIdGui.ERROR_URL_NOT_SUPPORTED, url),
								GuiReader.trans(StringIdGui.TITLE_ERROR), null);
					} else {
						error(GuiReader.trans(
								StringIdGui.ERROR_URL_IMPORT_FAILED, url,
								e.getMessage()), GuiReader
								.trans(StringIdGui.TITLE_ERROR), e);
					}
				} else {
					if (onSuccess != null) {
						onSuccess.run(meta);
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
	 * Enabling or disabling <b>this</b> component will also affect its
	 * children.
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

		for (GuiReaderGroup group : books.values()) {
			group.setEnabled(b);
		}
		super.setEnabled(b);
		repaint();
	}

	public void setWords(boolean words) {
		this.words = words;
	}

	public GuiReaderBook getSelectedBook() {
		return selectedBook;
	}

	public void unsetSelectedBook() {
		selectedBook = null;
	}

	private void addListPane(String name, List<String> values,
			final boolean type) {
		GuiReader reader = helper.getReader();
		BasicLibrary lib = reader.getLibrary();

		bookPane = new GuiReaderGroup(reader, name, color);

		List<GuiReaderBookInfo> infos = new ArrayList<GuiReaderBookInfo>();
		for (String value : values) {
			if (type) {
				infos.add(GuiReaderBookInfo.fromSource(lib, value));
			} else {
				infos.add(GuiReaderBookInfo.fromAuthor(lib, value));
			}
		}

		bookPane.refreshBooks(infos, words);

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
			public void popupRequested(GuiReaderBook book, Component target,
					int x, int y) {
				JPopupMenu popup = helper.createSourceAuthorPopup();
				popup.show(target, x, y);
			}

			@Override
			public void action(final GuiReaderBook book) {
				removeBookPanes();
				addBookPane(book.getInfo().getMainInfo(), type);
				refreshBooks();
			}
		});

		focus();
	}

	/**
	 * Focus the first {@link GuiReaderGroup} we find.
	 */
	private void focus() {
		GuiReaderGroup group = null;
		Map<String, GuiReaderGroup> books = this.books;
		if (books.size() > 0) {
			group = books.values().iterator().next();
		}

		if (group == null) {
			group = bookPane;
		}

		if (group != null) {
			group.requestFocusInWindow();
		}
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
				JOptionPane.showMessageDialog(GuiReaderMainPanel.this, message,
						title, JOptionPane.ERROR_MESSAGE);
			}
		});
	}
}
