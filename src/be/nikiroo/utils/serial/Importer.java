package be.nikiroo.utils.serial;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import be.nikiroo.utils.StringUtils;

/**
 * A simple class that can accept the output of {@link Exporter} to recreate
 * objects as they were sent to said exporter.
 * <p>
 * This class requires the objects (and their potential enclosing objects) to
 * have an empty constructor, and does not support inner classes (it does
 * support nested classes, though).
 * 
 * @author niki
 */
public class Importer {
	static private Integer SIZE_ID = null;
	static private byte[] NEWLINE = null;

	private Boolean link;
	private Object me;
	private Importer child;
	private Map<String, Object> map;

	private String currentFieldName;

	static {
		try {
			SIZE_ID = "EXT:".getBytes("UTF-8").length;
			NEWLINE = "\n".getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// UTF-8 is mandated to exist on confirming jre's
		}
	}

	/**
	 * Create a new {@link Importer}.
	 */
	public Importer() {
		map = new HashMap<String, Object>();
		map.put("NULL", null);
	}

	private Importer(Map<String, Object> map) {
		this.map = map;
	}

	/**
	 * Read some data into this {@link Importer}: it can be the full serialised
	 * content, or a number of lines of it (any given line <b>MUST</b> be
	 * complete though) and accumulate it with the already present data.
	 * 
	 * @param data
	 *            the data to parse
	 * 
	 * @return itself so it can be chained
	 * 
	 * @throws NoSuchFieldException
	 *             if the serialised data contains information about a field
	 *             which does actually not exist in the class we know of
	 * @throws NoSuchMethodException
	 *             if a class described in the serialised data cannot be created
	 *             because it is not compatible with this code
	 * @throws ClassNotFoundException
	 *             if a class described in the serialised data cannot be found
	 * @throws IOException
	 *             if the content cannot be read (for instance, corrupt data)
	 */
	public Importer read(String data) throws NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException, IOException {
		return read(data.getBytes("UTF-8"), 0);
	}

	/**
	 * Read some data into this {@link Importer}: it can be the full serialised
	 * content, or a number of lines of it (any given line <b>MUST</b> be
	 * complete though) and accumulate it with the already present data.
	 * 
	 * @param data
	 *            the data to parse
	 * @param offset
	 *            the offset at which to start reading the data (we ignore
	 *            anything that goes before that offset)
	 * 
	 * @return itself so it can be chained
	 * 
	 * @throws NoSuchFieldException
	 *             if the serialised data contains information about a field
	 *             which does actually not exist in the class we know of
	 * @throws NoSuchMethodException
	 *             if a class described in the serialised data cannot be created
	 *             because it is not compatible with this code
	 * @throws ClassNotFoundException
	 *             if a class described in the serialised data cannot be found
	 * @throws IOException
	 *             if the content cannot be read (for instance, corrupt data)
	 */
	private Importer read(byte[] data, int offset) throws NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException, IOException {

		int dataStart = offset;
		while (dataStart < data.length) {
			String id = "";
			if (data.length - dataStart >= SIZE_ID) {
				id = new String(data, dataStart, SIZE_ID);
			}

			boolean zip = id.equals("ZIP:");
			boolean b64 = id.equals("B64:");
			if (zip || b64) {
				dataStart += SIZE_ID;
			}

			int count = find(data, dataStart, NEWLINE);
			count -= dataStart;
			if (count < 0) {
				count = data.length - dataStart;
			}

			if (zip || b64) {
				boolean unpacked = false;
				try {
					byte[] line = StringUtils.unbase64(data, dataStart, count,
							zip);
					unpacked = true;
					read(line, 0);
				} catch (IOException e) {
					throw new IOException("Internal error when decoding "
							+ (unpacked ? "unpacked " : "")
							+ (zip ? "ZIP" : "B64")
							+ " content: input may be corrupt");
				}
			} else {
				String line = new String(data, dataStart, count, "UTF-8");
				processLine(line);
			}

			dataStart += count + NEWLINE.length;
		}

		return this;
	}

	/**
	 * Read a single (whole) line of serialised data into this {@link Importer}
	 * and accumulate it with the already present data.
	 * 
	 * @param line
	 *            the line to parse
	 * 
	 * @return TRUE if we are just done with one object or sub-object
	 * 
	 * @throws NoSuchFieldException
	 *             if the serialised data contains information about a field
	 *             which does actually not exist in the class we know of
	 * @throws NoSuchMethodException
	 *             if a class described in the serialised data cannot be created
	 *             because it is not compatible with this code
	 * @throws ClassNotFoundException
	 *             if a class described in the serialised data cannot be found
	 * @throws IOException
	 *             if the content cannot be read (for instance, corrupt data)
	 */
	private boolean processLine(String line) throws NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException, IOException {
		// Defer to latest child if any
		if (child != null) {
			if (child.processLine(line)) {
				if (currentFieldName != null) {
					setField(currentFieldName, child.getValue());
					currentFieldName = null;
				}
				child = null;
			}

			return false;
		}

		if (line.equals("{")) { // START: new child if needed
			if (link != null) {
				child = new Importer(map);
			}
		} else if (line.equals("}")) { // STOP: report self to parent
			return true;
		} else if (line.startsWith("REF ")) { // REF: create/link self
			String[] tab = line.substring("REF ".length()).split("@");
			String type = tab[0];
			tab = tab[1].split(":");
			String ref = tab[0];

			link = map.containsKey(ref);
			if (link) {
				me = map.get(ref);
			} else {
				if (line.endsWith(":")) {
					// construct
					me = SerialUtils.createObject(type);
				} else {
					// direct value
					int pos = line.indexOf(":");
					String encodedValue = line.substring(pos + 1);
					me = SerialUtils.decode(encodedValue);
				}
				map.put(ref, me);
			}
		} else { // FIELD: new field *or* direct simple value
			if (line.endsWith(":")) {
				// field value is compound
				currentFieldName = line.substring(0, line.length() - 1);
			} else if (line.startsWith(":") || !line.contains(":")
					|| line.startsWith("\"") || CustomSerializer.isCustom(line)) {
				// not a field value but a direct value
				me = SerialUtils.decode(line);
			} else {
				// field value is direct
				int pos = line.indexOf(":");
				String fieldName = line.substring(0, pos);
				String encodedValue = line.substring(pos + 1);
				Object value = null;
				value = SerialUtils.decode(encodedValue);

				// To support simple types directly:
				if (me == null) {
					me = value;
				} else {
					setField(fieldName, value);
				}
			}
		}

		return false;
	}

	private void setField(String name, Object value)
			throws NoSuchFieldException {

		try {
			Field field = me.getClass().getDeclaredField(name);

			field.setAccessible(true);
			field.set(me, value);
		} catch (NoSuchFieldException e) {
			throw new NoSuchFieldException(String.format(
					"Field \"%s\" was not found in object of type \"%s\".",
					name, me.getClass().getCanonicalName()));
		} catch (Exception e) {
			throw new NoSuchFieldException(String.format(
					"Internal error when setting \"%s.%s\": %s", me.getClass()
							.getCanonicalName(), name, e.getMessage()));
		}
	}

	/**
	 * Find the given needle in the data and return its position (or -1 if not
	 * found).
	 * 
	 * @param data
	 *            the data to look through
	 * @param offset
	 *            the offset at wich to start searching
	 * @param needle
	 *            the needle to find
	 * 
	 * @return the position of the needle if found, -1 if not found
	 */
	private int find(byte[] data, int offset, byte[] needle) {
		for (int i = offset; i + needle.length - 1 < data.length; i++) {
			boolean same = true;
			for (int j = 0; j < needle.length; j++) {
				if (data[i + j] != needle[j]) {
					same = false;
					break;
				}
			}

			if (same) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Return the current deserialised value.
	 * 
	 * @return the current value
	 */
	public Object getValue() {
		return me;
	}
}