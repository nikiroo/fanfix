package be.nikiroo.fanfix_swing.gui.book;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JLabel;
import javax.swing.JPanel;

import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix_swing.gui.BooksPanel;

/**
 * A book item presented in a {@link BooksPanel}.
 * <p>
 * Can be a story, or a comic or... a group.
 * 
 * @author niki
 */
public class BookLine extends JPanel {
	private static final long serialVersionUID = 1L;

	/** Colour used for the seconday item (author/word count). */
	protected static final Color AUTHOR_COLOR = new Color(128, 128, 128);

	private boolean selected;
	private boolean hovered;

	private BookInfo info;
	private boolean seeWordCount;

	private JLabel title;
	private JLabel secondary;
	private JLabel iconCached;
	private JLabel iconNotCached;

	/**
	 * Create a new {@link BookLine} item for the given {@link Story}.
	 * 
	 * @param info         the information about the story to represent
	 * @param seeWordCount TRUE to see word counts, FALSE to see authors
	 */
	public BookLine(BookInfo info, boolean seeWordCount) {
		this.info = info;
		this.seeWordCount = seeWordCount;

		init();
	}

	/**
	 * Initialise this {@link BookLine}.
	 */
	protected void init() {
		// TODO: image
		iconCached = new JLabel("   ");
		iconNotCached = new JLabel(" * ");

		title = new JLabel();
		secondary = new JLabel();
		secondary.setForeground(AUTHOR_COLOR);

		setLayout(new BorderLayout());
		add(title, BorderLayout.CENTER);
		add(secondary, BorderLayout.EAST);

		updateMeta();
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
	 * The book current selection state,
	 * 
	 * @param selected TRUE if it is selected
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
	public boolean isHovered() {
		return this.hovered;
	}

	/**
	 * The item mouse-hover state.
	 * 
	 * @param hovered TRUE if it is mouse-hovered
	 */
	public void setHovered(boolean hovered) {
		if (this.hovered != hovered) {
			this.hovered = hovered;
			repaint();
		}
	}

	/**
	 * The secondary value content: word count or author.
	 * 
	 * @return TRUE to see word counts, FALSE to see authors
	 */
	public boolean isSeeWordCount() {
		return seeWordCount;
	}

	/**
	 * The secondary value content: word count or author.
	 * 
	 * @param seeWordCount TRUE to see word counts, FALSE to see authors
	 */
	public void setSeeWordCount(boolean seeWordCount) {
		if (this.seeWordCount != seeWordCount) {
			this.seeWordCount = seeWordCount;
			repaint();
		}
	}

	/**
	 * The information about the book represented by this item.
	 * 
	 * @return the meta
	 */
	public BookInfo getInfo() {
		return info;
	}

	/**
	 * Update the title, paint the item.
	 */
	@Override
	public void paint(Graphics g) {
		updateMeta();
		super.paint(g);
	}

	/**
	 * Update the title with the currently registered information.
	 */
	protected void updateMeta() {
		String main = info.getMainInfo();
		String optSecondary = info.getSecondaryInfo(seeWordCount);

		//TODO: max size limit?
		title.setText(main);
		secondary.setText(optSecondary + " ");

		setBackground(BookCoverImager.getBackground(isEnabled(), isSelected(), isHovered()));

		remove(iconCached);
		remove(iconNotCached);
		add(getInfo().isCached() ? iconCached : iconNotCached, BorderLayout.WEST);
		validate();
	}
}
