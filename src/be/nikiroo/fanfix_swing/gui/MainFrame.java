package be.nikiroo.fanfix_swing.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import be.nikiroo.utils.Version;

public class MainFrame extends JFrame {
	private BooksPanel books;
	private DetailsPanel details;

	public MainFrame(boolean sidePanel, boolean detailsPanel) {
		super("Fanfix " + Version.getCurrentVersion());
		setSize(800, 600);
		setJMenuBar(createMenuBar());

		sidePanel = true;
		detailsPanel = true;

		final BrowserPanel browser = new BrowserPanel();

		JComponent other = null;
		boolean orientationH = true;
		if (sidePanel && !detailsPanel) {
			other = browser;
		} else if (sidePanel && detailsPanel) {
			JComponent side = browser;
			details = new DetailsPanel();
			other = split(side, details, false, 0.5, 1);
		} else if (!sidePanel && !detailsPanel) {
			orientationH = false;
			other = new JLabel("<< Go back");
		} else if (!sidePanel && detailsPanel) {
			JComponent goBack = new JLabel("<< Go back");
			details = new DetailsPanel();
			other = split(goBack, details, false, 0.5, 1);
		}

		books = new BooksPanel(true);
		browser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				books.load(browser.getSelectedSources(), browser.getSelectedAuthors(), browser.getSelectedTags());
				details.setBook(browser.getHighlight());
			}
		});

		JSplitPane split = split(other, books, orientationH, 0.5, 0);

		this.add(split);
	}

	private JSplitPane split(JComponent leftTop, JComponent rightBottom, boolean horizontal, double ratio,
			double weight) {
		JSplitPane split = new JSplitPane(horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT, leftTop,
				rightBottom);
		split.setOneTouchExpandable(true);
		split.setResizeWeight(weight);
		split.setContinuousLayout(true);
		split.setDividerLocation(ratio);

		return split;
	}

	private JMenuBar createMenuBar() {
		JMenuBar bar = new JMenuBar();

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem item1 = new JMenuItem("Uuu", KeyEvent.VK_U);
		item1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Uuu: ACTION");
			}
		});

		file.add(item1);

		JMenu edit = new JMenu("Edit");
		edit.setMnemonic(KeyEvent.VK_E);

		JMenu view = new JMenu("View");
		view.setMnemonic(KeyEvent.VK_V);

		JMenuItem listMode = new JMenuItem("List mode", KeyEvent.VK_L);
		listMode.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				books.setListMode(!books.isListMode());
			}
		});

		view.add(listMode);

		bar.add(file);
		bar.add(edit);
		bar.add(view);

		return bar;
	}
}
