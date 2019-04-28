package be.nikiroo.utils.serial;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.streams.NextableInputStream;
import be.nikiroo.utils.streams.NextableInputStreamStep;

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
	 * @param in
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
	 * @throws NullPointerException
	 *             if the stream is empty
	 */
	public Importer read(InputStream in) throws NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException, IOException,
			NullPointerException {

		NextableInputStream stream = new NextableInputStream(in,
				new NextableInputStreamStep('\n'));

		try {
			if (in == null) {
				throw new NullPointerException("InputStream is null");
			}

			boolean first = true;
			while (stream.next()) {
				if (stream.eof()) {
					if (first) {
						throw new NullPointerException(
								"InputStream empty, normal termination");
					}
					return this;
				}
				first = false;

				boolean zip = stream.startsWiths("ZIP:");
				boolean b64 = stream.startsWiths("B64:");

				if (zip || b64) {
					stream.skip("XXX:".length());
					InputStream decoded = StringUtils.unbase64(stream.open(),
							zip);
					try {
						read(decoded);
					} finally {
						decoded.close();
					}
				} else {
					processLine(stream);
				}
			}
		} finally {
			stream.close(false);
		}

		return this;
	}

	/**
	 * Read a single (whole) line of serialised data into this {@link Importer}
	 * and accumulate it with the already present data.
	 * 
	 * @param in
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
	private boolean processLine(InputStream in) throws NoSuchFieldException,
			NoSuchMethodException, ClassNotFoundException, IOException {

		// Defer to latest child if any
		if (child != null) {
			if (child.processLine(in)) {
				if (currentFieldName != null) {
					setField(currentFieldName, child.getValue());
					currentFieldName = null;
				}
				child = null;
			}

			return false;
		}

		// TODO use the stream, Luke
		String line = IOUtils.readSmallStream(in);

		if (line.isEmpty()) {
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
	 * Return the current deserialised value.
	 * 
	 * @return the current value
	 */
	public Object getValue() {
		return me;
	}
}