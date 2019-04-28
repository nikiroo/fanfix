package be.nikiroo.utils.serial;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.NotSerializableException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UnknownFormatConversionException;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.streams.NextableInputStream;
import be.nikiroo.utils.streams.NextableInputStreamStep;

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
			protected void toStream(OutputStream out, Object value)
					throws IOException {

				// TODO: we use \n to separate, and b64 to un-\n
				// -- but we could use \\n ?
				String type = value.getClass().getCanonicalName();
				type = type.substring(0, type.length() - 2); // remove the []

				write(out, type);
				try {
					for (int i = 0; true; i++) {
						Object item = Array.get(value, i);

						// encode it normally if direct value
						write(out, "\r");
						if (!SerialUtils.encode(out, item)) {
							try {
								// TODO: bad escaping?
								write(out, "B64:");
								OutputStream bout = StringUtils.base64(out,
										false, false);
								new Exporter(bout).append(item);
							} catch (NotSerializableException e) {
								throw new UnknownFormatConversionException(e
										.getMessage());
							}
						}
					}
				} catch (ArrayIndexOutOfBoundsException e) {
					// Done.
				}
			}

			@Override
			protected Object fromStream(InputStream in) throws IOException {
				NextableInputStream stream = new NextableInputStream(in,
						new NextableInputStreamStep('\r'));

				try {
					List<Object> list = new ArrayList<Object>();
					stream.next();
					String type = IOUtils.readSmallStream(stream);

					while (stream.next()) {
						Object value = new Importer().read(stream).getValue();
						list.add(value);
					}

					Object array = Array.newInstance(
							SerialUtils.getClass(type), list.size());
					for (int i = 0; i < list.size(); i++) {
						Array.set(array, i, list.get(i));
					}

					return array;
				} catch (Exception e) {
					if (e instanceof IOException) {
						throw (IOException) e;
					}
					throw new IOException(e.getMessage());
				}
			}

			@Override
			protected String getType() {
				return "[]";
			}
		});

		// URL:
		customTypes.put("java.net.URL", new CustomSerializer() {
			@Override
			protected void toStream(OutputStream out, Object value)
					throws IOException {
				String val = "";
				if (value != null) {
					val = ((URL) value).toString();
				}

				out.write(val.getBytes("UTF-8"));
			}

			@Override
			protected Object fromStream(InputStream in) throws IOException {
				String val = IOUtils.readSmallStream(in);
				if (!val.isEmpty()) {
					return new URL(val);
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
			protected void toStream(OutputStream out, Object value)
					throws IOException {
				Image img = (Image) value;
				OutputStream encoded = StringUtils.base64(out, false, false);
				try {
					InputStream in = img.newInputStream();
					try {
						IOUtils.write(in, encoded);
					} finally {
						in.close();
					}
				} finally {
					encoded.flush();
					// Cannot close!
				}
			}

			@Override
			protected String getType() {
				return "be.nikiroo.utils.Image";
			}

			@Override
			protected Object fromStream(InputStream in) throws IOException {
				try {
					// Cannot close it!
					InputStream decoded = StringUtils.unbase64(in, false);
					return new Image(decoded);
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

		String desc = null;
		try {
			Class<?> clazz = getClass(type);
			String className = clazz.getName();
			List<Object> args = new ArrayList<Object>();
			List<Class<?>> classes = new ArrayList<Class<?>>();
			Constructor<?> ctor = null;
			if (className.contains("$")) {
				for (String parentName = className.substring(0,
						className.lastIndexOf('$'));; parentName = parentName
						.substring(0, parentName.lastIndexOf('$'))) {
					Object parent = createObject(parentName);
					args.add(parent);
					classes.add(parent.getClass());

					if (!parentName.contains("$")) {
						break;
					}
				}

				// Better error description in case there is no empty
				// constructor:
				desc = "";
				String end = "";
				for (Class<?> parent = clazz; parent != null
						&& !parent.equals(Object.class); parent = parent
						.getSuperclass()) {
					if (!desc.isEmpty()) {
						desc += " [:";
						end += "]";
					}
					desc += parent;
				}
				desc += end;
				//

				try {
					ctor = clazz.getDeclaredConstructor(classes
							.toArray(new Class[] {}));
				} catch (NoSuchMethodException nsme) {
					// TODO: it seems we do not always need a parameter for each
					// level, so we currently try "ALL" levels or "FIRST" level
					// only -> we should check the actual rule and use it
					ctor = clazz.getDeclaredConstructor(classes.get(0));
					Object firstParent = args.get(0);
					args.clear();
					args.add(firstParent);
				}
				desc = null;
			} else {
				ctor = clazz.getDeclaredConstructor();
			}

			ctor.setAccessible(true);
			return ctor.newInstance(args.toArray());
		} catch (ClassNotFoundException e) {
			throw e;
		} catch (NoSuchMethodException e) {
			if (desc != null) {
				throw new NoSuchMethodException("Empty constructor not found: "
						+ desc);
			}
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
	 * Serialise the given object into this {@link OutputStream}.
	 * <p>
	 * <b>Important: </b>If the operation fails (with a
	 * {@link NotSerializableException}), the {@link StringBuilder} will be
	 * corrupted (will contain bad, most probably not importable data).
	 * 
	 * @param out
	 *            the output {@link OutputStream} to serialise to
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
	 * @throws IOException
	 *             in case of I/O errors
	 */
	static void append(OutputStream out, Object o, Map<Integer, Object> map)
			throws NotSerializableException, IOException {

		Field[] fields = new Field[] {};
		String type = "";
		String id = "NULL";

		if (o != null) {
			int hash = System.identityHashCode(o);
			fields = o.getClass().getDeclaredFields();
			type = o.getClass().getCanonicalName();
			if (type == null) {
				// Anonymous inner classes support
				type = o.getClass().getName();
			}
			id = Integer.toString(hash);
			if (map.containsKey(hash)) {
				fields = new Field[] {};
			} else {
				map.put(hash, o);
			}
		}

		write(out, "{\nREF ");
		write(out, type);
		write(out, "@");
		write(out, id);
		write(out, ":");

		if (!encode(out, o)) { // check if direct value
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

					write(out, "\n");
					write(out, field.getName());
					write(out, ":");

					Object value = field.get(o);

					if (!encode(out, value)) {
						write(out, "\n");
						append(out, value, map);
					}
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace(); // should not happen (see
										// setAccessible)
			} catch (IllegalAccessException e) {
				e.printStackTrace(); // should not happen (see
										// setAccessible)
			}

			write(out, "\n}");
		}
	}

	/**
	 * Encode the object into the given {@link OutputStream} if possible and if
	 * supported.
	 * <p>
	 * A supported object in this context means an object we can directly
	 * encode, like an Integer or a String. Custom objects and arrays are also
	 * considered supported, but <b>compound objects are not supported here</b>.
	 * <p>
	 * For compound objects, you should use {@link Exporter}.
	 * 
	 * @param out
	 *            the {@link OutputStream} to append to
	 * @param value
	 *            the object to encode (can be NULL, which will be encoded)
	 * 
	 * @return TRUE if success, FALSE if not (the content of the
	 *         {@link OutputStream} won't be changed in case of failure)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	static boolean encode(OutputStream out, Object value) throws IOException {
		if (value == null) {
			write(out, "NULL");
		} else if (value.getClass().getSimpleName().endsWith("[]")) {
			// Simple name does support [] suffix and do not return NULL for
			// inner anonymous classes
			customTypes.get("[]").encode(out, value);
		} else if (customTypes.containsKey(value.getClass().getCanonicalName())) {
			customTypes.get(value.getClass().getCanonicalName())//
					.encode(out, value);
		} else if (value instanceof String) {
			encodeString(out, (String) value);
		} else if (value instanceof Boolean) {
			write(out, value);
		} else if (value instanceof Byte) {
			write(out, value);
			write(out, "b");
		} else if (value instanceof Character) {
			encodeString(out, "" + value);
			write(out, "c");
		} else if (value instanceof Short) {
			write(out, value);
			write(out, "s");
		} else if (value instanceof Integer) {
			write(out, value);
		} else if (value instanceof Long) {
			write(out, value);
			write(out, "L");
		} else if (value instanceof Float) {
			write(out, value);
			write(out, "F");
		} else if (value instanceof Double) {
			write(out, value);
			write(out, "d");
		} else if (value instanceof Enum) {
			String type = value.getClass().getCanonicalName();
			write(out, type);
			write(out, ".");
			write(out, ((Enum<?>) value).name());
			write(out, ";");
		} else {
			return false;
		}

		return true;
	}

	/**
	 * Decode the data into an equivalent supported source object.
	 * <p>
	 * A supported object in this context means an object we can directly
	 * encode, like an Integer or a String. Custom objects and arrays are also
	 * considered supported, but <b>compound objects are not supported here</b>.
	 * <p>
	 * For compound objects, you should use {@link Importer}.
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
					// TODO: we should start with a stream
					InputStream streamEncodedValue = new ByteArrayInputStream(
							encodedValue.getBytes("UTF-8"));
					try {
						return customTypes.get(type).decode(streamEncodedValue);
					} finally {
						streamEncodedValue.close();
					}
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
			throw new IOException(e.getMessage(), e);
		}
	}

	/**
	 * Write the given {@link String} into the given {@link OutputStream} in
	 * UTF-8.
	 * 
	 * @param out
	 *            the {@link OutputStream}
	 * @param data
	 *            the data to write, cannot be NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	static void write(OutputStream out, Object data) throws IOException {
		try {
			out.write(data.toString().getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// A conforming JVM is required to support UTF-8
			e.printStackTrace();
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
	static private Enum<?> decodeEnum(String escaped) {
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
	static void encodeString(OutputStream out, String raw) throws IOException {
		// TODO: not. efficient.
		out.write('\"');
		// TODO !! utf-8 required
		for (char car : raw.toCharArray()) {
			encodeString(out, car);
		}
		out.write('\"');
	}

	// aa bb -> "aa\tbb"
	static void encodeString(OutputStream out, InputStream raw)
			throws IOException {
		out.write('\"');
		byte buffer[] = new byte[4096];
		for (int len = 0; (len = raw.read(buffer)) > 0;) {
			for (int i = 0; i < len; i++) {
				// TODO: not 100% correct, look up howto for UTF-8
				encodeString(out, (char) buffer[i]);
			}
		}
		out.write('\"');
	}

	// for encode string, NOT to encode a char by itself!
	static void encodeString(OutputStream out, char raw) throws IOException {
		switch (raw) {
		case '\\':
			out.write('\\');
			out.write('\\');
			break;
		case '\r':
			out.write('\\');
			out.write('r');
			break;
		case '\n':
			out.write('\\');
			out.write('n');
			break;
		case '"':
			out.write('\\');
			out.write('\"');
			break;
		default:
			out.write(raw);
			break;
		}
	}

	// "aa\tbb" -> aa bb
	static String decodeString(String escaped) {
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
