package be.nikiroo.utils.serial;

import java.io.NotSerializableException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Small class to help serialise/deserialise objects.
 * <p>
 * Note that we do not support inner classes (but we do support nested classes)
 * and all objects require an empty constructor to be deserialised.
 * 
 * @author niki
 */
class SerialUtils {
	private static Map<String, CustomSerializer> customTypes;

	static {
		customTypes = new HashMap<String, CustomSerializer>();
		// TODO: add "default" custom serialisers
	}

	static public void addCustomSerializer(CustomSerializer serializer) {
		customTypes.put(serializer.getType(), serializer);
	}

	static void append(StringBuilder builder, Object o, Map<Integer, Object> map)
			throws NotSerializableException {

		Field[] fields = new Field[] {};
		String type = "";
		String id = "NULL";

		if (o != null) {
			int hash = System.identityHashCode(o);
			fields = o.getClass().getDeclaredFields();
			type = o.getClass().getCanonicalName();
			if (type == null) {
				throw new NotSerializableException(
						String.format(
								"Cannot find the class for this object: %s (it could be an inner class, which is not supported)",
								o));
			}
			id = Integer.toString(hash);
			if (map.containsKey(hash)) {
				fields = new Field[] {};
			} else {
				map.put(hash, o);
			}
		}

		builder.append("{\nREF ").append(type).append("@").append(id);
		try {
			for (Field field : fields) {
				field.setAccessible(true);

				if (field.getName().startsWith("this$")) {
					// Do not keep this links of nested classes
					continue;
				}

				builder.append("\n");
				builder.append(field.getName());
				builder.append(":");
				Object value;

				value = field.get(o);

				if (!encode(builder, value)) {
					builder.append("\n");
					append(builder, value, map);
				}
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace(); // should not happen (see setAccessible)
		} catch (IllegalAccessException e) {
			e.printStackTrace(); // should not happen (see setAccessible)
		}
		builder.append("\n}");
	}

	// return true if encoded (supported)
	static boolean encode(StringBuilder builder, Object value) {
		if (value == null) {
			builder.append("NULL");
		} else if (customTypes.containsKey(value.getClass().getCanonicalName())) {
			customTypes.get(value.getClass().getCanonicalName())//
					.encode(builder, value);
		} else if (value instanceof String) {
			encodeString(builder, (String) value);
		} else if (value instanceof Boolean) {
			builder.append(value);
		} else if (value instanceof Byte) {
			builder.append(value).append('b');
		} else if (value instanceof Character) {
			encodeString(builder, (String) value);
			builder.append('c');
		} else if (value instanceof Short) {
			builder.append(value).append('s');
		} else if (value instanceof Integer) {
			builder.append(value);
		} else if (value instanceof Long) {
			builder.append(value).append('L');
		} else if (value instanceof Float) {
			builder.append(value).append('F');
		} else if (value instanceof Double) {
			builder.append(value).append('d');
		} else {
			return false;
		}

		return true;
	}

	static Object decode(String encodedValue) {
		String cut = "";
		if (encodedValue.length() > 1) {
			cut = encodedValue.substring(0, encodedValue.length() - 1);
		}

		if (CustomSerializer.isCustom(encodedValue)) {
			// custom:TYPE_NAME:"content is String-encoded"
			String type = CustomSerializer.typeOf(encodedValue);
			if (customTypes.containsKey(type)) {
				return customTypes.get(type).decode(encodedValue);
			} else {
				throw new java.util.UnknownFormatConversionException(
						"Unknown custom type: " + type);
			}
		} else if (encodedValue.equals("NULL") || encodedValue.equals("null")) {
			return null;
		} else if (encodedValue.endsWith("\"")) {
			return decodeString(encodedValue);
		} else if (encodedValue.equals("true")) {
			return true;
		} else if (encodedValue.equals("false")) {
			return false;
		} else if (encodedValue.endsWith("b")) {
			return Byte.parseByte(cut);
		} else if (encodedValue.endsWith("c")) {
			return decodeString(cut).charAt(0);
		} else if (encodedValue.endsWith("s")) {
			return Short.parseShort(cut);
		} else if (encodedValue.endsWith("L")) {
			return Long.parseLong(cut);
		} else if (encodedValue.endsWith("F")) {
			return Float.parseFloat(cut);
		} else if (encodedValue.endsWith("d")) {
			return Double.parseDouble(cut);
		} else {
			return Integer.parseInt(encodedValue);
		}
	}

	// aa bb -> "aa\tbb"
	private static void encodeString(StringBuilder builder, String raw) {
		builder.append('\"');
		for (char car : raw.toCharArray()) {
			switch (car) {
			case '\\':
				builder.append("\\\\");
				break;
			case '\r':
				builder.append("\\r");
				break;
			case '\n':
				builder.append("\\n");
				break;
			case '"':
				builder.append("\\\"");
				break;
			default:
				builder.append(car);
				break;
			}
		}
		builder.append('\"');
	}

	// "aa\tbb" -> aa bb
	private static String decodeString(String escaped) {
		StringBuilder builder = new StringBuilder();

		boolean escaping = false;
		for (char car : escaped.toCharArray()) {
			if (!escaping) {
				if (car == '\\') {
					escaping = true;
				} else {
					builder.append(car);
				}
			} else {
				switch (car) {
				case '\\':
					builder.append('\\');
					break;
				case 'r':
					builder.append('\r');
					break;
				case 'n':
					builder.append('\n');
					break;
				case '"':
					builder.append('"');
					break;
				}
				escaping = false;
			}
		}

		return builder.substring(1, builder.length() - 1).toString();
	}
}
