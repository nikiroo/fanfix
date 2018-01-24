package be.nikiroo.utils.serial;

import java.io.IOException;
import java.io.NotSerializableException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UnknownFormatConversionException;

import be.nikiroo.utils.Image;

/**
 * Small class to help with serialisation.
 * <p>
 * Note that we do not support inner classes (but we do support nested classes)
 * and all objects require an empty constructor to be deserialised.
 * <p>
 * It is possible to add support to custom types (both the encoder and the
 * decoder will require the custom classes) -- see {@link CustomSerializer}.
 * <p>
 * Default supported types are:
 * <ul>
 * <li>NULL (as a null value)</li>
 * <li>String</li>
 * <li>Boolean</li>
 * <li>Byte</li>
 * <li>Character</li>
 * <li>Short</li>
 * <li>Long</li>
 * <li>Float</li>
 * <li>Double</li>
 * <li>Integer</li>
 * <li>Enum (any enum whose name and value is known by the caller)</li>
 * <li>java.awt.image.BufferedImage (as a {@link CustomSerializer})</li>
 * <li>An array of the above (as a {@link CustomSerializer})</li>
 * <li>URL</li>
 * </ul>
 * 
 * @author niki
 */
public class SerialUtils {
	private static Map<String, CustomSerializer> customTypes;

	static {
		customTypes = new HashMap<String, CustomSerializer>();

		// Array types:
		customTypes.put("[]", new CustomSerializer() {
			@Override
			protected String toString(Object value) {
				String type = value.getClass().getCanonicalName();
				type = type.substring(0, type.length() - 2); // remove the []

				StringBuilder builder = new StringBuilder();
				builder.append(type).append("\n");
				try {
					for (int i = 0; true; i++) {
						Object item = Array.get(value, i);
						// encode it normally if direct value
						if (!SerialUtils.encode(builder, item)) {
							try {
								// use ZIP: if not
								builder.append(new Exporter().append(item)
										.toString(true));
							} catch (NotSerializableException e) {
								throw new UnknownFormatConversionException(e
										.getMessage());
							}
						}
						builder.append("\n");
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					// Done.
				}

				return builder.toString();
			}

			@Override
			protected String getType() {
				return "[]";
			}

			@Override
			protected Object fromString(String content) throws IOException {
				String[] tab = content.split("\n");

				try {
					Object array = Array.newInstance(
							SerialUtils.getClass(tab[0]), tab.length - 1);
					for (int i = 1; i < tab.length; i++) {
						Object value = new Importer().read(tab[i]).getValue();
						Array.set(array, i - 1, value);
					}

					return array;
				} catch (Exception e) {
					if (e instanceof IOException) {
						throw (IOException) e;
					}
					throw new IOException(e.getMessage());
				}
			}
		});

		// URL:
		customTypes.put("java.net.URL", new CustomSerializer() {
			@Override
			protected String toString(Object value) {
				if (value != null) {
					return ((URL) value).toString();
				}
				return null;
			}

			@Override
			protected Object fromString(String content) throws IOException {
				if (content != null) {
					return new URL(content);
				}
				return null;
			}

			@Override
			protected String getType() {
				return "java.net.URL";
			}
		});

		// Images (this is currently the only supported image type by default)
		customTypes.put("be.nikiroo.utils.Image", new CustomSerializer() {
			@Override
			protected String toString(Object value) {
				return ((Image) value).toBase64();
			}

			@Override
			protected String getType() {
				return "be.nikiroo.utils.Image";
			}

			@Override
			protected Object fromString(String content) {
				try {
					return new Image(content);
				} catch (IOException e) {
					throw new UnknownFormatConversionException(e.getMessage());
				}
			}
		});
	}

	/**
	 * Create an empty object of the given type.
	 * 
	 * @param type
	 *            the object type (its class name)
	 * 
	 * @return the new object
	 * 
	 * @throws ClassNotFoundException
	 *             if the class cannot be found
	 * @throws NoSuchMethodException
	 *             if the given class is not compatible with this code
	 */
	public static Object createObject(String type)
			throws ClassNotFoundException, NoSuchMethodException {

		try {
			Class<?> clazz = getClass(type);
			String className = clazz.getName();
			Object[] args = null;
			Constructor<?> ctor = null;
			if (className.contains("$")) {
				Object javaParent = createObject(className.substring(0,
						className.lastIndexOf('$')));
				args = new Object[] { javaParent };
				ctor = clazz.getDeclaredConstructor(new Class[] { javaParent
						.getClass() });
			} else {
				args = new Object[] {};
				ctor = clazz.getDeclaredConstructor();
			}

			ctor.setAccessible(true);
			return ctor.newInstance(args);
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (NoSuchMethodException e) {
			throw e;
		} catch (Exception e) {
			throw new NoSuchMethodException("Cannot instantiate: " + type);
		}
	}

	/**
	 * Insert a custom serialiser that will take precedence over the default one
	 * or the target class.
	 * 
	 * @param serializer
	 *            the custom serialiser
	 */
	static public void addCustomSerializer(CustomSerializer serializer) {
		customTypes.put(serializer.getType(), serializer);
	}

	/**
	 * Serialise the given object into this {@link StringBuilder}.
	 * <p>
	 * <b>Important: </b>If the operation fails (with a
	 * {@link NotSerializableException}), the {@link StringBuilder} will be
	 * corrupted (will contain bad, most probably not importable data).
	 * 
	 * @param builder
	 *            the output {@link StringBuilder} to serialise to
	 * @param o
	 *            the object to serialise
	 * @param map
	 *            the map of already serialised objects (if the given object or
	 *            one of its descendant is already present in it, only an ID
	 *            will be serialised)
	 * 
	 * @throws NotSerializableException
	 *             if the object cannot be serialised (in this case, the
	 *             {@link StringBuilder} can contain bad, most probably not
	 *             importable data)
	 */
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

		builder.append("{\nREF ").append(type).append("@").append(id)
				.append(":");
		if (!encode(builder, o)) { // check if direct value
			try {
				for (Field field : fields) {
					field.setAccessible(true);

					if (field.getName().startsWith("this$")
							|| field.isSynthetic()
							|| (field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
						// Do not keep this links of nested classes
						// Do not keep synthetic fields
						// Do not keep final fields
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
				e.printStackTrace(); // should not happen (see
										// setAccessible)
			} catch (IllegalAccessException e) {
				e.printStackTrace(); // should not happen (see
										// setAccessible)
			}
		}
		builder.append("\n}");
	}

	/**
	 * Encode the object into the given builder if possible (if supported).
	 * 
	 * @param builder
	 *            the builder to append to
	 * @param value
	 *            the object to encode (can be NULL, which will be encoded)
	 * 
	 * @return TRUE if success, FALSE if not (the content of the builder won't
	 *         be changed in case of failure)
	 */
	static boolean encode(StringBuilder builder, Object value) {
		if (value == null) {
			builder.append("NULL");
		} else if (value.getClass().getCanonicalName().endsWith("[]")) {
			return customTypes.get("[]").encode(builder, value);
		} else if (customTypes.containsKey(value.getClass().getCanonicalName())) {
			return customTypes.get(value.getClass().getCanonicalName())//
					.encode(builder, value);
		} else if (value instanceof String) {
			encodeString(builder, (String) value);
		} else if (value instanceof Boolean) {
			builder.append(value);
		} else if (value instanceof Byte) {
			builder.append(value).append('b');
		} else if (value instanceof Character) {
			encodeString(builder, "" + value);
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
		} else if (value instanceof Enum) {
			String type = value.getClass().getCanonicalName();
			builder.append(type).append(".").append(((Enum<?>) value).name())
					.append(";");
		} else {
			return false;
		}

		return true;
	}

	/**
	 * Decode the data into an equivalent source object.
	 * 
	 * @param encodedValue
	 *            the encoded data, cannot be NULL
	 * 
	 * @return the object (can be NULL for NULL encoded values)
	 * 
	 * @throws IOException
	 *             if the content cannot be converted
	 */
	static Object decode(String encodedValue) throws IOException {
		try {
			String cut = "";
			if (encodedValue.length() > 1) {
				cut = encodedValue.substring(0, encodedValue.length() - 1);
			}

			if (CustomSerializer.isCustom(encodedValue)) {
				// custom:TYPE_NAME:"content is String-encoded"
				String type = CustomSerializer.typeOf(encodedValue);
				if (customTypes.containsKey(type)) {
					return customTypes.get(type).decode(encodedValue);
				}
				throw new IOException("Unknown custom type: " + type);
			} else if (encodedValue.equals("NULL")
					|| encodedValue.equals("null")) {
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
			} else if (encodedValue.endsWith(";")) {
				return decodeEnum(encodedValue);
			} else {
				return Integer.parseInt(encodedValue);
			}
		} catch (Exception e) {
			if (e instanceof IOException) {
				throw (IOException) e;
			}
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Return the corresponding class or throw an {@link Exception} if it
	 * cannot.
	 * 
	 * @param type
	 *            the class name to look for
	 * 
	 * @return the class (will never be NULL)
	 * 
	 * @throws ClassNotFoundException
	 *             if the class cannot be found
	 * @throws NoSuchMethodException
	 *             if the class cannot be created (usually because it or its
	 *             enclosing class doesn't have an empty constructor)
	 */
	static private Class<?> getClass(String type)
			throws ClassNotFoundException, NoSuchMethodException {
		Class<?> clazz = null;
		try {
			clazz = Class.forName(type);
		} catch (ClassNotFoundException e) {
			int pos = type.length();
			pos = type.lastIndexOf(".", pos);
			if (pos >= 0) {
				String parentType = type.substring(0, pos);
				String nestedType = type.substring(pos + 1);
				Class<?> javaParent = null;
				try {
					javaParent = getClass(parentType);
					parentType = javaParent.getName();
					clazz = Class.forName(parentType + "$" + nestedType);
				} catch (Exception ee) {
				}

				if (javaParent == null) {
					throw new NoSuchMethodException(
							"Class not found: "
									+ type
									+ " (the enclosing class cannot be created: maybe it doesn't have an empty constructor?)");
				}
			}
		}

		if (clazz == null) {
			throw new ClassNotFoundException("Class not found: " + type);
		}

		return clazz;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Enum<?> decodeEnum(String escaped) {
		// escaped: be.xxx.EnumType.VALUE;
		int pos = escaped.lastIndexOf(".");
		String type = escaped.substring(0, pos);
		String name = escaped.substring(pos + 1, escaped.length() - 1);

		try {
			return Enum.valueOf((Class<Enum>) getClass(type), name);
		} catch (Exception e) {
			throw new UnknownFormatConversionException("Unknown enum: <" + type
					+ "> " + name);
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

		return builder.substring(1, builder.length() - 1);
	}
}
