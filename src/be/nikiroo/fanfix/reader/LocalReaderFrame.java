package be.nikiroo.fanfix.reader;

import java.awt.BorderLayout;
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
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.LocalReaderBook.BookActionListner;

class LocalReaderFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private LocalReader reader;
	private List<MetaData> stories;
	private List<LocalReaderBook> books;
	private JPanel bookPane;
	private String type;

	public LocalReaderFrame(LocalReader reader, String type) {
		super("HTML reader");

		this.reader = reader;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLayout(new BorderLayout());

		books = new ArrayList<LocalReaderBook>();
		bookPane = new JPanel(new WrapLayout(WrapLayout.LEADING));

		add(new JScrollPane(bookPane), BorderLayout.CENTER);

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
			books.add(book);
			final String luid = meta.getLuid();
			book.addActionListener(new BookActionListner() {
				public void select(LocalReaderBook book) {
					for (LocalReaderBook abook : books) {
						abook.setSelected(abook == book);
					}
				}

				public void action(LocalReaderBook book) {
					try {
						File target = LocalReaderFrame.this.reader
								.getTarget(luid);
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
						"url?");
				if (Main.imprt(url) != 0) {
					JOptionPane.showMessageDialog(LocalReaderFrame.this,
							"Cannot import", "Imort error",
							JOptionPane.ERROR_MESSAGE);
				} else {
					refreshBooks(type);
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
