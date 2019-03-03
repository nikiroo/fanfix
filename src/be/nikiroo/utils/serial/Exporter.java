package be.nikiroo.utils.serial;

import java.io.IOException;
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
	 * Append the exported items in a serialised form into the given
	 * {@link StringBuilder}.
	 * 
	 * @param toBuilder
	 *            the {@link StringBuilder}
	 * @param b64
	 *            TRUE to have BASE64-coded content, FALSE to have raw content,
	 *            NULL to let the system decide
	 * @param zip
	 *            TRUE to zip the BASE64 output if the output is indeed in
	 *            BASE64 format, FALSE not to
	 */
	public void appendTo(StringBuilder toBuilder, Boolean b64, boolean zip) {
		if (b64 == null && builder.length() < 128) {
			b64 = false;
		}

		if (b64 != null || b64) {
			try {
				String zipped = StringUtils.base64(builder.toString(), zip);
				if (b64 != null || zipped.length() < builder.length() - 4) {
					toBuilder.append(zip ? "ZIP:" : "B64:");
					toBuilder.append(zipped);
					return;
				}
			} catch (IOException e) {
				throw new RuntimeException(
						"Base64 conversion of data failed, maybe not enough memory?",
						e);
			}
		}

		toBuilder.append(builder);
	}

	/**
	 * The exported items in a serialised form.
	 * 
	 * @deprecated use {@link Exporter#toString(Boolean, boolean)} instead
	 * 
	 * @param zip
	 *            TRUE to have zipped (and BASE64-coded) content, FALSE to have
	 *            raw content, NULL to let the system decide
	 * 
	 * @return the items currently in this {@link Exporter}
	 */
	@Deprecated
	public String toString(Boolean zip) {
		return toString(zip, zip == null || zip);
	}

	/**
	 * The exported items in a serialised form.
	 * 
	 * @param b64
	 *            TRUE to have BASE64-coded content, FALSE to have raw content,
	 *            NULL to let the system decide
	 * @param zip
	 *            TRUE to zip the BASE64 output if the output is indeed in
	 *            BASE64 format, FALSE not to
	 * 
	 * @return the items currently in this {@link Exporter}
	 */
	public String toString(Boolean b64, boolean zip) {
		StringBuilder toBuilder = new StringBuilder();
		appendTo(toBuilder, b64, zip);
		return toBuilder.toString();
	}

	/**
	 * The exported items in a serialised form (possibly BASE64-coded, possibly
	 * zipped).
	 * 
	 * @return the items currently in this {@link Exporter}
	 */
	@Override
	public String toString() {
		return toString(null, true);
	}
}