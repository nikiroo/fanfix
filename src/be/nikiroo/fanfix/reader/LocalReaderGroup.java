package be.nikiroo.fanfix.reader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.LocalReaderBook.BookActionListener;
import be.nikiroo.utils.ui.WrapLayout;

/**
 * A group of {@link LocalReaderBook}s for display.
 * 
 * @author niki
 */
public class LocalReaderGroup extends JPanel {
	private static final long serialVersionUID = 1L;
	private BookActionListener action;
	private Color backgroundColor;
	private LocalReader reader;
	private List<MetaData> stories;
	private List<LocalReaderBook> books;
	private JPanel pane;
	private boolean words; // words or authors (secondary info on books)

	/**
	 * Create a new {@link LocalReaderGroup}.
	 * 
	 * @param reader
	 *            the {@link LocalReaderBook} used to probe some information
	 *            about the stories
	 * @param title
	 *            the title of this group
	 * @param backgroundColor
	 *            the background colour to use (or NULL for default)
	 */
	public LocalReaderGroup(LocalReader reader, String title,
			Color backgroundColor) {
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
				title = "[unknown]";
			}

			JLabel label = new JLabel();
			label.setText(String.format("<html>"
					+ "<body style='text-align: center'><br>" + "%s"
					+ "</body>" + "</html>", title));
			label.setHorizontalAlignment(JLabel.CENTER);
			add(label, BorderLayout.NORTH);
		}
	}

	/**
	 * Set the {@link ActionListener} that will be fired on each
	 * {@link LocalReaderBook} action.
	 * 
	 * @param action
	 *            the action
	 */
	public void setActionListener(BookActionListener action) {
		this.action = action;
		refreshBooks(stories, words);
	}

	/**
	 * Refresh the list of {@link LocalReaderBook}s displayed in the control.
	 * 
	 * @param stories
	 *            the stories
	 * @param seeWordcount
	 *            TRUE to see word counts, FALSE to see authors
	 */
	public void refreshBooks(List<MetaData> stories, boolean seeWordcount) {
		this.stories = stories;
		this.words = seeWordcount;

		books = new ArrayList<LocalReaderBook>();
		invalidate();
		pane.invalidate();
		pane.removeAll();

		if (stories != null) {
			for (MetaData meta : stories) {
				LocalReaderBook book = new LocalReaderBook(meta,
						reader.isCached(meta.getLuid()), seeWordcount);
				if (backgroundColor != null) {
					book.setBackground(backgroundColor);
				}

				books.add(book);

				book.addActionListener(new BookActionListener() {
					public void select(LocalReaderBook book) {
						for (LocalReaderBook abook : books) {
							abook.setSelected(abook == book);
						}
					}

					public void popupRequested(LocalReaderBook book,
							MouseEvent e) {
					}

					public void action(LocalReaderBook book) {
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
			for (LocalReaderBook book : books) {
				book.setEnabled(b);
				book.repaint();
			}
		}

		pane.setEnabled(b);
		super.setEnabled(b);
		repaint();
	}
}
