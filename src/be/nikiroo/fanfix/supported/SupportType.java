package be.nikiroo.fanfix.supported;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringId;

/**
 * The supported input types for which we can get a {@link BasicSupport} object.
 * 
 * @author niki
 */
public enum SupportType {
	/** EPUB files created with this program */
	EPUB,
	/** Pure text file with some rules */
	TEXT,
	/** TEXT but with associated .info file */
	INFO_TEXT,
	/** My Little Pony fanfictions */
	FIMFICTION,
	/** Fanfictions from a lot of different universes */
	FANFICTION,
	/** Website with lots of Mangas */
	MANGAFOX,
	/** Furry website with comics support */
	E621,
	/** Furry website with stories */
	YIFFSTAR,
	/** Comics and images groups, mostly but not only NSFW */
	E_HENTAI,
	/** CBZ files */
	CBZ,
	/** HTML files */
	HTML;

	/**
	 * A description of this support type (more information than the
	 * {@link BasicSupport#getSourceName()}).
	 * 
	 * @return the description
	 */
	public String getDesc() {
		String desc = Instance.getTrans().getStringX(StringId.INPUT_DESC,
				this.name());

		if (desc == null) {
			desc = Instance.getTrans().getString(StringId.INPUT_DESC, this);
		}

		return desc;
	}

	/**
	 * The name of this support type (a short version).
	 * 
	 * @return the name
	 */
	public String getSourceName() {
		BasicSupport support = BasicSupport.getSupport(this, null);
		if (support != null) {
			return support.getSourceName();
		}

		return null;
	}

	@Override
	public String toString() {
		return super.toString().toLowerCase();
	}

	/**
	 * Call {@link SupportType#valueOf(String)} after conversion to upper case.
	 * 
	 * @param typeName
	 *            the possible type name
	 * 
	 * @return NULL or the type
	 */
	public static SupportType valueOfUC(String typeName) {
		return SupportType.valueOf(typeName == null ? null : typeName
				.toUpperCase());
	}

	/**
	 * Call {@link SupportType#valueOf(String)} after conversion to upper case
	 * but return NULL for NULL instead of raising exception.
	 * 
	 * @param typeName
	 *            the possible type name
	 * 
	 * @return NULL or the type
	 */
	public static SupportType valueOfNullOkUC(String typeName) {
		if (typeName == null) {
			return null;
		}

		return SupportType.valueOfUC(typeName);
	}

	/**
	 * Call {@link SupportType#valueOf(String)} after conversion to upper case
	 * but return NULL in case of error instead of raising an exception.
	 * 
	 * @param typeName
	 *            the possible type name
	 * 
	 * @return NULL or the type
	 */
	public static SupportType valueOfAllOkUC(String typeName) {
		try {
			return SupportType.valueOfUC(typeName);
		} catch (Exception e) {
			return null;
		}
	}
}