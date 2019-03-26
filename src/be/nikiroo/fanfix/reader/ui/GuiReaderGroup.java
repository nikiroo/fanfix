package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.reader.ui.GuiReaderBook.BookActionListener;
import be.nikiroo.utils.ui.WrapLayout;

/**
 * A group of {@link GuiReaderBook}s for display.
 * 
 * @author niki
 */
public class GuiReaderGroup extends JPanel {
	private static final long serialVersionUID = 1L;
	private BookActionListener action;
	private Color backgroundColor;
	private GuiReader reader;
	private List<GuiReaderBookInfo> infos;
	private List<GuiReaderBook> books;
	private JPanel pane;
	private boolean words; // words or authors (secondary info on books)
	private int itemsPerLine;

	/**
	 * Create a new {@link GuiReaderGroup}.
	 * 
	 * @param reader
	 *            the {@link GuiReaderBook} used to probe some information about
	 *            the stories
	 * @param title
	 *            the title of this group
	 * @param backgroundColor
	 *            the background colour to use (or NULL for default)
	 */
	public GuiReaderGroup(GuiReader reader, String title, Color backgroundColor) {
		this.reader = reader;
		this.backgroundColor = backgroundColor;

		this.pane = new JPanel();

		pane.setLayout(new WrapLayout(WrapLayout.LEADING, 5, 5));
		if (backgroundColor != null) {
			pane.setBackground(backgroundColor);
			setBackground(backgroundColor);
		}

		setLayout(new BorderLayout(0, 10));
		add(pane, BorderLayout.CENTER);

		if (title != null) {
			if (title.isEmpty()) {
				title = GuiReader.trans(StringIdGui.MENU_AUTHORS_UNKNOWN);
			}

			JLabel label = new JLabel();
			label.setText(String.format("<html>"
					+ "<body style='text-align: center; color: gray;'><br><b>"
					+ "%s" + "</b></body>" + "</html>", title));
			label.setHorizontalAlignment(JLabel.CENTER);
			add(label, BorderLayout.NORTH);
		}

		// Compute the number of items per line at each resize
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				super.componentResized(e);
				computeItemsPerLine();
			}
		});
		computeItemsPerLine();

		addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				onKeyTyped(e);
			}
		});
	}

	/**
	 * Compute how many items can fit in a line so UP and DOWN can be used to go
	 * up/down one line at a time.
	 */
	private void computeItemsPerLine() {
		// TODO
		itemsPerLine = 5;
	}

	/**
	 * Set the {@link ActionListener} that will be fired on each
	 * {@link GuiReaderBook} action.
	 * 
	 * @param action
	 *            the action
	 */
	public void setActionListener(BookActionListener action) {
		this.action = action;
		refreshBooks(infos, words);
	}

	/**
	 * Refresh the list of {@link GuiReaderBook}s displayed in the control.
	 * 
	 * @param infos
	 *            the new list of infos
	 * @param seeWordcount
	 *            TRUE to see word counts, FALSE to see authors
	 */
	public void refreshBooks(List<GuiReaderBookInfo> infos, boolean seeWordcount) {
		this.infos = infos;
		refreshBooks(seeWordcount);
	}

	/**
	 * Refresh the list of {@link GuiReaderBook}s displayed in the control.
	 * <p>
	 * Will not change the current stories.
	 * 
	 * @param seeWordcount
	 *            TRUE to see word counts, FALSE to see authors
	 */
	public void refreshBooks(boolean seeWordcount) {
		this.words = seeWordcount;

		books = new ArrayList<GuiReaderBook>();
		invalidate();
		pane.invalidate();
		pane.removeAll();

		if (infos != null) {
			for (GuiReaderBookInfo info : infos) {
				boolean isCached = false;
				if (info.getMeta() != null) {
					isCached = reader.isCached(info.getMeta().getLuid());
				}

				GuiReaderBook book = new GuiReaderBook(reader, info, isCached,
						words);
				if (backgroundColor != null) {
					book.setBackground(backgroundColor);
				}

				books.add(book);

				book.addActionListener(new BookActionListener() {
					@Override
					public void select(GuiReaderBook book) {
						for (GuiReaderBook abook : books) {
							abook.setSelected(abook == book);
						}
					}

					@Override
					public void popupRequested(GuiReaderBook book, MouseEvent e) {
					}

					@Override
					public void action(GuiReaderBook book) {
					}
				});

				if (action != null) {
					book.addActionListener(action);
				}

				pane.add(book);
			}
		}

		pane.validate();
		pane.repaint();
		validate();
		repaint();
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
		if (books != null) {
			for (GuiReaderBook book : books) {
				book.setEnabled(b);
				book.repaint();
			}
		}

		pane.setEnabled(b);
		super.setEnabled(b);
		repaint();
	}

	/**
	 * The action to execute when a key is typed.
	 * 
	 * @param e
	 *            the key event
	 */
	private void onKeyTyped(KeyEvent e) {
		boolean consumed = false;
		System.out.println(e);
		if (e.isActionKey()) {
			int offset = 0;
			switch (e.getKeyCode()) {
			case KeyEvent.VK_LEFT:
				offset = -1;
				break;
			case KeyEvent.VK_RIGHT:
				offset = 1;
				break;
			case KeyEvent.VK_UP:
				offset = itemsPerLine;
				break;
			case KeyEvent.VK_DOWN:
				offset = -itemsPerLine;
				break;
			}

			if (offset != 0) {
				consumed = true;

				int selected = -1;
				for (int i = 0; i < books.size(); i++) {
					if (books.get(i).isSelected()) {
						selected = i;
						break;
					}
				}

				if (selected >= 0) {
					int newSelect = selected + offset;
					if (newSelect >= books.size()) {
						newSelect = books.size() - 1;
					}

					if (selected != newSelect && newSelect >= 0) {
						if (selected >= 0) {
							books.get(selected).setSelected(false);
							books.get(newSelect).setSelected(true);
						}
					}
				}
			}
		}

		if (consumed) {
			e.consume();
		} else {
			super.processKeyEvent(e);
		}
	}
}
