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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EventListener;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.ui.UIUtils;

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

	// TODO: export some of the configuration options?
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
	private static final Color BORDER = Color.black;
	private static final long doubleClickDelay = 200; // in ms
	//

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
	 * @param meta
	 *            the story {@link MetaData}
	 * @param cached
	 *            TRUE if it is locally cached
	 * @param seeWordCount
	 *            TRUE to see word counts, FALSE to see authors
	 */
	public GuiReaderBook(MetaData meta, boolean cached, boolean seeWordCount) {
		this.cached = cached;
		this.meta = meta;

		String optSecondary = meta.getAuthor();
		if (seeWordCount) {
			if (meta.getWords() >= 4000) {
				optSecondary = (meta.getWords() / 1000) + "k words";
			} else if (meta.getWords() > 0) {
				optSecondary = meta.getWords() + " words";
			} else {
				optSecondary = "";
			}
		}

		if (optSecondary != null && !optSecondary.isEmpty()) {
			optSecondary = "(" + optSecondary + ")";
		}

		icon = new JLabel(generateCoverIcon(meta));
		title = new JLabel(
				String.format(
						"<html>"
								+ "<body style='width: %d px; height: %d px; text-align: center'>"
								+ "%s" + "<br>" + "<span style='color: %s;'>"
								+ "%s" + "</span>" + "</body>" + "</html>",
						TEXT_WIDTH, TEXT_HEIGHT, meta.getTitle(), AUTHOR_COLOR,
						optSecondary));

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
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup(e);
				}
			}

			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					popup(e);
				}
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
		paintOverlay(g);
	}

	/**
	 * Draw a partially transparent overlay if needed depending upon the
	 * selection and mouse-hover states on top of the normal component, as well
	 * as a possible "cached" icon if the item is cached.
	 * 
	 * @param g
	 *            the {@link Graphics} to paint onto
	 */
	public void paintOverlay(Graphics g) {
		Rectangle clip = g.getClipBounds();
		if (clip.getWidth() <= 0 || clip.getHeight() <= 0) {
			return;
		}

		int h = COVER_HEIGHT;
		int w = COVER_WIDTH;
		int xOffset = (TEXT_WIDTH - COVER_WIDTH) - 1;
		int yOffset = HOFFSET;

		if (BORDER != null) {
			if (BORDER != null) {
				g.setColor(BORDER);
				g.drawRect(xOffset, yOffset, COVER_WIDTH, COVER_HEIGHT);
			}

			xOffset++;
			yOffset++;
		}

		int[] xs = new int[] { xOffset, xOffset + SPINE_WIDTH,
				xOffset + w + SPINE_WIDTH, xOffset + w };
		int[] ys = new int[] { yOffset + h, yOffset + h + SPINE_HEIGHT,
				yOffset + h + SPINE_HEIGHT, yOffset + h };
		g.setColor(SPINE_COLOR_BOTTOM);
		g.fillPolygon(new Polygon(xs, ys, xs.length));
		xs = new int[] { xOffset + w, xOffset + w + SPINE_WIDTH,
				xOffset + w + SPINE_WIDTH, xOffset + w };
		ys = new int[] { yOffset, yOffset + SPINE_HEIGHT,
				yOffset + h + SPINE_HEIGHT, yOffset + h };
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

		g.setColor(color);
		g.fillRect(clip.x, clip.y, clip.width, clip.height);

		if (cached) {
			UIUtils.drawEllipse3D(g, Color.green.darker(), COVER_WIDTH
					+ HOFFSET + 30, 10, 20, 20);
		}
	}

	/**
	 * Generate a cover icon based upon the given {@link MetaData}.
	 * 
	 * @param meta
	 *            the {@link MetaData} about the target {@link Story}
	 * 
	 * @return the icon
	 */
	private ImageIcon generateCoverIcon(MetaData meta) {
		String id = meta.getUuid() + ".thumb_" + SPINE_WIDTH + "x"
				+ COVER_WIDTH + "+" + SPINE_HEIGHT + "+" + COVER_HEIGHT + "@"
				+ HOFFSET;
		BufferedImage resizedImage = null;

		InputStream in = Instance.getCache().getFromCache(id);
		if (in != null) {
			try {
				resizedImage = IOUtils.toImage(in);
				in.close();
				in = null;
			} catch (IOException e) {
				Instance.syserr(e);
			}
		}

		if (resizedImage == null) {
			try {
				BufferedImage cover = Instance.getLibrary().getCover(
						meta.getLuid());

				resizedImage = new BufferedImage(SPINE_WIDTH + COVER_WIDTH,
						SPINE_HEIGHT + COVER_HEIGHT + HOFFSET,
						BufferedImage.TYPE_4BYTE_ABGR);
				Graphics2D g = resizedImage.createGraphics();
				g.setColor(Color.white);
				g.fillRect(0, HOFFSET, COVER_WIDTH, COVER_HEIGHT);
				if (cover != null) {
					g.drawImage(cover, 0, HOFFSET, COVER_WIDTH, COVER_HEIGHT,
							null);
				} else {
					g.setColor(Color.black);
					g.drawLine(0, HOFFSET, COVER_WIDTH, HOFFSET + COVER_HEIGHT);
					g.drawLine(COVER_WIDTH, HOFFSET, 0, HOFFSET + COVER_HEIGHT);
				}
				g.dispose();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				ImageIO.write(resizedImage, "png", out);
				byte[] imageBytes = out.toByteArray();
				in = new ByteArrayInputStream(imageBytes);
				Instance.getCache().addToCache(in, id);
				in.close();
				in = null;
			} catch (MalformedURLException e) {
				Instance.syserr(e);
			} catch (IOException e) {
				Instance.syserr(e);
			}
		}

		return new ImageIcon(resizedImage);
	}
}
