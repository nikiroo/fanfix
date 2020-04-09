package be.nikiroo.fanfix_swing.gui.book;

import java.awt.print.Book;
import java.io.IOException;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.CacheLibrary;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.StringUtils;

/**
 * Some meta information related to a "book" (which can either be a
 * {@link Story}, a fake-story grouping some authors or a fake-story grouping
 * some sources/types).
 * 
 * @author niki
 */
public class BookInfo {
	/**
	 * The type of {@link Book} (i.e., related to a story or to something else that
	 * can encompass stories).
	 * 
	 * @author niki
	 */
	public enum Type {
		/** A normal story, which can be "read". */
		STORY,
		/**
		 * A special, empty story that represents a source/type common to one or more
		 * normal stories.
		 */
		SOURCE,
		/** A special, empty story that represents an author. */
		AUTHOR,
		/** A special, empty story that represents a tag. **/
		TAG
	}

	private Type type;
	private String id;
	private String value;
	private String count;

	private boolean cached;

	private MetaData meta;

	/**
	 * For private use; see the "fromXXX" constructors instead for public use.
	 * 
	 * @param type  the type of book
	 * @param id    the main id, which must uniquely identify this book and will be
	 *              used as a unique ID later on
	 * @param value the main value to show (see {@link BookInfo#getMainInfo()})
	 */
	protected BookInfo(Type type, String id, String value) {
		this.type = type;
		this.id = id;
		this.value = value;
	}

	/**
	 * The type of {@link BookInfo}.
	 * 
	 * @return the type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Get the main info to display for this book (a title, an author, a source/type
	 * name...).
	 * <p>
	 * Note that when {@link MetaData} about the book are present, the title inside
	 * is returned instead of the actual value (that way, we can update the
	 * {@link MetaData} and see the changes here).
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
	 * @param seeCount TRUE for word/image/story count, FALSE for author name
	 * 
	 * @return the secondary info, never NULL
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
	 * A unique ID for this {@link BookInfo}.
	 * 
	 * @return the unique ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * This item library cache state.
	 * 
	 * @return TRUE if it is present in the {@link CacheLibrary} cache
	 */
	public boolean isCached() {
		return cached;
	}

	/**
	 * This item library cache state.
	 * 
	 * @param cached TRUE if it is present in the {@link CacheLibrary} cache
	 */
	public void setCached(boolean cached) {
		this.cached = cached;
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
	 * @param lib the {@link BasicLibrary} to use to fetch the image (can be NULL)
	 * 
	 * @return the base image, or NULL if no library or no image
	 * 
	 * @throws IOException in case of I/O error
	 */
	public Image getBaseImage(BasicLibrary lib) throws IOException {
		if (lib != null) {
			switch (type) {
			case STORY:
				if (meta.getCover() != null) {
					return meta.getCover();
				}

				if (meta.getLuid() != null) {
					return lib.getCover(meta.getLuid());
				}

				return null;
			case SOURCE:
				return lib.getSourceCover(value);
			case AUTHOR:
				return lib.getAuthorCover(value);
			case TAG:
				return null;
			}
		}

		return null;
	}
	
	/**
	 * This {@link BookInfo} could have a cover (so we need to somehow represent
	 * that to the user).
	 * 
	 * @return TRUE if it does
	 */
	public boolean supportsCover() {
		return type != Type.TAG;
	}

	/**
	 * Create a new book describing the given {@link Story}.
	 * 
	 * @param lib  the {@link BasicLibrary} to use to retrieve some more information
	 *             about the source
	 * @param meta the {@link MetaData} representing the {@link Story}
	 * 
	 * @return the book
	 */
	static public BookInfo fromMeta(BasicLibrary lib, MetaData meta) {
		String uid = meta.getUuid();
		if (uid == null || uid.trim().isEmpty()) {
			uid = meta.getLuid();
		}
		if (uid == null || uid.trim().isEmpty()) {
			uid = meta.getUrl();
		}

		BookInfo info = new BookInfo(Type.STORY, uid, meta.getTitle());

		info.meta = meta;
		info.count = StringUtils.formatNumber(meta.getWords());
		if (!info.count.isEmpty()) {
			info.count = Instance.getInstance().getTransGui().getString(
					meta.isImageDocument() ? StringIdGui.BOOK_COUNT_IMAGES : StringIdGui.BOOK_COUNT_WORDS,
					new Object[] { info.count });
		}

		if (lib instanceof CacheLibrary) {
			info.setCached(((CacheLibrary) lib).isCached(meta.getLuid()));
		} else {
			info.setCached(true);
		}

		return info;
	}

	/**
	 * Create a new book describing the given source/type.
	 * 
	 * @param lib    the {@link BasicLibrary} to use to retrieve some more
	 *               information about the source
	 * @param source the source name
	 * 
	 * @return the book
	 */
	static public BookInfo fromSource(BasicLibrary lib, String source) {
		BookInfo info = new BookInfo(Type.SOURCE, "source_" + (source == null ? "" : source), source);

		int size = 0;
		try {
			size = lib.getList().filter(source, null, null).size();
		} catch (IOException e) {
		}

		info.count = StringUtils.formatNumber(size);
		if (!info.count.isEmpty()) {
			info.count = Instance.getInstance().getTransGui().getString(StringIdGui.BOOK_COUNT_STORIES,
					new Object[] { info.count });
		}

		return info;
	}

	/**
	 * Create a new book describing the given author.
	 * 
	 * @param lib    the {@link BasicLibrary} to use to retrieve some more
	 *               information about the author
	 * @param author the author name
	 * 
	 * @return the book
	 */
	static public BookInfo fromAuthor(BasicLibrary lib, String author) {
		BookInfo info = new BookInfo(Type.AUTHOR, "author_" + (author == null ? "" : author), author);

		int size = 0;
		try {
			size = lib.getList().filter(null, author, null).size();
		} catch (IOException e) {
		}

		info.count = StringUtils.formatNumber(size);
		if (!info.count.isEmpty()) {
			info.count = Instance.getInstance().getTransGui().getString(StringIdGui.BOOK_COUNT_STORIES,
					new Object[] { info.count });
		}

		return info;
	}

	/**
	 * Create a new book describing the given tag.
	 * 
	 * @param lib the {@link BasicLibrary} to use to retrieve some more information
	 *            about the tag
	 * @param tag the tag name
	 * 
	 * @return the book
	 */
	static public BookInfo fromTag(BasicLibrary lib, String tag) {
		BookInfo info = new BookInfo(Type.TAG, "tag_" + (tag == null ? "" : tag), tag);

		int size = 0;
		try {
			size = lib.getList().filter(null, null, tag).size();
		} catch (IOException e) {
		}

		info.count = StringUtils.formatNumber(size);
		if (!info.count.isEmpty()) {
			info.count = Instance.getInstance().getTransGui().getString(StringIdGui.BOOK_COUNT_STORIES,
					new Object[] { info.count });
		}

		return info;
	}
}
