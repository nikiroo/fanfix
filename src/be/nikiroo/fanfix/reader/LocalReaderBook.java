package be.nikiroo.fanfix.reader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import javax.swing.JTextArea;

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
	interface BookActionListner extends EventListener {
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

	private static final long serialVersionUID = 1L;
	private JLabel icon;
	private JTextArea title;
	private JTextArea author;
	private boolean selected;
	private boolean hovered;
	private Date lastClick;
	private long doubleClickDelay = 200; // in ms
	private List<BookActionListner> listeners;

	public LocalReaderBook(MetaData meta) {
		if (meta.getCover() != null) {
			BufferedImage resizedImage = new BufferedImage(100, 150,
					BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g = resizedImage.createGraphics();
			g.drawImage(meta.getCover(), 0, 0, 100, 150, null);
			g.dispose();

			icon = new JLabel(new ImageIcon(resizedImage));
		} else {
			icon = new JLabel(" [ no cover ] ");
		}

		title = new JTextArea(meta.getTitle());
		title.setWrapStyleWord(true);
		title.setLineWrap(true);
		title.setEditable(false);
		title.setBackground(new Color(0, true));
		author = new JTextArea("by " + meta.getAuthor());

		this.setLayout(new BorderLayout());
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
		listeners = new ArrayList<LocalReaderBook.BookActionListner>();
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
				Date now = new Date();
				if (lastClick != null
						&& now.getTime() - lastClick.getTime() < doubleClickDelay) {
					click(true);
				} else {
					click(false);
				}
				lastClick = now;
			}
		});
	}

	private void click(boolean doubleClick) {
		for (BookActionListner listener : listeners) {
			if (doubleClick) {
				listener.action(this);
			} else {
				listener.select(this);
			}
		}
	}

	public void addActionListener(BookActionListner listener) {
		listeners.add(listener);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Color color = new Color(255, 255, 255, 0);
		if (selected && !hovered) {
			color = new Color(80, 80, 100, 40);
		} else if (!selected && hovered) {
			color = new Color(230, 230, 255, 100);
		} else if (selected && hovered) {
			color = new Color(200, 200, 255, 100);
		}

		Rectangle clip = g.getClipBounds();
		g.setColor(color);
		g.fillRect(clip.x, clip.y, clip.width, clip.height);
	}
}
