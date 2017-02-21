package be.nikiroo.fanfix.reader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.LocalReaderBook.BookActionListener;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.ui.ProgressBar;
import be.nikiroo.utils.ui.WrapLayout;

class LocalReaderFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private LocalReader reader;
	private List<MetaData> stories;
	private List<LocalReaderBook> books;
	private JPanel bookPane;
	private String type;
	private Color color;
	private ProgressBar pgBar;
	private JMenuBar bar;
	private LocalReaderBook selectedBook;

	public LocalReaderFrame(LocalReader reader, String type) {
		super("Fanfix Library");

		this.reader = reader;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLayout(new BorderLayout());

		books = new ArrayList<LocalReaderBook>();
		bookPane = new JPanel(new WrapLayout(WrapLayout.LEADING, 5, 5));

		color = Instance.getUiConfig().getColor(UiConfig.BACKGROUND_COLOR);

		if (color != null) {
			setBackground(color);
			bookPane.setBackground(color);
		}

		JScrollPane scroll = new JScrollPane(bookPane);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

		pgBar = new ProgressBar();
		add(pgBar, BorderLayout.SOUTH);

		refreshBooks(type);
		setJMenuBar(createMenu());

		setVisible(true);
	}

	private void refreshBooks(String type) {
		this.type = type;
		stories = Instance.getLibrary().getList(type);
		books.clear();
		bookPane.removeAll();
		for (MetaData meta : stories) {
			LocalReaderBook book = new LocalReaderBook(meta,
					reader.isCached(meta.getLuid()));
			if (color != null) {
				book.setBackground(color);
			}

			book.addMouseListener(new MouseListener() {
				public void mouseReleased(MouseEvent e) {
					if (e.isPopupTrigger())
						pop(e);
				}

				public void mousePressed(MouseEvent e) {
					if (e.isPopupTrigger())
						pop(e);
				}

				public void mouseExited(MouseEvent e) {
				}

				public void mouseEntered(MouseEvent e) {
				}

				public void mouseClicked(MouseEvent e) {
				}

				private void pop(MouseEvent e) {
					JPopupMenu popup = new JPopupMenu();
					popup.add(createMenuItemExport());
					popup.add(createMenuItemRefresh());
					popup.addSeparator();
					popup.add(createMenuItemDelete());
					// popup.show(e.getComponent(), e.getX(), e.getY());
				}
			});

			books.add(book);
			book.addActionListener(new BookActionListener() {
				public void select(LocalReaderBook book) {
					selectedBook = book;
					for (LocalReaderBook abook : books) {
						abook.setSelected(abook == book);
					}
				}

				public void popupRequested(LocalReaderBook book, MouseEvent e) {
					JPopupMenu popup = new JPopupMenu();
					popup.add(createMenuItemOpenBook());
					popup.addSeparator();
					popup.add(createMenuItemExport());
					popup.add(createMenuItemRefresh());
					popup.addSeparator();
					popup.add(createMenuItemDelete());
					popup.show(e.getComponent(), e.getX(), e.getY());
				}

				public void action(final LocalReaderBook book) {
					openBook(book);
				}
			});

			bookPane.add(book);
		}

		bookPane.validate();
		bookPane.repaint();
	}

	private JMenuBar createMenu() {
		bar = new JMenuBar();

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem imprt = new JMenuItem("Import URL", KeyEvent.VK_U);
		imprt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				imprt(true);
			}
		});
		JMenuItem imprtF = new JMenuItem("Import File", KeyEvent.VK_F);
		imprtF.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				imprt(false);
			}
		});
		JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalReaderFrame.this.dispatchEvent(new WindowEvent(
						LocalReaderFrame.this, WindowEvent.WINDOW_CLOSING));
			}
		});

		file.add(createMenuItemOpenBook());
		file.add(createMenuItemExport());
		file.addSeparator();
		file.add(imprt);
		file.add(imprtF);
		file.addSeparator();
		file.add(exit);

		bar.add(file);

		JMenu edit = new JMenu("Edit");
		edit.setMnemonic(KeyEvent.VK_E);

		edit.add(createMenuItemRefresh());
		edit.addSeparator();
		edit.add(createMenuItemDelete());

		bar.add(edit);

		JMenu view = new JMenu("View");
		view.setMnemonic(KeyEvent.VK_V);

		List<String> tt = Instance.getLibrary().getTypes();
		tt.add(0, null);
		for (final String type : tt) {

			JMenuItem item = new JMenuItem(type == null ? "All books" : type);
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					refreshBooks(type);
				}
			});
			view.add(item);

			if (type == null) {
				view.addSeparator();
			}
		}

		bar.add(view);

		return bar;
	}

	private JMenuItem createMenuItemExport() {
		// TODO
		final String notYet = "[TODO] not ready yet, but you can do it on command line, see: fanfix --help";

		JMenuItem export = new JMenuItem("Save as...", KeyEvent.VK_E);
		export.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(LocalReaderFrame.this, notYet);
			}
		});

		return export;
	}

	private JMenuItem createMenuItemRefresh() {
		JMenuItem refresh = new JMenuItem("Refresh", KeyEvent.VK_R);
		refresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectedBook != null) {
					outOfUi(null, new Runnable() {
						public void run() {
							reader.refresh(selectedBook.getLuid());
							selectedBook.setCached(false);
							SwingUtilities.invokeLater(new Runnable() {
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

	private JMenuItem createMenuItemDelete() {
		JMenuItem delete = new JMenuItem("Delete", KeyEvent.VK_D);
		delete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectedBook != null) {
					outOfUi(null, new Runnable() {
						public void run() {
							reader.delete(selectedBook.getLuid());
							selectedBook = null;
							SwingUtilities.invokeLater(new Runnable() {
								public void run() {
									refreshBooks(type);
								}
							});
						}
					});
				}
			}
		});

		return delete;
	}

	private JMenuItem createMenuItemOpenBook() {
		JMenuItem open = new JMenuItem("Open", KeyEvent.VK_O);
		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectedBook != null) {
					openBook(selectedBook);
				}
			}
		});

		return open;
	}

	private void openBook(final LocalReaderBook book) {
		final Progress pg = new Progress();
		outOfUi(pg, new Runnable() {
			public void run() {
				try {
					File target = LocalReaderFrame.this.reader.getTarget(
							book.getLuid(), pg);
					book.setCached(true);
					// TODO: allow custom programs, with
					// Desktop/xdg-open fallback
					try {
						Desktop.getDesktop().browse(target.toURI());
					} catch (UnsupportedOperationException e) {
						String browsers[] = new String[] { "xdg-open",
								"epiphany", "konqueror", "firefox", "chrome",
								"google-chrome", "mozilla" };

						Runtime runtime = Runtime.getRuntime();
						for (String browser : browsers) {
							try {
								runtime.exec(new String[] { browser,
										target.getAbsolutePath() });
								runtime = null;
								break;
							} catch (IOException ioe) {
								// continue, try next browser
							}
						}

						if (runtime != null) {
							throw new IOException(
									"Cannot find a working GUI browser...");
						}
					}
				} catch (IOException e) {
					Instance.syserr(e);
				}
			}
		});
	}

	private void outOfUi(final Progress pg, final Runnable run) {
		pgBar.setProgress(pg);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				setAllEnabled(false);
				pgBar.addActioListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						pgBar.setProgress(null);
						setAllEnabled(true);
					}
				});
			}
		});

		new Thread(new Runnable() {
			public void run() {
				run.run();
				if (pg == null) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							setAllEnabled(true);
						}
					});
				} else if (!pg.isDone()) {
					pg.setProgress(pg.getMax());
				}
			}
		}).start();
	}

	private void imprt(boolean askUrl) {
		JFileChooser fc = new JFileChooser();

		final String url;
		if (askUrl) {
			url = JOptionPane.showInputDialog(LocalReaderFrame.this,
					"url of the story to import?", "Importing from URL",
					JOptionPane.QUESTION_MESSAGE);
		} else if (fc.showOpenDialog(this) != JFileChooser.CANCEL_OPTION) {
			url = fc.getSelectedFile().getAbsolutePath();
		} else {
			url = null;
		}

		if (url != null && !url.isEmpty()) {
			final Progress pg = new Progress("Importing " + url);
			outOfUi(pg, new Runnable() {
				public void run() {
					Exception ex = null;
					try {
						Instance.getLibrary()
								.imprt(BasicReader.getUrl(url), pg);
					} catch (IOException e) {
						ex = e;
					}

					final Exception e = ex;

					final boolean ok = (e == null);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							if (!ok) {
								JOptionPane.showMessageDialog(
										LocalReaderFrame.this,
										"Cannot import: " + url,
										e.getMessage(),
										JOptionPane.ERROR_MESSAGE);

								setAllEnabled(true);
							} else {
								refreshBooks(type);
							}
						}
					});
				}
			});
		}
	}

	public void setAllEnabled(boolean enabled) {
		for (LocalReaderBook book : books) {
			book.setEnabled(enabled);
			book.validate();
			book.repaint();
		}

		bar.setEnabled(enabled);
		bookPane.setEnabled(enabled);
		bookPane.validate();
		bookPane.repaint();

		setEnabled(enabled);
		validate();
		repaint();
	}
}
