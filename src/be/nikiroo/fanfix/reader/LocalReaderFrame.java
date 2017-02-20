package be.nikiroo.fanfix.reader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
			LocalReaderBook book = new LocalReaderBook(meta);
			if (color != null) {
				book.setBackground(color);
			}

			books.add(book);
			final String luid = meta.getLuid();
			book.addActionListener(new BookActionListener() {
				public void select(LocalReaderBook book) {
					for (LocalReaderBook abook : books) {
						abook.setSelected(abook == book);
					}
				}

				public void action(LocalReaderBook book) {
					final Progress pg = new Progress();
					outOfUi(pg, new Runnable() {
						public void run() {
							try {
								File target = LocalReaderFrame.this.reader
										.getTarget(luid, pg);
								// TODO: allow custom programs, with
								// Desktop/xdg-open fallback
								try {
									Desktop.getDesktop().browse(target.toURI());
								} catch (UnsupportedOperationException e) {
									String browsers[] = new String[] {
											"xdg-open", "epiphany",
											"konqueror", "firefox", "chrome",
											"google-chrome", "mozilla" };

									Runtime runtime = Runtime.getRuntime();
									for (String browser : browsers) {
										try {
											runtime.exec(new String[] {
													browser,
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
			});

			bookPane.add(book);
		}

		bookPane.validate();
		bookPane.repaint();
	}

	private JMenuBar createMenu() {
		bar = new JMenuBar();

		JMenu file = new JMenu("File");

		JMenuItem imprt = new JMenuItem("Import", KeyEvent.VK_I);
		imprt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				final String url = JOptionPane.showInputDialog(
						LocalReaderFrame.this, "url of the story to import?",
						"Importing from URL", JOptionPane.QUESTION_MESSAGE);
				if (url != null && !url.isEmpty()) {
					final Progress pg = new Progress("Importing " + url);
					outOfUi(pg, new Runnable() {
						public void run() {
							Exception ex = null;
							try {
								Instance.getLibrary().imprt(
										BasicReader.getUrl(url), pg);
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
		});
		JMenu types = new JMenu("Type");
		List<String> tt = Instance.getLibrary().getTypes();
		tt.add(0, null);
		for (final String type : tt) {
			JMenuItem item = new JMenuItem(type == null ? "[all]" : type);
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					refreshBooks(type);
				}
			});
			types.add(item);
		}
		JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalReaderFrame.this.dispatchEvent(new WindowEvent(
						LocalReaderFrame.this, WindowEvent.WINDOW_CLOSING));
			}
		});

		file.add(imprt);
		file.add(types);
		file.addSeparator();
		file.add(exit);

		bar.add(file);

		return bar;
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
				if (!pg.isDone()) {
					pg.setProgress(pg.getMax());
				}
			}
		}).start();
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
