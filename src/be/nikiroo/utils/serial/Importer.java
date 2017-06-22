package be.nikiroo.utils.serial;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import be.nikiroo.utils.IOUtils;
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

	public Importer readLine(String line) {
		try {
			processLine(line);
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return this;
	}

	public Importer read(String data) {
		try {
			if (data.startsWith("ZIP:")) {
				data = StringUtils.unzip64(data.substring("ZIP:".length()));
			}
			Scanner scan = new Scanner(data);
			scan.useDelimiter("\n");
			while (scan.hasNext()) {
				processLine(scan.next());
			}
			scan.close();
		} catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
		return this;
	}

	public boolean processLine(String line) throws IllegalArgumentException,
			NoSuchFieldException, SecurityException, IllegalAccessException,
			NoSuchMethodException, InstantiationException, ClassNotFoundException, InvocationTargetException {
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
				me = createSelf(line.substring(4).split("@")[0]);
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

	/**
	 * Create an empty object of the given type.
	 * 
	 * @param type
	 *            the object type
	 * @return the object
	 * 
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 */
	private Object createSelf(String type) throws NoSuchMethodException,
			SecurityException, InstantiationException, IllegalAccessException,
			ClassNotFoundException, IllegalArgumentException,
			InvocationTargetException {

		try {
			Class<?> clazz = getClass(type);
			if (clazz == null) {
				throw new ClassNotFoundException("Class not found: " + type);
			}

			String className = clazz.getName();
			Object[] args = null;
			Constructor<?> ctor = null;
			if (className.contains("$")) {
				Object javaParent = createSelf(className.substring(0,
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
		} catch (NoSuchMethodException e) {
			throw new NoSuchMethodException(
					String.format(
							"Objects of type \"%s\" cannot be created by this code: maybe the class"
									+ " or its enclosing class doesn't have an empty constructor?",
							type));

		}
	}

	private Class<?> getClass(String type) throws ClassNotFoundException,
			NoSuchMethodException {
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

		return clazz;
	}

	private void setField(String name, Object value)
			throws NoSuchFieldException, SecurityException,
			IllegalArgumentException, IllegalAccessException {

		try {
			Field field = me.getClass().getDeclaredField(name);

			field.setAccessible(true);
			field.set(me, value);
		} catch (NoSuchFieldException e) {
			throw new NoSuchFieldException(String.format(
					"Field \"%s\" was not found in object of type \"%s\".",
					name, me.getClass().getCanonicalName()));
		}
	}

	public Object getValue() {
		return me;
	}
}