package be.nikiroo.fanfix_swing.gui.book;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JLabel;
import javax.swing.JPanel;

import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.BooksPanel;

/**
 * A book item presented in a {@link BooksPanel}.
 * <p>
 * Can be a story, or a comic or... a group.
 * 
 * @author niki
 */
public class BookBlock extends BookLine {
	static private final long serialVersionUID = 1L;
	static private Image empty = BookCoverImager.generateCoverImage(null,
			(BookInfo) null);

	private JLabel title;
	private Image coverImage;

	/**
	 * Create a new {@link BookBlock} item for the given {@link Story}.
	 * 
	 * @param info
	 *            the information about the story to represent
	 * @param seeWordCount
	 *            TRUE to see word counts, FALSE to see authors
	 */
	public BookBlock(BookInfo info, boolean seeWordCount) {
		super(info, seeWordCount);
	}

	@Override
	protected void init() {
		coverImage = empty;
		title = new JLabel();
		updateMeta();

		JPanel filler = new JPanel();
		filler.setPreferredSize(new Dimension(BookCoverImager.getCoverWidth(),
				BookCoverImager.getCoverHeight()));
		filler.setOpaque(false);

		setLayout(new BorderLayout(10, 10));
		add(filler, BorderLayout.CENTER);
		add(title, BorderLayout.SOUTH);
	}

	/**
	 * the cover image to use a base (see
	 * {@link BookCoverImager#generateCoverImage(BasicLibrary, BookInfo)})
	 * 
	 * @param coverImage
	 *            the image
	 */
	public void setCoverImage(Image coverImage) {
		this.coverImage = coverImage;
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		g.drawImage(coverImage,
				BookCoverImager.TEXT_WIDTH - BookCoverImager.COVER_WIDTH, 0,
				null);
		BookCoverImager.paintOverlay(g, isEnabled(), isSelected(), isHovered(),
				getInfo().isCached());
	}

	@Override
	protected void updateMeta() {
		String main = getMainInfoDisplay();
		String optSecondary = getSecondaryInfoDisplay(isSeeWordCount());
		String color = String.format("#%X%X%X", AUTHOR_COLOR.getRed(),
				AUTHOR_COLOR.getGreen(), AUTHOR_COLOR.getBlue());
		title.setText(String.format("<html>"
				+ "<body style='width: %d px; height: %d px; text-align: center;'>"
				+ "%s" + "<br>" + "<span style='color: %s;'>" + "%s" + "</span>"
				+ "</body>" + "</html>", BookCoverImager.TEXT_WIDTH,
				BookCoverImager.TEXT_HEIGHT, main, color, optSecondary));

		setBackground(BookCoverImager.getBackground(isEnabled(), isSelected(),
				isHovered()));
	}

	/**
	 * Generate a cover icon based upon the given {@link BookInfo}.
	 * 
	 * @param lib
	 *            the library the meta comes from
	 * @param info
	 *            the {@link BookInfo}
	 * 
	 * @return the image
	 */
	static public java.awt.Image generateCoverImage(BasicLibrary lib,
			BookInfo info) {
		return BookCoverImager.generateCoverImage(lib, info);
	}
}
