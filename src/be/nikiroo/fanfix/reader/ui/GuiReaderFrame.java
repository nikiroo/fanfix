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
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.ui.GuiReaderMainPanel.FrameHelper;
import be.nikiroo.fanfix.reader.ui.GuiReaderMainPanel.StoryRunnable;
import be.nikiroo.fanfix.searchable.BasicSearchable;
import be.nikiroo.fanfix.supported.SupportType;
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

	/**
	 * The different modification actions you can use on {@link Story} items.
	 * 
	 * @author niki
	 */
	private enum ChangeAction {
		/** Change the source/type, that is, move it to another source. */
		SOURCE,
		/** Change its name. */
		TITLE,
		/** Change its author. */
		AUTHOR
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
		super(getAppTitle(reader.getLibrary().getLibraryName()));
		
		this.reader = reader;

		mainPanel = new GuiReaderMainPanel(this, type);

		setSize(800, 600);
		setLayout(new BorderLayout());
		add(mainPanel, BorderLayout.CENTER);
	}

	@Override
	public JPopupMenu createBookPopup() {
		JPopupMenu popup = new JPopupMenu();
		popup.add(createMenuItemOpenBook());
		popup.addSeparator();
		popup.add(createMenuItemExport());
		popup.add(createMenuItemMoveTo(true));
		popup.add(createMenuItemSetCoverForSource());
		popup.add(createMenuItemSetCoverForAuthor());
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
	public JPopupMenu createSourceAuthorPopup() {
		JPopupMenu popup = new JPopupMenu();
		popup.add(createMenuItemOpenBook());
		return popup;
	}

	@Override
	public void createMenu(boolean libOk) {
		invalidate();

		JMenuBar bar = new JMenuBar();

		JMenu file = new JMenu(GuiReader.trans(StringIdGui.MENU_FILE));
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem imprt = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_FILE_IMPORT_URL),
				KeyEvent.VK_U);
		imprt.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.imprt(true);
			}
		});
		JMenuItem imprtF = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_FILE_IMPORT_FILE),
				KeyEvent.VK_F);
		imprtF.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.imprt(false);
			}
		});
		JMenuItem exit = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_FILE_EXIT), KeyEvent.VK_X);
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
		file.add(createMenuItemProperties());
		file.addSeparator();
		file.add(exit);

		bar.add(file);

		JMenu edit = new JMenu(GuiReader.trans(StringIdGui.MENU_EDIT));
		edit.setMnemonic(KeyEvent.VK_E);

		edit.add(createMenuItemSetCoverForSource());
		edit.add(createMenuItemSetCoverForAuthor());
		edit.add(createMenuItemClearCache());
		edit.add(createMenuItemRedownload());
		edit.addSeparator();
		edit.add(createMenuItemDelete());

		bar.add(edit);

		JMenu search = new JMenu(GuiReader.trans(StringIdGui.MENU_SEARCH));
		search.setMnemonic(KeyEvent.VK_H);
		for (SupportType type : SupportType.values()) {
			BasicSearchable searchable = BasicSearchable.getSearchable(type);
			if (searchable != null) {
				JMenuItem searchItem = new JMenuItem(type.getSourceName());
				searchItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						// TODO: open a search window
					}
				});
				search.add(searchItem);
			}
		}
		bar.add(search);
		
		JMenu view = new JMenu(GuiReader.trans(StringIdGui.MENU_VIEW));
		view.setMnemonic(KeyEvent.VK_V);
		JMenuItem vauthors = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_VIEW_AUTHOR));
		vauthors.setMnemonic(KeyEvent.VK_A);
		vauthors.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mainPanel.setWords(false);
				mainPanel.refreshBooks();
			}
		});
		view.add(vauthors);
		JMenuItem vwords = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_VIEW_WCOUNT));
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
		JMenu sources = new JMenu(GuiReader.trans(StringIdGui.MENU_SOURCES));
		sources.setMnemonic(KeyEvent.VK_S);
		populateMenuSA(sources, groupedSources, true);
		bar.add(sources);

		Map<String, List<String>> goupedAuthors = new HashMap<String, List<String>>();
		if (libOk) {
			goupedAuthors = reader.getLibrary().getAuthorsGrouped();
		}
		JMenu authors = new JMenu(GuiReader.trans(StringIdGui.MENU_AUTHORS));
		authors.setMnemonic(KeyEvent.VK_A);
		populateMenuSA(authors, goupedAuthors, false);
		bar.add(authors);

		JMenu options = new JMenu(GuiReader.trans(StringIdGui.MENU_OPTIONS));
		options.setMnemonic(KeyEvent.VK_O);
		options.add(createMenuItemConfig());
		options.add(createMenuItemUiConfig());
		bar.add(options);

		setJMenuBar(bar);
	}

	// "" = [unknown]
	private void populateMenuSA(JMenu menu,
			Map<String, List<String>> groupedValues, boolean type) {

		// "All" and "Listing" special items first
		JMenuItem item = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_XXX_ALL_GROUPED));
		item.addActionListener(getActionOpenList(type, false));
		menu.add(item);
		item = new JMenuItem(GuiReader.trans(StringIdGui.MENU_XXX_ALL_LISTING));
		item.addActionListener(getActionOpenList(type, true));
		menu.add(item);

		menu.addSeparator();

		for (final String value : groupedValues.keySet()) {
			List<String> list = groupedValues.get(value);
			if (type && list.size() == 1 && list.get(0).isEmpty()) {
				// leaf item source/type
				item = new JMenuItem(
						value.isEmpty() ? GuiReader
								.trans(StringIdGui.MENU_AUTHORS_UNKNOWN)
								: value);
				item.addActionListener(getActionOpen(value, type));
				menu.add(item);
			} else {
				JMenu dir;
				if (!type && groupedValues.size() == 1) {
					// only one group of authors
					dir = menu;
				} else {
					dir = new JMenu(
							value.isEmpty() ? GuiReader
									.trans(StringIdGui.MENU_AUTHORS_UNKNOWN)
									: value);
				}

				for (String sub : list) {
					// " " instead of "" for the visual height
					String itemName = sub;
					if (itemName.isEmpty()) {
						itemName = type ? " " : GuiReader
								.trans(StringIdGui.MENU_AUTHORS_UNKNOWN);
					}

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
		final String title = GuiReader.trans(StringIdGui.TITLE_CONFIG);
		JMenuItem item = new JMenuItem(title);
		item.setMnemonic(KeyEvent.VK_F);

		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfigEditor<Config> ed = new ConfigEditor<Config>(
						Config.class, Instance.getConfig(), GuiReader
								.trans(StringIdGui.SUBTITLE_CONFIG));
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
		final String title = GuiReader.trans(StringIdGui.TITLE_CONFIG_UI);
		JMenuItem item = new JMenuItem(title);
		item.setMnemonic(KeyEvent.VK_U);

		item.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ConfigEditor<UiConfig> ed = new ConfigEditor<UiConfig>(
						UiConfig.class, Instance.getUiConfig(), GuiReader
								.trans(StringIdGui.SUBTITLE_CONFIG_UI));
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

		// Add the "ALL" filters first, then the others
		final Map<FileFilter, OutputType> otherFilters = new HashMap<FileFilter, OutputType>();
		for (OutputType type : OutputType.values()) {
			String ext = type.getDefaultExtension(false);
			String desc = type.getDesc(false);

			if (ext == null || ext.isEmpty()) {
				fc.addChoosableFileFilter(createAllFilter(desc));
			} else {
				otherFilters.put(new FileNameExtensionFilter(desc, ext), type);
			}
		}

		for (Entry<FileFilter, OutputType> entry : otherFilters.entrySet()) {
			fc.addChoosableFileFilter(entry.getKey());
		}
		//

		JMenuItem export = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_FILE_EXPORT), KeyEvent.VK_S);
		export.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					fc.showDialog(GuiReaderFrame.this,
							GuiReader.trans(StringIdGui.TITLE_SAVE));
					if (fc.getSelectedFile() != null) {
						final OutputType type = otherFilters.get(fc.getFileFilter());
						final String path = fc.getSelectedFile()
								.getAbsolutePath()
								+ type.getDefaultExtension(false);
						final Progress pg = new Progress();
						mainPanel.outOfUi(pg, false, new Runnable() {
							@Override
							public void run() {
								try {
									reader.getLibrary().export(
											selectedBook.getInfo().getMeta()
													.getLuid(), type, path, pg);
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
		JMenuItem refresh = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_EDIT_CLEAR_CACHE),
				KeyEvent.VK_C);
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					mainPanel.outOfUi(null, false, new Runnable() {
						@Override
						public void run() {
							reader.clearLocalReaderCache(selectedBook.getInfo()
									.getMeta().getLuid());
							selectedBook.setCached(false);
							GuiReaderCoverImager.clearIcon(selectedBook
									.getInfo());
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
		JMenu changeTo = new JMenu(
				GuiReader.trans(StringIdGui.MENU_FILE_MOVE_TO));
		changeTo.setMnemonic(KeyEvent.VK_M);

		Map<String, List<String>> groupedSources = new HashMap<String, List<String>>();
		if (libOk) {
			groupedSources = reader.getLibrary().getSourcesGrouped();
		}

		JMenuItem item = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_FILE_MOVE_TO_NEW_TYPE));
		item.addActionListener(createMoveAction(ChangeAction.SOURCE, null));
		changeTo.add(item);
		changeTo.addSeparator();

		for (final String type : groupedSources.keySet()) {
			List<String> list = groupedSources.get(type);
			if (list.size() == 1 && list.get(0).isEmpty()) {
				item = new JMenuItem(type);
				item.addActionListener(createMoveAction(ChangeAction.SOURCE,
						type));
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
					item.addActionListener(createMoveAction(
							ChangeAction.SOURCE, actualType));
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
		JMenu changeTo = new JMenu(
				GuiReader.trans(StringIdGui.MENU_FILE_SET_AUTHOR));
		changeTo.setMnemonic(KeyEvent.VK_A);

		// New author
		JMenuItem newItem = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_FILE_MOVE_TO_NEW_AUTHOR));
		changeTo.add(newItem);
		changeTo.addSeparator();
		newItem.addActionListener(createMoveAction(ChangeAction.AUTHOR, null));

		// Existing authors
		if (libOk) {
			Map<String, List<String>> groupedAuthors = reader.getLibrary()
					.getAuthorsGrouped();

			if (groupedAuthors.size() > 1) {
				for (String key : groupedAuthors.keySet()) {
					JMenu group = new JMenu(key);
					for (String value : groupedAuthors.get(key)) {
						JMenuItem item = new JMenuItem(
								value.isEmpty() ? GuiReader
										.trans(StringIdGui.MENU_AUTHORS_UNKNOWN)
										: value);
						item.addActionListener(createMoveAction(
								ChangeAction.AUTHOR, value));
						group.add(item);
					}
					changeTo.add(group);
				}
			} else if (groupedAuthors.size() == 1) {
				for (String value : groupedAuthors.values().iterator().next()) {
					JMenuItem item = new JMenuItem(
							value.isEmpty() ? GuiReader
									.trans(StringIdGui.MENU_AUTHORS_UNKNOWN)
									: value);
					item.addActionListener(createMoveAction(
							ChangeAction.AUTHOR, value));
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
		JMenuItem changeTo = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_FILE_RENAME));
		changeTo.setMnemonic(KeyEvent.VK_R);
		changeTo.addActionListener(createMoveAction(ChangeAction.TITLE, null));
		return changeTo;
	}

	private ActionListener createMoveAction(final ChangeAction what,
			final String type) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					boolean refreshRequired = false;

					if (what == ChangeAction.SOURCE) {
						refreshRequired = mainPanel.getCurrentType();
					} else if (what == ChangeAction.TITLE) {
						refreshRequired = false;
					} else if (what == ChangeAction.AUTHOR) {
						refreshRequired = !mainPanel.getCurrentType();
					}

					String changeTo = type;
					if (type == null) {
						MetaData meta = selectedBook.getInfo().getMeta();
						String init = "";
						if (what == ChangeAction.SOURCE) {
							init = meta.getSource();
						} else if (what == ChangeAction.TITLE) {
							init = meta.getTitle();
						} else if (what == ChangeAction.AUTHOR) {
							init = meta.getAuthor();
						}

						Object rep = JOptionPane.showInputDialog(
								GuiReaderFrame.this,
								GuiReader.trans(StringIdGui.SUBTITLE_MOVE_TO),
								GuiReader.trans(StringIdGui.TITLE_MOVE_TO),
								JOptionPane.QUESTION_MESSAGE, null, null, init);

						if (rep == null) {
							return;
						}

						changeTo = rep.toString();
					}

					final String fChangeTo = changeTo;
					mainPanel.outOfUi(null, refreshRequired, new Runnable() {
						@Override
						public void run() {
							String luid = selectedBook.getInfo().getMeta()
									.getLuid();
							if (what == ChangeAction.SOURCE) {
								reader.changeSource(luid, fChangeTo);
							} else if (what == ChangeAction.TITLE) {
								reader.changeTitle(luid, fChangeTo);
							} else if (what == ChangeAction.AUTHOR) {
								reader.changeAuthor(luid, fChangeTo);
							}

							mainPanel.getSelectedBook().repaint();
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
	 * Create the re-download (then delete original) menu item.
	 * 
	 * @return the item
	 */
	private JMenuItem createMenuItemRedownload() {
		JMenuItem refresh = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_EDIT_REDOWNLOAD),
				KeyEvent.VK_R);
		refresh.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					final MetaData meta = selectedBook.getInfo().getMeta();
					mainPanel.imprt(
							meta.getUrl(),
							new StoryRunnable() {
								@Override
								public void run(Story story) {
									MetaData newMeta = story.getMeta();
									if (!newMeta.getSource().equals(
											meta.getSource())) {
										reader.changeSource(newMeta.getLuid(),
												meta.getSource());
									}
								}
							},
							GuiReader
									.trans(StringIdGui.PROGRESS_CHANGE_SOURCE));
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
		JMenuItem delete = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_EDIT_DELETE), KeyEvent.VK_D);
		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null
						&& selectedBook.getInfo().getMeta() != null) {

					final MetaData meta = selectedBook.getInfo().getMeta();
					int rep = JOptionPane.showConfirmDialog(
							GuiReaderFrame.this,
							GuiReader.trans(StringIdGui.SUBTITLE_DELETE,
									meta.getLuid(), meta.getTitle()),
							GuiReader.trans(StringIdGui.TITLE_DELETE),
							JOptionPane.OK_CANCEL_OPTION);

					if (rep == JOptionPane.OK_OPTION) {
						mainPanel.outOfUi(null, true, new Runnable() {
							@Override
							public void run() {
								reader.delete(meta.getLuid());
								mainPanel.unsetSelectedBook();
							}
						});
					}
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
		JMenuItem delete = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_FILE_PROPERTIES),
				KeyEvent.VK_P);
		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					mainPanel.outOfUi(null, false, new Runnable() {
						@Override
						public void run() {
							new GuiReaderPropertiesFrame(reader.getLibrary(),
									selectedBook.getInfo().getMeta())
									.setVisible(true);
						}
					});
				}
			}
		});

		return delete;
	}

	/**
	 * Create the open menu item for a book, a source/type or an author.
	 * 
	 * @return the item
	 */
	public JMenuItem createMenuItemOpenBook() {
		JMenuItem open = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_FILE_OPEN), KeyEvent.VK_O);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					if (selectedBook.getInfo().getMeta() == null) {
						mainPanel.removeBookPanes();
						mainPanel.addBookPane(selectedBook.getInfo()
								.getMainInfo(), mainPanel.getCurrentType());
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
	private JMenuItem createMenuItemSetCoverForSource() {
		JMenuItem open = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_EDIT_SET_COVER_FOR_SOURCE),
				KeyEvent.VK_C);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					BasicLibrary lib = reader.getLibrary();
					String luid = selectedBook.getInfo().getMeta().getLuid();
					String source = selectedBook.getInfo().getMeta()
							.getSource();

					lib.setSourceCover(source, luid);

					GuiReaderBookInfo sourceInfo = GuiReaderBookInfo
							.fromSource(lib, source);
					GuiReaderCoverImager.clearIcon(sourceInfo);
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
	private JMenuItem createMenuItemSetCoverForAuthor() {
		JMenuItem open = new JMenuItem(
				GuiReader.trans(StringIdGui.MENU_EDIT_SET_COVER_FOR_AUTHOR),
				KeyEvent.VK_A);
		open.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final GuiReaderBook selectedBook = mainPanel.getSelectedBook();
				if (selectedBook != null) {
					BasicLibrary lib = reader.getLibrary();
					String luid = selectedBook.getInfo().getMeta().getLuid();
					String author = selectedBook.getInfo().getMeta()
							.getAuthor();

					lib.setAuthorCover(author, luid);

					GuiReaderBookInfo authorInfo = GuiReaderBookInfo
							.fromAuthor(lib, author);
					GuiReaderCoverImager.clearIcon(authorInfo);
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

	/**
	 * Return the title of the application.
	 * 
	 * @param libraryName
	 *            the name of the associated {@link BasicLibrary}, which can be
	 *            EMPTY
	 * 
	 * @return the title
	 */
	static private String getAppTitle(String libraryName) {
		if (!libraryName.isEmpty()) {
			return GuiReader.trans(StringIdGui.TITLE_LIBRARY_WITH_NAME, Version
					.getCurrentVersion().toString(), libraryName);
		}

		return GuiReader.trans(StringIdGui.TITLE_LIBRARY, Version
				.getCurrentVersion().toString());
	}
}
