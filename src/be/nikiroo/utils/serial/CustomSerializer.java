package be.nikiroo.utils.serial;

public abstract class CustomSerializer {

	protected abstract String toString(Object value);

	protected abstract Object fromString(String content);

	protected abstract String getType();

	public void encode(StringBuilder builder, Object value) {
		String customString = toString(value);
		builder.append("custom:").append(getType()).append(":");
		SerialUtils.encode(builder, customString);
	}

	public Object decode(String encodedValue) {
		return fromString((String) SerialUtils.decode(contentOf(encodedValue)));
	}

	public static boolean isCustom(String encodedValue) {
		int pos1 = encodedValue.indexOf(':');
		int pos2 = encodedValue.indexOf(':', pos1 + 1);

		return pos1 >= 0 && pos2 >= 0 && encodedValue.startsWith("custom:");
	}

	public static String typeOf(String encodedValue) {
		int pos1 = encodedValue.indexOf(':');
		int pos2 = encodedValue.indexOf(':', pos1 + 1);
		String type = encodedValue.substring(pos1 + 1, pos2);

		return type;
	}

	public static String contentOf(String encodedValue) {
		int pos1 = encodedValue.indexOf(':');
		int pos2 = encodedValue.indexOf(':', pos1 + 1);
		String encodedContent = encodedValue.substring(pos2 + 1);

		return encodedContent;
	}
}
