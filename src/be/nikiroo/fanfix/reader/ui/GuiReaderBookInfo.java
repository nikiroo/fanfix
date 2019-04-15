package be.nikiroo.fanfix.reader.ui;

import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.StringUtils;

/**
 * Some meta information related to a "book" (which can either be a
 * {@link Story}, a fake-story grouping some authors or a fake-story grouping
 * some sources/types).
 * 
 * @author niki
 */
public class GuiReaderBookInfo {
	public enum Type {
		/** A normal story, which can be "read". */
		STORY,
		/**
		 * A special, empty story that represents a source/type common to one or
		 * more normal stories.
		 */
		SOURCE,
		/** A special, empty story that represents an author. */
		AUTHOR
	}

	private Type type;
	private String id;
	private String value;
	private String count;

	private MetaData meta;

	/**
	 * For private use, see the "fromXXX" constructors instead for public use.
	 * 
	 * @param type
	 *            the type of book
	 * @param id
	 *            the main id, which must uniquely identify this book and will
	 *            be used as a unique ID later on
	 * @param value
	 *            the main value to show (see
	 *            {@link GuiReaderBookInfo#getMainInfo()})
	 */
	private GuiReaderBookInfo(Type type, String id, String value) {
		this.type = type;
		this.id = id;
		this.value = value;
	}

	/**
	 * Get the main info to display for this book (a title, an author, a
	 * source/type name...).
	 * <p>
	 * Note that when {@link MetaData} about the book are present, the title
	 * inside is returned instead of the actual value (that way, we can update
	 * the {@link MetaData} and see the changes here).
	 * 
	 * @return the main info, usually the title
	 */
	public String getMainInfo() {
		if (meta != null) {
			return meta.getTitle();
		}

		return value;
	}

	/**
	 * Get the secondary info, of the given type.
	 * 
	 * @param seeCount
	 *            TRUE for word/image/story count, FALSE for author name
	 * 
	 * @return the secondary info
	 */
	public String getSecondaryInfo(boolean seeCount) {
		String author = meta == null ? null : meta.getAuthor();
		String secondaryInfo = seeCount ? count : author;

		if (secondaryInfo != null && !secondaryInfo.trim().isEmpty()) {
			secondaryInfo = "(" + secondaryInfo + ")";
		} else {
			secondaryInfo = "";
		}

		return secondaryInfo;
	}

	/**
	 * A unique ID for this {@link GuiReaderBookInfo}.
	 * 
	 * @return the unique ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * The {@link MetaData} associated with this book, if this book is a
	 * {@link Story}.
	 * <p>
	 * Can be NULL for non-story books (authors or sources/types).
	 * 
	 * @return the {@link MetaData} or NULL
	 */
	public MetaData getMeta() {
		return meta;
	}

	/**
	 * Get the base image to use to represent this book.
	 * <p>
	 * The image is <b>NOT</b> resized in any way, this is the original version.
	 * <p>
	 * It can be NULL if no image can be found for this book.
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to use to fetch the image
	 * 
	 * @return the base image
	 */
	public Image getBaseImage(BasicLibrary lib) {
		switch (type) {
		case STORY:
			return lib.getCover(meta.getLuid());
		case SOURCE:
			return lib.getSourceCover(value);
		case AUTHOR:
			return lib.getAuthorCover(value);
		}

		return null;
	}

	/**
	 * Create a new book describing the given {@link Story}.
	 * 
	 * @param meta
	 *            the {@link MetaData} representing the {@link Story}
	 * 
	 * @return the book
	 */
	static public GuiReaderBookInfo fromMeta(MetaData meta) {
		String uid = meta.getUuid();
		if (uid == null || uid.trim().isEmpty()) {
			uid = meta.getLuid();
		}

		GuiReaderBookInfo info = new GuiReaderBookInfo(Type.STORY, uid,
				meta.getTitle());

		info.meta = meta;
		info.count = StringUtils.formatNumber(meta.getWords());
		if (!info.count.isEmpty()) {
			info.count = GuiReader.trans(
					meta.isImageDocument() ? StringIdGui.BOOK_COUNT_IMAGES
							: StringIdGui.BOOK_COUNT_WORDS, info.count);
		}

		return info;
	}

	/**
	 * Create a new book describing the given source/type.
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to use to retrieve some more
	 *            information about the source
	 * @param source
	 *            the source name
	 * 
	 * @return the book
	 */
	static public GuiReaderBookInfo fromSource(BasicLibrary lib, String source) {
		GuiReaderBookInfo info = new GuiReaderBookInfo(Type.SOURCE, "source_"
				+ source, source);

		info.count = StringUtils.formatNumber(lib.getListBySource(source)
				.size());
		if (!info.count.isEmpty()) {
			info.count = GuiReader.trans(StringIdGui.BOOK_COUNT_STORIES,
					info.count);
		}

		return info;
	}

	/**
	 * Create a new book describing the given author.
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to use to retrieve some more
	 *            information about the author
	 * @param author
	 *            the author name
	 * 
	 * @return the book
	 */
	static public GuiReaderBookInfo fromAuthor(BasicLibrary lib, String author) {
		GuiReaderBookInfo info = new GuiReaderBookInfo(Type.AUTHOR, "author_"
				+ author, author);

		info.count = StringUtils.formatNumber(lib.getListByAuthor(author)
				.size());
		if (!info.count.isEmpty()) {
			info.count = GuiReader.trans(StringIdGui.BOOK_COUNT_STORIES,
					info.count);
		}

		return info;
	}
}
