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

	public Exporter() {
		map = new HashMap<Integer, Object>();
		builder = new StringBuilder();
	}

	public Exporter append(Object o) throws NotSerializableException {
		SerialUtils.append(builder, o, map);
		return this;
	}

	public void clear() {
		builder.setLength(0);
		map.clear();
	}

	// null = auto
	public String toString(Boolean zip) {
		if (zip == null) {
			zip = builder.length() > 128;
		}

		if (zip) {
			return "ZIP:" + StringUtils.zip64(builder.toString());
		}

		return builder.toString();
	}

	/**
	 * The exported items in a serialised form.
	 * 
	 * @return the items currently in this {@link Exporter}.
	 */
	@Override
	public String toString() {
		return toString(null);
	}
}