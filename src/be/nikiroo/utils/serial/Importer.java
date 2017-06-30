package be.nikiroo.utils.serial;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

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
	private Boolean link;
	private Object me;
	private Importer child;
	private Map<String, Object> map;

	private String currentFieldName;

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
	 */
	public Importer read(String data) throws NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException {

		try {
			Scanner scan = new Scanner(data);
			scan.useDelimiter("\n");
			while (scan.hasNext()) {
				String line = scan.next();

				if (line.startsWith("ZIP:")) {
					line = StringUtils.unzip64(line.substring("ZIP:".length()));
				}
				processLine(line);

			}
			scan.close();
		} catch (IOException e) {
			throw new NoSuchMethodException(
					"Internal error when decoding ZIP content: input may be corrupt");
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
	 */
	private boolean processLine(String line) throws NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException {
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
			String ref = line.substring(4).split("@")[1];
			link = map.containsKey(ref);
			if (link) {
				me = map.get(ref);
			} else {
				me = SerialUtils.createObject(line.substring(4).split("@")[0]);
				map.put(ref, me);
			}
		} else { // FIELD: new field
			if (line.endsWith(":")) {
				// field value is compound
				currentFieldName = line.substring(0, line.length() - 1);
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
	 * Return the current deserialised value.
	 * 
	 * @return the current value
	 */
	public Object getValue() {
		return me;
	}
}