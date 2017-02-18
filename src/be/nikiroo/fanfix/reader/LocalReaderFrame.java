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

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.Main;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.LocalReaderBook.BookActionListener;
import be.nikiroo.utils.ui.WrapLayout;

class LocalReaderFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private LocalReader reader;
	private List<MetaData> stories;
	private List<LocalReaderBook> books;
	private JPanel bookPane;
	private String type;
	private Color color;

	public LocalReaderFrame(LocalReader reader, String type) {
		super("Fanfix Library");

		this.reader = reader;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLayout(new BorderLayout());

		books = new ArrayList<LocalReaderBook>();
		bookPane = new JPanel(new WrapLayout(WrapLayout.LEADING, 5, 5));

		color = null;
		String bg = Instance.getUiConfig().getString(UiConfig.BACKGROUND_COLOR);
		if (bg.startsWith("#") && bg.length() == 7) {
			try {
				color = new Color(Integer.parseInt(bg.substring(1, 3), 16),
						Integer.parseInt(bg.substring(3, 5), 16),
						Integer.parseInt(bg.substring(5, 7), 16));
			} catch (NumberFormatException e) {
				color = null; // no changes
				e.printStackTrace();
			}
		}

		if (color != null) {
			setBackground(color);
			bookPane.setBackground(color);
		}

		JScrollPane scroll = new JScrollPane(bookPane);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);

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
					try {
						File target = LocalReaderFrame.this.reader.getTarget(
								luid, null);
						Desktop.getDesktop().browse(target.toURI());
					} catch (IOException e) {
						Instance.syserr(e);
					}
				}
			});

			bookPane.add(book);
		}

		bookPane.validate();
		bookPane.repaint();
	}

	private JMenuBar createMenu() {
		JMenuBar bar = new JMenuBar();

		JMenu file = new JMenu("File");

		JMenuItem imprt = new JMenuItem("Import", KeyEvent.VK_I);
		imprt.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String url = JOptionPane.showInputDialog(LocalReaderFrame.this,
						"url of the story to import?\n" + "\n"
								+ "Note: it will currently make the UI \n"
								+ "unresponsive until it is downloaded...",
						"Importing from URL", JOptionPane.QUESTION_MESSAGE);
				if (url != null && !url.isEmpty()) {
					if (Main.imprt(url, null) != 0) {
						JOptionPane.showMessageDialog(LocalReaderFrame.this,
								"Cannot import: " + url, "Imort error",
								JOptionPane.ERROR_MESSAGE);
					} else {
						refreshBooks(type);
					}
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
}
