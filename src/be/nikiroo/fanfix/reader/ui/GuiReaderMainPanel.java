package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.BasicLibrary.Status;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.ui.GuiReaderBook.BookActionListener;
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
	private Map<GuiReaderGroup, String> booksByType;
	private Map<GuiReaderGroup, String> booksByAuthor;
	private JPanel pane;
	private Color color;
	private ProgressBar pgBar;
	private JMenuBar bar;
	private GuiReaderBook selectedBook;
	private boolean words; // words or authors (secondary info on books)

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
		 * 
		 * @param libOk
		 *            the library can be queried
		 * 
		 * @return the bar
		 */
		public void createMenu(boolean b);

		/**
		 * Create a popup menu for a {@link GuiReaderBook} that represents a
		 * story.
		 * 
		 * @return the popup menu to display
		 */
		public JPopupMenu createBookPopup();

		/**
		 * Create a popup menu for a {@link GuiReaderBook} that represents a
		 * source/type (no LUID).
		 * 
		 * @return the popup menu to display
		 */
		public JPopupMenu createSourcePopup();
	}

	/**
	 * A {@link Runnable} with a {@link Story} parameter.
	 * 
	 * @author niki
	 */
	public interface StoryRunnable {
		/**
		 * Run the action.
		 * 
		 * @param story
		 *            the story
		 */
		public void run(Story story);
	}

	/**
	 * Create a new {@link GuiReaderMainPanel}.
	 * 
	 * @param reader
	 *            the associated {@link GuiReader} to forward some commands and
	 *            access its {@link LocalLibrary}
	 * @param type
	 *            the type of {@link Story} to load, or NULL for all types
	 */
	public GuiReaderMainPanel(FrameHelper parent, String type) {
		super(new BorderLayout(), true);

		this.helper = parent;

		pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));

		Integer icolor = Instance.getUiConfig().getColor(
				UiConfig.BACKGROUND_COLOR);
		if (icolor != null) {
			color = new Color(icolor);
			setBackground(color);
			pane.setBackground(color);
		}

		JScrollPane scroll = new JScrollPane(pane);
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
				BasicLibrary lib = helper.getReader().getLibrary();
				Status status = lib.getStatus();

				if (status == Status.READY) {
					lib.refresh(pg);
					invalidate();
					helper.createMenu(true);
					if (typeF == null) {
						addBookPane(true, false);
					} else {
						addBookPane(typeF, true);
					}
					refreshBooks();
					validate();
					pane.setVisible(true);
				} else {
					invalidate();
					helper.createMenu(false);
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
						err += "Library currently unavailable";
						break;

					default:
						err += "An error occured when contacting the library";
						break;
					}

					error(err, "Library error", null);
				}
			}
		});
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
	 */
	public void addBookPane(boolean type, boolean listMode) {
		BasicLibrary lib = helper.getReader().getLibrary();
		if (type) {
			if (!listMode) {
				addListPane("Sources", lib.getSources(), type);
			} else {
				for (String tt : lib.getSources()) {
					if (tt != null) {
						addBookPane(tt, type);
					}
				}
			}
		} else {
			if (!listMode) {
				addListPane("Authors", lib.getAuthors(), type);
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
	 * 
	 * @param value
	 *            the author or the type, or NULL to get all the
	 *            authors-or-types
	 * @param type
	 *            TRUE for type/source, FALSE for author
	 * 
	 */
	public void addBookPane(String value, boolean type) {
		GuiReaderGroup bookPane = new GuiReaderGroup(helper.getReader(), value,
				color);
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
				JPopupMenu popup = helper.createBookPopup();
				popup.show(e.getComponent(), e.getX(), e.getY());
			}

			@Override
			public void action(final GuiReaderBook book) {
				openBook(book);
			}
		});
	}

	/**
	 * Clear the pane from any book that may be present, usually prior to adding
	 * new ones.
	 */
	public void removeBookPanes() {
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
	 */
	public void refreshBooks() {
		BasicLibrary lib = helper.getReader().getLibrary();
		for (GuiReaderGroup group : booksByType.keySet()) {
			List<MetaData> stories = lib
					.getListBySource(booksByType.get(group));
			group.refreshBooks(stories, words);
		}

		for (GuiReaderGroup group : booksByAuthor.keySet()) {
			List<MetaData> stories = lib.getListByAuthor(booksByAuthor
					.get(group));
			group.refreshBooks(stories, words);
		}

		pane.repaint();
		this.repaint();
	}

	/**
	 * Open a {@link GuiReaderBook} item.
	 * 
	 * @param book
	 *            the {@link GuiReaderBook} to open
	 */
	public void openBook(final GuiReaderBook book) {
		final Progress pg = new Progress();
		outOfUi(pg, new Runnable() {
			@Override
			public void run() {
				try {
					helper.getReader()
							.read(book.getMeta().getLuid(), false, pg);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							book.setCached(true);
						}
					});
				} catch (IOException e) {
					Instance.getTraceHandler().error(e);
					error("Cannot open the selected book", "Error", e);
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
	public void outOfUi(Progress progress, final Runnable run) {
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

			if (clipboard == null || !clipboard.startsWith("http")) {
				clipboard = "";
			}

			url = JOptionPane.showInputDialog(GuiReaderMainPanel.this,
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
	public void imprt(final String url, final StoryRunnable onSuccess,
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
					story = helper.getReader().getLibrary()
							.imprt(BasicReader.getUrl(url), pgImprt);
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

		for (GuiReaderGroup group : booksByType.keySet()) {
			group.setEnabled(b);
		}
		for (GuiReaderGroup group : booksByAuthor.keySet()) {
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
		// Sources -> i18n
		GuiReaderGroup bookPane = new GuiReaderGroup(helper.getReader(), name,
				color);

		List<MetaData> metas = new ArrayList<MetaData>();
		for (String source : values) {
			MetaData mSource = new MetaData();
			mSource.setLuid(null);
			mSource.setTitle(source);
			mSource.setSource(source);
			metas.add(mSource);
		}

		bookPane.refreshBooks(metas, false);

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
				JPopupMenu popup = helper.createSourcePopup();
				popup.show(e.getComponent(), e.getX(), e.getY());
			}

			@Override
			public void action(final GuiReaderBook book) {
				removeBookPanes();
				addBookPane(book.getMeta().getSource(), type);
				refreshBooks();
			}
		});
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
