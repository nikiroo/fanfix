package be.nikiroo.fanfix.reader.ui;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.utils.Image;

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

	// use the fromXXX methods
	private GuiReaderBookInfo(Type type, String id, String value) {
		this.type = type;
		this.id = id;
		this.value = value;
	}

	public String getMainInfo() {
		if (meta != null) {
			return meta.getTitle();
		}

		return value;
	}

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

	// can return null for non-books
	public MetaData getMeta() {
		return meta;
	}

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

	static public GuiReaderBookInfo fromMeta(MetaData meta) {
		String uid = meta.getUuid();
		if (uid == null || uid.trim().isEmpty()) {
			uid = meta.getLuid();
		}

		GuiReaderBookInfo info = new GuiReaderBookInfo(Type.STORY, uid,
				meta.getTitle());

		info.meta = meta;
		info.count = formatNumber(meta.getWords(),
				meta.isImageDocument() ? "images" : "words");

		return info;
	}

	static public GuiReaderBookInfo fromSource(BasicLibrary lib, String source) {
		GuiReaderBookInfo info = new GuiReaderBookInfo(Type.SOURCE, "source_"
				+ source, source);

		info.count = formatNumber(lib.getListBySource(source).size(), "stories");

		return info;
	}

	static public GuiReaderBookInfo fromAuthor(BasicLibrary lib, String author) {
		GuiReaderBookInfo info = new GuiReaderBookInfo(Type.AUTHOR, "author_"
				+ author, author);

		info.count = formatNumber(lib.getListByAuthor(author).size(), "stories");

		return info;
	}

	static private String formatNumber(long number, String ofWhat) {
		String displayNumber;
		if (number >= 4000) {
			displayNumber = "" + (number / 1000) + "k";
		} else if (number > 0) {
			displayNumber = "" + number;
		} else {
			displayNumber = "";
		}

		if (!displayNumber.isEmpty()) {
			displayNumber += " " + ofWhat;
		}

		return displayNumber;
	}
}
