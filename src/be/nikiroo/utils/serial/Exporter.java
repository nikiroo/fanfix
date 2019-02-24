package be.nikiroo.utils.serial;

import java.io.NotSerializableException;
import java.util.HashMap;
import java.util.Map;

import be.nikiroo.utils.StringUtils;

/**
 * A simple class to serialise objects to {@link String}.
 * <p>
 * This class does not support inner classes (it does support nested classes,
 * though).
 * 
 * @author niki
 */
public class Exporter {
	private Map<Integer, Object> map;
	private StringBuilder builder;

	/**
	 * Create a new {@link Exporter}.
	 */
	public Exporter() {
		map = new HashMap<Integer, Object>();
		builder = new StringBuilder();
	}

	/**
	 * Serialise the given object and add it to the list.
	 * <p>
	 * <b>Important: </b>If the operation fails (with a
	 * {@link NotSerializableException}), the {@link Exporter} will be corrupted
	 * (will contain bad, most probably not importable data).
	 * 
	 * @param o
	 *            the object to serialise
	 * @return this (for easier appending of multiple values)
	 * 
	 * @throws NotSerializableException
	 *             if the object cannot be serialised (in this case, the
	 *             {@link Exporter} can contain bad, most probably not
	 *             importable data)
	 */
	public Exporter append(Object o) throws NotSerializableException {
		SerialUtils.append(builder, o, map);
		return this;
	}

	/**
	 * Clear the current content.
	 */
	public void clear() {
		builder.setLength(0);
		map.clear();
	}

	/**
	 * The exported items in a serialised form.
	 * 
	 * @param zip
	 *            TRUE to have zipped (and BASE64-coded) content, FALSE to have
	 *            raw content, NULL to let the system decide
	 * 
	 * @return the items currently in this {@link Exporter}
	 */
	public String toString(Boolean zip) {
		if (zip == null && builder.length() > 128) {
			zip = false;
		}

		if (zip == null || zip) {
			String zipped = "ZIP:" + StringUtils.zip64(builder.toString());

			if (zip != null || builder.length() < zipped.length())
				return zipped;
		}

		return builder.toString();
	}

	/**
	 * The exported items in a serialised form (possibly zipped).
	 * 
	 * @return the items currently in this {@link Exporter}
	 */
	@Override
	public String toString() {
		return toString(null);
	}
}