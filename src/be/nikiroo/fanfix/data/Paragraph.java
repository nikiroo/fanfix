package be.nikiroo.fanfix.data;

import java.net.URL;

/**
 * A paragraph in a chapter of the story.
 * 
 * @author niki
 */
public class Paragraph {
	/**
	 * A paragraph type, that will dictate how the paragraph will be handled.
	 * 
	 * @author niki
	 */
	public enum ParagraphType {
		/** Normal paragraph (text) */
		NORMAL,
		/** Blank line */
		BLANK,
		/** A Break paragraph, i.e.: HR (Horizontal Line) or '* * *' or whatever */
		BREAK,
		/** Quotation (dialogue) */
		QUOTE,
		/** An image (no text) */
		IMAGE,
	}

	private ParagraphType type;
	private String content;

	/**
	 * Create a new {@link Paragraph} with the given values.
	 * 
	 * @param type
	 *            the {@link ParagraphType}
	 * @param content
	 *            the content of this paragraph
	 */
	public Paragraph(ParagraphType type, String content) {
		this.type = type;
		this.content = content;
	}

	/**
	 * Create a new {@link Paragraph} with the given image.
	 * 
	 * @param support
	 *            the support that will be used to fetch the image via
	 *            {@link Paragraph#getContentImage()}.
	 * @param content
	 *            the content image of this paragraph
	 */
	public Paragraph(URL imageUrl) {
		this.type = ParagraphType.IMAGE;
		this.content = imageUrl.toString();
	}

	/**
	 * The {@link ParagraphType}.
	 * 
	 * @return the type
	 */
	public ParagraphType getType() {
		return type;
	}

	/**
	 * The {@link ParagraphType}.
	 * 
	 * @param type
	 *            the type to set
	 */
	public void setType(ParagraphType type) {
		this.type = type;
	}

	/**
	 * The content of this {@link Paragraph}.
	 * 
	 * @return the content
	 */
	public String getContent() {
		return content;
	}

	/**
	 * The content of this {@link Paragraph}.
	 * 
	 * @param content
	 *            the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}

	/**
	 * Display a DEBUG {@link String} representation of this object.
	 */
	@Override
	public String toString() {
		return String.format("%s: [%s]", "" + type, "" + content);
	}
}
