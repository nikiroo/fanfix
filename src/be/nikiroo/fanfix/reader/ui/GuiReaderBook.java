package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

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
		 * @param target
		 *            the target component for the popup
		 * @param x
		 *            the X position of the click/request (in case of popup
		 *            request from the keyboard, the center of the target is
		 *            selected as point of reference)
		 * @param y
		 *            the Y position of the click/request (in case of popup
		 *            request from the keyboard, the center of the target is
		 *            selected as point of reference)
		 */
		public void popupRequested(GuiReaderBook book, Component target, int x,
				int y);
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
	private GuiReaderBookInfo info;
	private boolean cached;
	private boolean seeWordCount;

	/**
	 * Create a new {@link GuiReaderBook} item for the given {@link Story}.
	 * 
	 * @param reader
	 *            the associated reader
	 * @param info
	 *            the information about the story to represent
	 * @param cached
	 *            TRUE if it is locally cached
	 * @param seeWordCount
	 *            TRUE to see word counts, FALSE to see authors
	 */
	public GuiReaderBook(Reader reader, GuiReaderBookInfo info, boolean cached,
			boolean seeWordCount) {
		this.info = info;
		this.cached = cached;
		this.seeWordCount = seeWordCount;

		icon = new JLabel(GuiReaderCoverImager.generateCoverIcon(
				reader.getLibrary(), info));

		title = new JLabel();
		updateTitle();

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
	 * <p>
	 * Setting this value to true can cause a "select" action to occur if the
	 * previous state was "unselected".
	 * 
	 * @param selected
	 *            TRUE if it is selected
	 */
	public void setSelected(boolean selected) {
		if (this.selected != selected) {
			this.selected = selected;
			repaint();

			if (selected) {
				select();
			}
		}
	}

	/**
	 * The item mouse-hover state.
	 * 
	 * @return TRUE if it is mouse-hovered
	 */
	public boolean isHovered() {
		return this.hovered;
	}

	/**
	 * The item mouse-hover state.
	 * 
	 * @param hovered
	 *            TRUE if it is mouse-hovered
	 */
	public void setHovered(boolean hovered) {
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
				if (isEnabled() && e.isPopupTrigger()) {
					popup(e);
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (isEnabled() && e.isPopupTrigger()) {
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
					e.consume();
				}
			}

			private void click(boolean doubleClick) {
				if (doubleClick) {
					action();
				} else {
					select();
				}
			}

			private void popup(MouseEvent e) {
				GuiReaderBook.this
						.popup(GuiReaderBook.this, e.getX(), e.getY());
				e.consume();
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
	 * Cause an action to occur on this {@link GuiReaderBook}.
	 */
	public void action() {
		for (BookActionListener listener : listeners) {
			listener.action(GuiReaderBook.this);
		}
	}

	/**
	 * Cause a select event on this {@link GuiReaderBook}.
	 * <p>
	 * Have a look at {@link GuiReaderBook#setSelected(boolean)}.
	 */
	private void select() {
		for (BookActionListener listener : listeners) {
			listener.select(GuiReaderBook.this);
		}
	}

	/**
	 * Request a popup.
	 * 
	 * @param target
	 *            the target component for the popup
	 * @param x
	 *            the X position of the click/request (in case of popup request
	 *            from the keyboard, the center of the target should be selected
	 *            as point of reference)
	 * @param y
	 *            the Y position of the click/request (in case of popup request
	 *            from the keyboard, the center of the target should be selected
	 *            as point of reference)
	 */
	public void popup(Component target, int x, int y) {
		for (BookActionListener listener : listeners) {
			listener.select((GuiReaderBook.this));
			listener.popupRequested(GuiReaderBook.this, target, x, y);
		}
	}

	/**
	 * The information about the book represented by this item.
	 * 
	 * @return the meta
	 */
	public GuiReaderBookInfo getInfo() {
		return info;
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
	 * Update the title, paint the item, then call
	 * {@link GuiReaderCoverImager#paintOverlay(Graphics, boolean, boolean, boolean, boolean)}
	 * .
	 */
	@Override
	public void paint(Graphics g) {
		updateTitle();
		super.paint(g);
		GuiReaderCoverImager.paintOverlay(g, isEnabled(), isSelected(),
				isHovered(), isCached());
	}

	/**
	 * Update the title with the currently registered information.
	 */
	private void updateTitle() {
		String optSecondary = info.getSecondaryInfo(seeWordCount);
		title.setText(String
				.format("<html>"
						+ "<body style='width: %d px; height: %d px; text-align: center'>"
						+ "%s" + "<br>" + "<span style='color: %s;'>" + "%s"
						+ "</span>" + "</body>" + "</html>",
						GuiReaderCoverImager.TEXT_WIDTH,
						GuiReaderCoverImager.TEXT_HEIGHT, info.getMainInfo(),
						AUTHOR_COLOR, optSecondary));
	}
}
