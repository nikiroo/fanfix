package be.nikiroo.fanfix.reader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import be.nikiroo.fanfix.data.MetaData;

/**
 * A book item presented in a {@link LocalReaderFrame}.
 * 
 * @author niki
 */
class LocalReaderBook extends JPanel {
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
		 *            the {@link LocalReaderBook} itself
		 */
		public void select(LocalReaderBook book);

		/**
		 * The book was double-clicked.
		 * 
		 * @param book
		 *            the {@link LocalReaderBook} itself
		 */
		public void action(LocalReaderBook book);
	}

	private static final int COVER_WIDTH = 100;
	private static final int COVER_HEIGHT = 150;
	private static final int SPINE_WIDTH = 5;
	private static final int SPINE_HEIGHT = 5;
	private static final int HOFFSET = 20;
	private static final Color SPINE_COLOR_BOTTOM = new Color(180, 180, 180);
	private static final Color SPINE_COLOR_RIGHT = new Color(100, 100, 100);
	private static final int TEXT_WIDTH = COVER_WIDTH + 40;
	private static final int TEXT_HEIGHT = 50;
	private static final String AUTHOR_COLOR = "#888888";
	private static final long serialVersionUID = 1L;

	private JLabel icon;
	private JLabel title;
	private boolean selected;
	private boolean hovered;
	private Date lastClick;
	private long doubleClickDelay = 200; // in ms
	private List<BookActionListener> listeners;
	private String luid;
	private boolean cached;

	public LocalReaderBook(MetaData meta, boolean cached) {
		this.luid = meta.getLuid();
		this.cached = cached;

		if (meta.getCover() != null) {
			BufferedImage resizedImage = new BufferedImage(SPINE_WIDTH
					+ COVER_WIDTH, SPINE_HEIGHT + COVER_HEIGHT + HOFFSET,
					BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g = resizedImage.createGraphics();
			g.setColor(Color.white);
			g.fillRect(0, HOFFSET, COVER_WIDTH, COVER_HEIGHT);
			g.drawImage(meta.getCover(), 0, HOFFSET, COVER_WIDTH, COVER_HEIGHT,
					null);
			g.dispose();

			icon = new JLabel(new ImageIcon(resizedImage));
		} else {
			// TODO: a big black "X" ?
			icon = new JLabel(" [ no cover ] ");
		}

		String optAuthor = meta.getAuthor();
		if (optAuthor != null && !optAuthor.isEmpty()) {
			optAuthor = "(" + optAuthor + ")";
		}
		title = new JLabel(
				String.format(
						"<html>"
								+ "<body style='width: %d px; height: %d px; text-align: center'>"
								+ "%s" + "<br>" + "<span style='color: %s;'>"
								+ "%s" + "</span>" + "</body>" + "</html>",
						TEXT_WIDTH, TEXT_HEIGHT, meta.getTitle(), AUTHOR_COLOR,
						optAuthor));

		this.setLayout(new BorderLayout(10, 10));
		this.add(icon, BorderLayout.CENTER);
		this.add(title, BorderLayout.SOUTH);

		setupListeners();
		setSelected(false);
	}

	/**
	 * The book current selection state.
	 * 
	 * @return the selected
	 */
	public boolean isSelected() {
		return selected;
	}

	/**
	 * The book current selection state.
	 * 
	 * @param selected
	 *            the selected to set
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
		repaint();
	}

	private void setHovered(boolean hovered) {
		this.hovered = hovered;
		repaint();
	}

	private void setupListeners() {
		listeners = new ArrayList<LocalReaderBook.BookActionListener>();
		addMouseListener(new MouseListener() {
			public void mouseReleased(MouseEvent e) {
			}

			public void mousePressed(MouseEvent e) {
			}

			public void mouseExited(MouseEvent e) {
				setHovered(false);
			}

			public void mouseEntered(MouseEvent e) {
				setHovered(true);
			}

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
		});
	}

	private void click(boolean doubleClick) {
		for (BookActionListener listener : listeners) {
			if (doubleClick) {
				listener.action(this);
			} else {
				listener.select(this);
			}
		}
	}

	public void addActionListener(BookActionListener listener) {
		listeners.add(listener);
	}

	public String getLuid() {
		return luid;
	}

	/**
	 * This boos is cached into the {@link LocalReader} library.
	 * 
	 * @return the cached
	 */
	public boolean isCached() {
		return cached;
	}

	/**
	 * This boos is cached into the {@link LocalReader} library.
	 * 
	 * @param cached
	 *            the cached to set
	 */
	public void setCached(boolean cached) {
		this.cached = cached;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		int h = COVER_HEIGHT;
		int w = COVER_WIDTH;
		int xOffset = (TEXT_WIDTH - COVER_WIDTH) - 4;

		int[] xs = new int[] { xOffset, xOffset + SPINE_WIDTH,
				xOffset + w + SPINE_WIDTH, xOffset + w };
		int[] ys = new int[] { HOFFSET + h, HOFFSET + h + SPINE_HEIGHT,
				HOFFSET + h + SPINE_HEIGHT, HOFFSET + h };
		g.setColor(SPINE_COLOR_BOTTOM);
		g.fillPolygon(new Polygon(xs, ys, xs.length));
		xs = new int[] { xOffset + w, xOffset + w + SPINE_WIDTH,
				xOffset + w + SPINE_WIDTH, xOffset + w };
		ys = new int[] { HOFFSET, HOFFSET + SPINE_HEIGHT,
				HOFFSET + h + SPINE_HEIGHT, HOFFSET + h };
		g.setColor(SPINE_COLOR_RIGHT);
		g.fillPolygon(new Polygon(xs, ys, xs.length));

		Color color = new Color(255, 255, 255, 0);
		if (!isEnabled()) {
		} else if (selected && !hovered) {
			color = new Color(80, 80, 100, 40);
		} else if (!selected && hovered) {
			color = new Color(230, 230, 255, 100);
		} else if (selected && hovered) {
			color = new Color(200, 200, 255, 100);
		}

		Rectangle clip = g.getClipBounds();
		if (cached) {
			g.setColor(Color.green);
			g.fillOval(clip.x + clip.width - 30, 10, 20, 20);
		}
		
		g.setColor(color);
		g.fillRect(clip.x, clip.y, clip.width, clip.height);
	}
}
