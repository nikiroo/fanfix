package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.ui.GuiReaderMainPanel.FrameHelper;
import be.nikiroo.fanfix.reader.ui.GuiReaderMainPanel.StoryRunnable;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.ui.ConfigEditor;

/**
 * A {@link Frame} that will show a {@link GuiReaderBook} item for each
 * {@link Story} in the main cache ({@link Instance#getCache()}), and offer a
 * way to copy them to the {@link GuiReader} cache (
 * {@link BasicReader#getLibrary()}), read them, delete them...
 * 
 * @author niki
 */
class GuiReaderFrame extends JFrame implements FrameHelper {
	private static final long serialVersionUID = 1L;
	private GuiReader reader;
	private GuiReaderMainPanel mainPanel;

	private enum MoveAction {
		SOURCE, TITLE, AUTHOR
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

		mainPanel = new GuiReaderMainPanel(this, type);

		setSize(800, 600);
		setLayout(new BorderLayout());
		add(mainPanel);
	}

	@Override
	public JPopupMenu createBookPopup() {
		JPopupMenu popup = new JPopupMenu();
		popup.add(createMenuItemOpenBook());
		popup.addSeparator();
		popup.add(createMenuItemExport());
		popup.add(createMenuItemMoveTo(true));
		popup.add(createMenuItemSetCover());
		popup.add(createMenuItemClearCache());
		popup.add(createMenuItemRedownload());
		popup.addSeparator();
		popup.add(createMenuItemRename(true));
		popup.add(createMenuItemSetAuthor(true));
		popup.addSeparator();
		popup.add(createMenuItemDelete());
		popup.addSeparator();
		popup.add(createMenuItemProperties());
		return popup;
	}

	@Override
	public JPopupMenu createSourcePopup() {
		JPopupMenu popup = new JPopupMenu();
		popup.add(createMenuItemOpenBook());
		return popup;
	}

	@Override
	public void createMenu(boolean libOk) {
		JMenuBar bar = new JMenuBar();

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem imprt = new JMenuItem("Import URL...", KeyEvent.VK_U);
		imprt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.imprt(true);
			}
		});
		JMenuItem imprtF = new JMenuItem("Import File...", KeyEvent.VK_F);
		imprtF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.imprt(false);
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
		file.add(createMenuItemMoveTo(libOk));
		file.addSeparator();
		file.add(imprt);
		file.add(imprtF);
		file.addSeparator();
		file.add(createMenuItemRename(libOk));
		file.add(createMenuItemSetAuthor(libOk));
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
				mainPanel.setWords(false);
				mainPanel.refreshBooks();
			}
		});
		view.add(vauthors);
		JMenuItem vwords = new JMenuItem("Word count");
		vwords.setMnemonic(KeyEvent.VK_W);
		vwords.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.setWords(true);
				mainPanel.refreshBooks();
			}
		});
		view.add(vwords);
		bar.add(view);

		Map<String, List<String>> groupedSources = new HashMap<String, List<String>>();
		if (libOk) {
			groupedSources = reader.getLibrary().getSourcesGrouped();
		}
		JMenu sources = new JMenu("Sources");
		sources.setMnemonic(KeyEvent.VK_S);
		populateMenuSA(sources, groupedSources, true);
		bar.add(sources);

		Map<String, List<String>> goupedAuthors = new HashMap<String, List<String>>();
		if (libOk) {
			goupedAuthors = reader.getLibrary().getAuthorsGrouped();
		}
		JMenu authors = new JMenu("Authors");
		authors.setMnemonic(KeyEvent.VK_A);
		populateMenuSA(authors, goupedAuthors, false);
		bar.add(authors);

		JMenu options = new JMenu("Options");
		options.setMnemonic(KeyEvent.VK_O);
		options.add(createMenuItemConfig());
		options.add(createMenuItemUiConfig());
		bar.add(options);

		setJMenuBar(bar);
	}

	// "" = [unknown]
	private void populateMenuSA(JMenu menu,
			Map<String, List<String>> groupedValues, boolean type) {

		// "All" and "Listing" special items
		JMenuItem item = new JMenuItem("All");
		item.addActionListener(getActionOpenList(type, false));
		menu.add(item);
		item = new JMenuItem("Listing");
		item.addActionListener(getActionOpenList(type, true));
		menu.add(item);
		menu.addSeparator();

		for (final String value : groupedValues.keySet()) {
			List<String> list = groupedValues.get(value);
			if (type && list.size() == 1 && list.get(0).isEmpty()) {
				// leaf item source/type
				item = new JMenuItem(value.isEmpty() ? "[unknown]" : value);
				item.addActionListener(getActionOpen(value, type));
				menu.add(item);
			} else {
				JMenu dir;
				if (!type && groupedValues.size() == 1) {
					// only one group of authors
					dir = menu;
				} else {
					dir = new JMenu(value.isEmpty() ? "[unknown]" : value);
				}

				for (String sub : list) {
					// " " instead of "" for the visual height
					String itemName = sub.isEmpty() ? " " : sub;
					String actualValue = value;

					if (type) {
						if (!sub.isEmpty()) {
							actualValue += "/" + sub;
						}
					} else {
						actualValue = sub;
					}

					item = new JMenuItem(itemName);
					item.addActionListener(getActionOpen(actualValue, type));
					dir.add(item);
				}

				if (menu != dir) {
					menu.add(dir);
				}
			}
		}
	}

	/**
	 * Return an {@link ActionListener} that will set the given source (type) as
	 * the selected/displayed one.
	 * 
	 * @param type
	 *            the type (source) to select, cannot be NULL
	 * 
	 * @return the {@link ActionListener}
	 */
	private ActionListener getActionOpen(final String source, final boolean type) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.removeBookPanes();
				mainPanel.addBookPane(source, type);
				mainPanel.refreshBooks();
			}
		};
	}

	private ActionListener getActionOpenList(final boolean type,
			final boolean listMode) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.removeBookPanes();
				mainPanel.addBookPane(type, listMode);
				mainPanel.refreshBooks();
			}
		};
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
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					fc.showDialog(GuiReaderFrame.this, "Save");
					if (fc.getSelectedFile() != null) {
						final OutputType type = filters.get(fc.getFileFilter());
						final String path = fc.getSelectedFile()
								.getAbsolutePath()
								+ type.getDefaultExtension(false);
						final Progress pg = new Progress();
						mainPanel.outOfUi(pg, new Runnable() {
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
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					mainPanel.outOfUi(null, new Runnable() {
						@Override
						public void run() {
							reader.clearLocalReaderCache(selectedBook.getMeta()
									.getLuid());
							selectedBook.setCached(false);
							GuiReaderCoverImager.clearIcon(selectedBook
									.getMeta());
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
	 * Create the "move to" menu item.
	 * 
	 * @param libOk
	 *            the library can be queried
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemMoveTo(boolean libOk) {
		JMenu changeTo = new JMenu("Move to");
		changeTo.setMnemonic(KeyEvent.VK_M);

		Map<String, List<String>> groupedSources = new HashMap<String, List<String>>();
		if (libOk) {
			groupedSources = reader.getLibrary().getSourcesGrouped();
		}

		JMenuItem item = new JMenuItem("New type...");
		item.addActionListener(createMoveAction(MoveAction.SOURCE, null));
		changeTo.add(item);
		changeTo.addSeparator();

		for (final String type : groupedSources.keySet()) {
			List<String> list = groupedSources.get(type);
			if (list.size() == 1 && list.get(0).isEmpty()) {
				item = new JMenuItem(type);
				item.addActionListener(createMoveAction(MoveAction.SOURCE, type));
				changeTo.add(item);
			} else {
				JMenu dir = new JMenu(type);
				for (String sub : list) {
					// " " instead of "" for the visual height
					String itemName = sub.isEmpty() ? " " : sub;
					String actualType = type;
					if (!sub.isEmpty()) {
						actualType += "/" + sub;
					}

					item = new JMenuItem(itemName);
					item.addActionListener(createMoveAction(MoveAction.SOURCE,
							actualType));
					dir.add(item);
				}
				changeTo.add(dir);
			}
		}

		return changeTo;
	}

	/**
	 * Create the "set author" menu item.
	 * 
	 * @param libOk
	 *            the library can be queried
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemSetAuthor(boolean libOk) {
		JMenu changeTo = new JMenu("Set author");
		changeTo.setMnemonic(KeyEvent.VK_A);

		// New author
		JMenuItem newItem = new JMenuItem("New author...");
		changeTo.add(newItem);
		changeTo.addSeparator();
		newItem.addActionListener(createMoveAction(MoveAction.AUTHOR, null));

		// Existing authors
		if (libOk) {
			Map<String, List<String>> groupedAuthors = reader.getLibrary()
					.getAuthorsGrouped();

			if (groupedAuthors.size() > 1) {
				for (String key : groupedAuthors.keySet()) {
					JMenu group = new JMenu(key);
					for (String value : groupedAuthors.get(key)) {
						JMenuItem item = new JMenuItem(value);
						item.addActionListener(createMoveAction(
								MoveAction.AUTHOR, value));
						group.add(item);
					}
					changeTo.add(group);
				}
			} else if (groupedAuthors.size() == 1) {
				for (String value : groupedAuthors.values().iterator().next()) {
					JMenuItem item = new JMenuItem(value);
					item.addActionListener(createMoveAction(MoveAction.AUTHOR,
							value));
					changeTo.add(item);
				}
			}
		}

		return changeTo;
	}

	/**
	 * Create the "rename" menu item.
	 * 
	 * @param libOk
	 *            the library can be queried
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemRename(
			@SuppressWarnings("unused") boolean libOk) {
		JMenuItem changeTo = new JMenuItem("Rename...");
		changeTo.setMnemonic(KeyEvent.VK_R);
		changeTo.addActionListener(createMoveAction(MoveAction.TITLE, null));
		return changeTo;
	}

	private ActionListener createMoveAction(final MoveAction what,
			final String type) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					String changeTo = type;
					if (type == null) {
						String init = "";
						if (what == MoveAction.SOURCE) {
							init = selectedBook.getMeta().getSource();
						} else if (what == MoveAction.TITLE) {
							init = selectedBook.getMeta().getTitle();
						} else if (what == MoveAction.AUTHOR) {
							init = selectedBook.getMeta().getAuthor();
						}

						Object rep = JOptionPane.showInputDialog(
								GuiReaderFrame.this, "Move to:",
								"Moving story", JOptionPane.QUESTION_MESSAGE,
								null, null, init);

						if (rep == null) {
							return;
						}

						changeTo = rep.toString();
					}

					final String fChangeTo = changeTo;
					mainPanel.outOfUi(null, new Runnable() {
						@Override
						public void run() {
							if (what.equals("SOURCE")) {
								reader.changeSource(selectedBook.getMeta()
										.getLuid(), fChangeTo);
							} else if (what.equals("TITLE")) {
								reader.changeTitle(selectedBook.getMeta()
										.getLuid(), fChangeTo);
							} else if (what.equals("AUTHOR")) {
								reader.changeAuthor(selectedBook.getMeta()
										.getLuid(), fChangeTo);
							}

							mainPanel.unsetSelectedBook();

							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									createMenu(true);
								}
							});
						}
					});
				}
			}
		};
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
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					final MetaData meta = selectedBook.getMeta();
					mainPanel.imprt(meta.getUrl(), new StoryRunnable() {
						@Override
						public void run(Story story) {
							reader.delete(meta.getLuid());
							mainPanel.unsetSelectedBook();
							MetaData newMeta = story.getMeta();
							if (!newMeta.getSource().equals(meta.getSource())) {
								reader.changeSource(newMeta.getLuid(),
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
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					mainPanel.outOfUi(null, new Runnable() {
						@Override
						public void run() {
							reader.delete(selectedBook.getMeta().getLuid());
							mainPanel.unsetSelectedBook();
						}
					});
				}
			}
		});

		return delete;
	}

	/**
	 * Create the properties menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemProperties() {
		JMenuItem delete = new JMenuItem("Properties", KeyEvent.VK_P);
		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					mainPanel.outOfUi(null, new Runnable() {
						@Override
						public void run() {
							new GuiReaderPropertiesFrame(reader, selectedBook
									.getMeta()).setVisible(true);
						}
					});
				}
			}
		});

		return delete;
	}

	/**
	 * Create the open menu item for a book or a source/type (no LUID).
	 * 
	 * @return the item
	 */
	public JMenuItem createMenuItemOpenBook() {
		JMenuItem open = new JMenuItem("Open", KeyEvent.VK_O);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					if (selectedBook.getMeta().getLuid() == null) {
						mainPanel.removeBookPanes();
						mainPanel.addBookPane(selectedBook.getMeta()
								.getSource(), true);
						mainPanel.refreshBooks();
					} else {
						mainPanel.openBook(selectedBook);
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
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					reader.getLibrary().setSourceCover(
							selectedBook.getMeta().getSource(),
							selectedBook.getMeta().getLuid());
					MetaData source = selectedBook.getMeta().clone();
					source.setLuid(null);
					GuiReaderCoverImager.clearIcon(source);
				}
			}
		});

		return open;
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
	public void error(final String message, final String title, Exception e) {
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

	@Override
	public GuiReader getReader() {
		return reader;
	}
}
