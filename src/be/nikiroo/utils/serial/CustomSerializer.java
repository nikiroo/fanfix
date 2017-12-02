package be.nikiroo.utils.serial;

import java.io.IOException;

public abstract class CustomSerializer {

	protected abstract String toString(Object value);

	protected abstract Object fromString(String content) throws IOException;

	protected abstract String getType();

	/**
	 * Encode the object into the given builder if possible (if supported).
	 * 
	 * @param builder
	 *            the builder to append to
	 * @param value
	 *            the object to encode
	 * @return TRUE if success, FALSE if not (the content of the builder won't
	 *         be changed in case of failure)
	 */
	public boolean encode(StringBuilder builder, Object value) {
		int prev = builder.length();
		String customString = toString(value);
		builder.append("custom^").append(getType()).append("^");
		if (!SerialUtils.encode(builder, customString)) {
			builder.delete(prev, builder.length());
			return false;
		}

		return true;
	}

	public Object decode(String encodedValue) throws IOException {
		return fromString((String) SerialUtils.decode(contentOf(encodedValue)));
	}

	public static boolean isCustom(String encodedValue) {
		int pos1 = encodedValue.indexOf('^');
		int pos2 = encodedValue.indexOf('^', pos1 + 1);

		return pos1 >= 0 && pos2 >= 0 && encodedValue.startsWith("custom^");
	}

	public static String typeOf(String encodedValue) {
		int pos1 = encodedValue.indexOf('^');
		int pos2 = encodedValue.indexOf('^', pos1 + 1);
		String type = encodedValue.substring(pos1 + 1, pos2);

		return type;
	}

	public static String contentOf(String encodedValue) {
		int pos1 = encodedValue.indexOf('^');
		int pos2 = encodedValue.indexOf('^', pos1 + 1);
		String encodedContent = encodedValue.substring(pos2 + 1);

		return encodedContent;
	}
}
