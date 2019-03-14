package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.reader.Reader;

/**
 * A book item presented in a {@link GuiReaderFrame}.
 * 
 * @author niki
 */
class GuiReaderBook extends JPanel {
	/**
	 * Action on a book item.
	 * 
	 * @author niki
	 */
	interface BookActionListener extends EventListener {
		/**
		 * The book was selected (single click).
		 * 
		 * @param book
		 *            the {@link GuiReaderBook} itself
		 */
		public void select(GuiReaderBook book);

		/**
		 * The book was double-clicked.
		 * 
		 * @param book
		 *            the {@link GuiReaderBook} itself
		 */
		public void action(GuiReaderBook book);

		/**
		 * A popup menu was requested for this {@link GuiReaderBook}.
		 * 
		 * @param book
		 *            the {@link GuiReaderBook} itself
		 * @param e
		 *            the {@link MouseEvent} that generated this call
		 */
		public void popupRequested(GuiReaderBook book, MouseEvent e);
	}

	private static final long serialVersionUID = 1L;

	private static final String AUTHOR_COLOR = "#888888";
	private static final long doubleClickDelay = 200; // in ms

	private JLabel icon;
	private JLabel title;
	private boolean selected;
	private boolean hovered;
	private Date lastClick;

	private List<BookActionListener> listeners;
	private MetaData meta;
	private boolean cached;

	/**
	 * Create a new {@link GuiReaderBook} item for the given {@link Story}.
	 * 
	 * @param reader
	 *            the associated reader
	 * @param meta
	 *            the story {@link MetaData} or source (if no LUID)
	 * @param cached
	 *            TRUE if it is locally cached
	 * @param seeWordCount
	 *            TRUE to see word counts, FALSE to see authors
	 */
	public GuiReaderBook(Reader reader, MetaData meta, boolean cached,
			boolean seeWordCount) {
		this.cached = cached;
		this.meta = meta;

		String optSecondary = meta.getAuthor();
		if (seeWordCount) {
			if (meta.getWords() >= 4000) {
				optSecondary = "" + (meta.getWords() / 1000) + "k";
			} else if (meta.getWords() > 0) {
				optSecondary = "" + meta.getWords();
			} else {
				optSecondary = "";
			}

			if (!optSecondary.isEmpty()) {
				if (meta.isImageDocument()) {
					optSecondary += " images";
				} else {
					optSecondary += " words";
				}
			}
		}

		if (optSecondary != null && !optSecondary.isEmpty()) {
			optSecondary = "(" + optSecondary + ")";
		} else {
			optSecondary = "";
		}

		icon = new JLabel(GuiReaderCoverImager.generateCoverIcon(
				reader.getLibrary(), getMeta()));
		title = new JLabel(
				String.format(
						"<html>"
								+ "<body style='width: %d px; height: %d px; text-align: center'>"
								+ "%s" + "<br>" + "<span style='color: %s;'>"
								+ "%s" + "</span>" + "</body>" + "</html>",
						GuiReaderCoverImager.TEXT_WIDTH,
						GuiReaderCoverImager.TEXT_HEIGHT, meta.getTitle(),
						AUTHOR_COLOR, optSecondary));

		setLayout(new BorderLayout(10, 10));
		add(icon, BorderLayout.CENTER);
		add(title, BorderLayout.SOUTH);

		setupListeners();
	}

	/**
	 * The book current selection state.
	 * 
	 * @return the selection state
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * The book current selection state.
	 * 
	 * @param selected
	 *            TRUE if it is selected
	 */
	public void setSelected(boolean selected) {
		if (this.selected != selected) {
			this.selected = selected;
			repaint();
		}
	}

	/**
	 * The item mouse-hover state.
	 * 
	 * @return TRUE if it is mouse-hovered
	 */
	private boolean isHovered() {
		return this.hovered;
	}

	/**
	 * The item mouse-hover state.
	 * 
	 * @param hovered
	 *            TRUE if it is mouse-hovered
	 */
	private void setHovered(boolean hovered) {
		if (this.hovered != hovered) {
			this.hovered = hovered;
			repaint();
		}
	}

	/**
	 * Setup the mouse listener that will activate {@link BookActionListener}
	 * events.
	 */
	private void setupListeners() {
		listeners = new ArrayList<GuiReaderBook.BookActionListener>();
		addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup(e);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup(e);
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				setHovered(false);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				setHovered(true);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (isEnabled()) {
					Date now = new Date();
					if (lastClick != null
							&& now.getTime() - lastClick.getTime() < doubleClickDelay) {
						click(true);
					} else {
						click(false);
					}

					lastClick = now;
				}
			}

			private void click(boolean doubleClick) {
				for (BookActionListener listener : listeners) {
					if (doubleClick) {
						listener.action(GuiReaderBook.this);
					} else {
						listener.select(GuiReaderBook.this);
					}
				}
			}

			private void popup(MouseEvent e) {
				for (BookActionListener listener : listeners) {
					listener.select((GuiReaderBook.this));
					listener.popupRequested(GuiReaderBook.this, e);
				}
			}
		});
	}

	/**
	 * Add a new {@link BookActionListener} on this item.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void addActionListener(BookActionListener listener) {
		listeners.add(listener);
	}

	/**
	 * The Library {@link MetaData} of the book represented by this item.
	 * 
	 * @return the meta
	 */
	public MetaData getMeta() {
		return meta;
	}

	/**
	 * This item {@link GuiReader} library cache state.
	 * 
	 * @return TRUE if it is present in the {@link GuiReader} cache
	 */
	public boolean isCached() {
		return cached;
	}

	/**
	 * This item {@link GuiReader} library cache state.
	 * 
	 * @param cached
	 *            TRUE if it is present in the {@link GuiReader} cache
	 */
	public void setCached(boolean cached) {
		if (this.cached != cached) {
			this.cached = cached;
			repaint();
		}
	}

	/**
	 * Paint the item, then call {@link GuiReaderBook#paintOverlay(Graphics)}.
	 */
	@Override
	public void paint(Graphics g) {
		super.paint(g);
		GuiReaderCoverImager.paintOverlay(g, isEnabled(), isSelected(),
				isHovered(), isCached());
	}
}
