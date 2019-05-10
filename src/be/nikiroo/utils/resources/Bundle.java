package be.nikiroo.utils.resources;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * This class encapsulate a {@link ResourceBundle} in UTF-8. It allows to
 * retrieve values associated to an enumeration, and allows some additional
 * methods.
 * <p>
 * It also sports a writable change map, and you can save back the
 * {@link Bundle} to file with {@link Bundle#updateFile(String)}.
 * 
 * @param <E>
 *            the enum to use to get values out of this class
 * 
 * @author niki
 */

public class Bundle<E extends Enum<E>> {
	/** The type of E. */
	protected Class<E> type;
	/**
	 * The {@link Enum} associated to this {@link Bundle} (all the keys used in
	 * this {@link Bundle} will be of this type).
	 */
	protected Enum<?> keyType;

	private TransBundle<E> descriptionBundle;

	/** R/O map */
	private Map<String, String> map;
	/** R/W map */
	private Map<String, String> changeMap;

	/**
	 * Create a new {@link Bundles} of the given name.
	 * 
	 * @param type
	 *            a runtime instance of the class of E
	 * @param name
	 *            the name of the {@link Bundles}
	 * @param descriptionBundle
	 *            the description {@link TransBundle}, that is, a
	 *            {@link TransBundle} dedicated to the description of the values
	 *            of the given {@link Bundle} (can be NULL)
	 */
	protected Bundle(Class<E> type, Enum<?> name,
			TransBundle<E> descriptionBundle) {
		this.type = type;
		this.keyType = name;
		this.descriptionBundle = descriptionBundle;

		this.map = new HashMap<String, String>();
		this.changeMap = new HashMap<String, String>();
		setBundle(name, Locale.getDefault(), false);
	}

	/**
	 * Return the value associated to the given id as a {@link String}.
	 * 
	 * @param id
	 *            the id of the value to get
	 * 
	 * @return the associated value, or NULL if not found (not present in the
	 *         resource file)
	 */
	public String getString(E id) {
		return getString(id.name());
	}

	/**
	 * Set the value associated to the given id as a {@link String}.
	 * 
	 * @param id
	 *            the id of the value to get
	 * @param value
	 *            the value
	 * 
	 */
	public void setString(E id, String value) {
		setString(id.name(), value);
	}

	/**
	 * Return the value associated to the given id as a {@link String} suffixed
	 * with the runtime value "_suffix" (that is, "_" and suffix).
	 * <p>
	 * Will only accept suffixes that form an existing id.
	 * 
	 * @param id
	 *            the id of the value to get
	 * @param suffix
	 *            the runtime suffix
	 * 
	 * @return the associated value, or NULL if not found (not present in the
	 *         resource file)
	 */
	public String getStringX(E id, String suffix) {
		String key = id.name()
				+ (suffix == null ? "" : "_" + suffix.toUpperCase());

		try {
			id = Enum.valueOf(type, key);
			return getString(id);
		} catch (IllegalArgumentException e) {

		}

		return null;
	}

	/**
	 * Set the value associated to the given id as a {@link String} suffixed
	 * with the runtime value "_suffix" (that is, "_" and suffix).
	 * <p>
	 * Will only accept suffixes that form an existing id.
	 * 
	 * @param id
	 *            the id of the value to get
	 * @param suffix
	 *            the runtime suffix
	 * @param value
	 *            the value
	 */
	public void setStringX(E id, String suffix, String value) {
		String key = id.name()
				+ (suffix == null ? "" : "_" + suffix.toUpperCase());

		try {
			id = Enum.valueOf(type, key);
			setString(id, value);
		} catch (IllegalArgumentException e) {

		}
	}

	/**
	 * Return the value associated to the given id as a {@link Boolean}.
	 * 
	 * @param id
	 *            the id of the value to get
	 * 
	 * @return the associated value
	 */
	public Boolean getBoolean(E id) {
		String str = getString(id);
		if (str != null && str.length() > 0) {
			if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("on")
					|| str.equalsIgnoreCase("yes"))
				return true;
			if (str.equalsIgnoreCase("false") || str.equalsIgnoreCase("off")
					|| str.equalsIgnoreCase("no"))
				return false;

		}

		return null;
	}

	/**
	 * Return the value associated to the given id as a {@link Boolean}.
	 * 
	 * @param id
	 *            the id of the value to get
	 * @param def
	 *            the default value when it is not present in the config file or
	 *            if it is not a boolean value
	 * 
	 * @return the associated value
	 */
	public boolean getBoolean(E id, boolean def) {
		Boolean b = getBoolean(id);
		if (b != null)
			return b;

		return def;
	}

	/**
	 * Return the value associated to the given id as an {@link Integer}.
	 * 
	 * @param id
	 *            the id of the value to get
	 * 
	 * @return the associated value
	 */
	public Integer getInteger(E id) {
		try {
			return Integer.parseInt(getString(id));
		} catch (Exception e) {
		}

		return null;
	}

	/**
	 * Return the value associated to the given id as an int.
	 * 
	 * @param id
	 *            the id of the value to get
	 * @param def
	 *            the default value when it is not present in the config file or
	 *            if it is not a int value
	 * 
	 * @return the associated value
	 */
	public int getInteger(E id, int def) {
		Integer i = getInteger(id);
		if (i != null)
			return i;

		return def;
	}

	/**
	 * Return the value associated to the given id as a {@link Character}.
	 * 
	 * @param id
	 *            the id of the value to get
	 * 
	 * @return the associated value
	 */
	public Character getCharacter(E id) {
		String s = getString(id).trim();
		if (s.length() > 0) {
			return s.charAt(0);
		}

		return null;
	}

	/**
	 * Return the value associated to the given id as a {@link Character}.
	 * 
	 * @param id
	 *            the id of the value to get
	 * @param def
	 *            the default value when it is not present in the config file or
	 *            if it is not a char value
	 * 
	 * @return the associated value
	 */
	public char getCharacter(E id, char def) {
		String s = getString(id);
		if (s != null && s.length() > 0) {
			return s.trim().charAt(0);
		}

		return def;
	}

	/**
	 * Return the value associated to the given id as a colour if it is found
	 * and can be parsed.
	 * <p>
	 * The returned value is an ARGB value.
	 * 
	 * @param id
	 *            the id of the value to get
	 * 
	 * @return the associated value
	 */
	public Integer getColor(E id) {
		Integer rep = null;

		String bg = getString(id).trim();

		int r = 0, g = 0, b = 0, a = -1;
		if (bg.startsWith("#") && (bg.length() == 7 || bg.length() == 9)) {
			try {
				r = Integer.parseInt(bg.substring(1, 3), 16);
				g = Integer.parseInt(bg.substring(3, 5), 16);
				b = Integer.parseInt(bg.substring(5, 7), 16);
				if (bg.length() == 9) {
					a = Integer.parseInt(bg.substring(7, 9), 16);
				} else {
					a = 255;
				}

			} catch (NumberFormatException e) {
				// no changes
			}
		}

		// Try by name if still not found
		if (a == -1) {
			if ("black".equalsIgnoreCase(bg)) {
				a = 255;
				r = 0;
				g = 0;
				b = 0;
			} else if ("white".equalsIgnoreCase(bg)) {
				a = 255;
				r = 255;
				g = 255;
				b = 255;
			} else if ("red".equalsIgnoreCase(bg)) {
				a = 255;
				r = 255;
				g = 0;
				b = 0;
			} else if ("green".equalsIgnoreCase(bg)) {
				a = 255;
				r = 0;
				g = 255;
				b = 0;
			} else if ("blue".equalsIgnoreCase(bg)) {
				a = 255;
				r = 0;
				g = 0;
				b = 255;
			} else if ("grey".equalsIgnoreCase(bg)
					|| "gray".equalsIgnoreCase(bg)) {
				a = 255;
				r = 128;
				g = 128;
				b = 128;
			} else if ("cyan".equalsIgnoreCase(bg)) {
				a = 255;
				r = 0;
				g = 255;
				b = 255;
			} else if ("magenta".equalsIgnoreCase(bg)) {
				a = 255;
				r = 255;
				g = 0;
				b = 255;
			} else if ("yellow".equalsIgnoreCase(bg)) {
				a = 255;
				r = 255;
				g = 255;
				b = 0;
			}
		}

		if (a != -1) {
			rep = ((a & 0xFF) << 24) //
					| ((r & 0xFF) << 16) //
					| ((g & 0xFF) << 8) //
					| ((b & 0xFF) << 0);
		}

		return rep;
	}

	/**
	 * Set the value associated to the given id as a colour.
	 * <p>
	 * The value is an BGRA value.
	 * 
	 * @param id
	 *            the id of the value to set
	 * @param color
	 *            the new colour
	 */
	public void setColor(E id, Integer color) {
		int a = (color >> 24) & 0xFF;
		int r = (color >> 16) & 0xFF;
		int g = (color >> 8) & 0xFF;
		int b = (color >> 0) & 0xFF;

		String rs = Integer.toString(r, 16);
		String gs = Integer.toString(g, 16);
		String bs = Integer.toString(b, 16);
		String as = "";
		if (a < 255) {
			as = Integer.toString(a, 16);
		}

		setString(id, "#" + rs + gs + bs + as);
	}

	/**
	 * Return the value associated to the given id as a list of values if it is
	 * found and can be parsed.
	 * 
	 * @param id
	 *            the id of the value to get
	 * 
	 * @return the associated list, empty if the value is empty, NULL if it is
	 *         not found or cannot be parsed as a list
	 */
	public List<String> getList(E id) {
		List<String> list = new ArrayList<String>();
		String raw = getString(id);
		try {
			boolean inQuote = false;
			boolean prevIsBackSlash = false;
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < raw.length(); i++) {
				char car = raw.charAt(i);

				if (prevIsBackSlash) {
					builder.append(car);
					prevIsBackSlash = false;
				} else {
					switch (car) {
					case '"':
						if (inQuote) {
							list.add(builder.toString());
							builder.setLength(0);
						}

						inQuote = !inQuote;
						break;
					case '\\':
						prevIsBackSlash = true;
						break;
					case ' ':
					case '\n':
					case '\r':
						if (inQuote) {
							builder.append(car);
						}
						break;

					case ',':
						if (!inQuote) {
							break;
						}
						// continue to default
					default:
						if (!inQuote) {
							// Bad format!
							return null;
						}

						builder.append(car);
						break;
					}
				}
			}

			if (inQuote || prevIsBackSlash) {
				// Bad format!
				return null;
			}

		} catch (Exception e) {
			return null;
		}

		return list;
	}

	/**
	 * Set the value associated to the given id as a list of values.
	 * 
	 * @param id
	 *            the id of the value to set
	 * @param list
	 *            the new list of values
	 */
	public void setList(E id, List<String> list) {
		StringBuilder builder = new StringBuilder();
		for (String item : list) {
			if (builder.length() > 0) {
				builder.append(", ");
			}
			builder.append('"')//
					.append(item.replace("\\", "\\\\").replace("\"", "\\\""))//
					.append('"');
		}

		setString(id, builder.toString());
	}

	/**
	 * Create/update the .properties file.
	 * <p>
	 * Will use the most likely candidate as base if the file does not already
	 * exists and this resource is translatable (for instance, "en_US" will use
	 * "en" as a base if the resource is a translation file).
	 * <p>
	 * Will update the files in {@link Bundles#getDirectory()}; it <b>MUST</b>
	 * be set.
	 * 
	 * @throws IOException
	 *             in case of IO errors
	 */
	public void updateFile() throws IOException {
		updateFile(Bundles.getDirectory());
	}

	/**
	 * Create/update the .properties file.
	 * <p>
	 * Will use the most likely candidate as base if the file does not already
	 * exists and this resource is translatable (for instance, "en_US" will use
	 * "en" as a base if the resource is a translation file).
	 * 
	 * @param path
	 *            the path where the .properties files are, <b>MUST NOT</b> be
	 *            NULL
	 * 
	 * @throws IOException
	 *             in case of IO errors
	 */
	public void updateFile(String path) throws IOException {
		File file = getUpdateFile(path);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(file), "UTF-8"));

		writeHeader(writer);
		writer.write("\n");
		writer.write("\n");

		for (Field field : type.getDeclaredFields()) {
			Meta meta = field.getAnnotation(Meta.class);
			if (meta != null) {
				E id = Enum.valueOf(type, field.getName());
				String info = getMetaInfo(meta);

				if (info != null) {
					writer.write(info);
					writer.write("\n");
				}

				writeValue(writer, id);
			}
		}

		writer.close();
	}

	/**
	 * Delete the .properties file.
	 * <p>
	 * Will use the most likely candidate as base if the file does not already
	 * exists and this resource is translatable (for instance, "en_US" will use
	 * "en" as a base if the resource is a translation file).
	 * <p>
	 * Will delete the files in {@link Bundles#getDirectory()}; it <b>MUST</b>
	 * be set.
	 * 
	 * @return TRUE if the file was deleted
	 */
	public boolean deleteFile() {
		return deleteFile(Bundles.getDirectory());
	}

	/**
	 * Delete the .properties file.
	 * <p>
	 * Will use the most likely candidate as base if the file does not already
	 * exists and this resource is translatable (for instance, "en_US" will use
	 * "en" as a base if the resource is a translation file).
	 * 
	 * @param path
	 *            the path where the .properties files are, <b>MUST NOT</b> be
	 *            NULL
	 * 
	 * @return TRUE if the file was deleted
	 */
	public boolean deleteFile(String path) {
		File file = getUpdateFile(path);
		return file.delete();
	}

	/**
	 * The description {@link TransBundle}, that is, a {@link TransBundle}
	 * dedicated to the description of the values of the given {@link Bundle}
	 * (can be NULL).
	 * 
	 * @return the description {@link TransBundle}
	 */
	public TransBundle<E> getDescriptionBundle() {
		return descriptionBundle;
	}

	/**
	 * Reload the {@link Bundle} data files.
	 * 
	 * @param resetToDefault
	 *            reset to the default configuration (do not look into the
	 *            possible user configuration files, only take the original
	 *            configuration)
	 */
	public void reload(boolean resetToDefault) {
		setBundle(keyType, Locale.getDefault(), resetToDefault);
	}

	/**
	 * Check if the internal map contains the given key.
	 * 
	 * @param key
	 *            the key to check for
	 * 
	 * @return true if it does
	 */
	protected boolean containsKey(String key) {
		return changeMap.containsKey(key) || map.containsKey(key);
	}

	/**
	 * Get the value for the given key if it exists in the internal map, or NULL
	 * if not.
	 * 
	 * @param key
	 *            the key to check for
	 * 
	 * @return the value, or NULL
	 */
	protected String getString(String key) {
		if (changeMap.containsKey(key)) {
			return changeMap.get(key);
		}

		if (map.containsKey(key)) {
			return map.get(key);
		}

		return null;
	}

	/**
	 * Set the value for this key, in the change map (it is kept in memory, not
	 * yet on disk).
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the associated value
	 */
	protected void setString(String key, String value) {
		changeMap.put(key, value == null ? null : value.trim());
	}

	/**
	 * Return formated, display-able information from the {@link Meta} field
	 * given. Each line will always starts with a "#" character.
	 * 
	 * @param meta
	 *            the {@link Meta} field
	 * 
	 * @return the information to display or NULL if none
	 */
	protected String getMetaInfo(Meta meta) {
		String desc = meta.description();
		boolean group = meta.group();
		Meta.Format format = meta.format();
		String[] list = meta.list();
		boolean nullable = meta.nullable();
		String def = meta.def();
		String info = meta.info();
		boolean array = meta.array();

		// Default, empty values -> NULL
		if (desc.length() + list.length + info.length() + def.length() == 0
				&& !group && nullable && format == Meta.Format.STRING) {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		builder.append("# ").append(desc);
		if (desc.length() > 20) {
			builder.append("\n#");
		}

		if (group) {
			builder.append("This item is used as a group, its content is not expected to be used.");
		} else {
			builder.append(" (FORMAT: ").append(format)
					.append(nullable ? "" : " (required)");
			builder.append(") ").append(info);

			if (list.length > 0) {
				builder.append("\n# ALLOWED VALUES:");
				for (String value : list) {
					builder.append(" \"").append(value).append("\"");
				}
			}

			if (array) {
				builder.append("\n# (This item accept a list of comma-separated values)");
			}
		}

		return builder.toString();
	}

	/**
	 * The display name used in the <tt>.properties file</tt>.
	 * 
	 * @return the name
	 */
	protected String getBundleDisplayName() {
		return keyType.toString();
	}

	/**
	 * Write the header found in the configuration <tt>.properties</tt> file of
	 * this {@link Bundles}.
	 * 
	 * @param writer
	 *            the {@link Writer} to write the header in
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	protected void writeHeader(Writer writer) throws IOException {
		writer.write("# " + getBundleDisplayName() + "\n");
		writer.write("#\n");
	}

	/**
	 * Write the given id to the config file, i.e., "MY_ID = my_curent_value"
	 * followed by a new line
	 * 
	 * @param writer
	 *            the {@link Writer} to write into
	 * @param id
	 *            the id to write
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	protected void writeValue(Writer writer, E id) throws IOException {
		writeValue(writer, id.name(), getString(id));
	}

	/**
	 * Write the given data to the config file, i.e., "MY_ID = my_curent_value"
	 * followed by a new line
	 * 
	 * @param writer
	 *            the {@link Writer} to write into
	 * @param id
	 *            the id to write
	 * @param value
	 *            the id's value
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	protected void writeValue(Writer writer, String id, String value)
			throws IOException {
		writer.write(id);
		writer.write(" = ");

		if (value == null) {
			value = "";
		}

		String[] lines = value.replaceAll("\t", "\\\\\\t").split("\n");
		for (int i = 0; i < lines.length; i++) {
			writer.write(lines[i]);
			if (i < lines.length - 1) {
				writer.write("\\n\\");
			}
			writer.write("\n");
		}
	}

	/**
	 * Return the source file for this {@link Bundles} from the given path.
	 * 
	 * @param path
	 *            the path where the .properties files are
	 * 
	 * @return the source {@link File}
	 */
	protected File getUpdateFile(String path) {
		return new File(path, keyType.name() + ".properties");
	}

	/**
	 * Change the currently used bundle, and reset all changes.
	 * 
	 * @param name
	 *            the name of the bundle to load
	 * @param locale
	 *            the {@link Locale} to use
	 * @param resetToDefault
	 *            reset to the default configuration (do not look into the
	 *            possible user configuration files, only take the original
	 *            configuration)
	 */
	protected void setBundle(Enum<?> name, Locale locale, boolean resetToDefault) {
		changeMap.clear();
		String dir = Bundles.getDirectory();
		String bname = type.getPackage().getName() + "." + name.name();

		boolean found = false;
		if (!resetToDefault && dir != null) {
			// Look into Bundles.getDirectory() for .properties files
			try {
				File file = getPropertyFile(dir, name.name(), locale);
				if (file != null) {
					Reader reader = new InputStreamReader(new FileInputStream(
							file), "UTF8");
					resetMap(new PropertyResourceBundle(reader));
					found = true;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		if (!found) {
			// Look into the package itself for resources
			try {
				resetMap(ResourceBundle
						.getBundle(bname, locale, type.getClassLoader(),
								new FixedResourceBundleControl()));
				found = true;
			} catch (MissingResourceException e) {
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (!found) {
			// We have no bundle for this Bundle
			System.err.println("No bundle found for: " + bname);
			resetMap(null);
		}
	}

	/**
	 * Reset the backing map to the content of the given bundle, or with default
	 * valiues if bundle is NULL.
	 * 
	 * @param bundle
	 *            the bundle to copy
	 */
	protected void resetMap(ResourceBundle bundle) {
		this.map.clear();
		for (Field field : type.getDeclaredFields()) {
			try {
				Meta meta = field.getAnnotation(Meta.class);
				if (meta != null) {
					E id = Enum.valueOf(type, field.getName());

					String value;
					if (bundle != null) {
						value = bundle.getString(id.name());
					} else {
						value = meta.def();
					}

					this.map.put(id.name(), value == null ? null : value.trim());
				}
			} catch (MissingResourceException e) {
			}
		}
	}

	/**
	 * Take a snapshot of the changes in memory in this {@link Bundle} made by
	 * the "set" methods ( {@link Bundle#setString(Enum, String)}...) at the
	 * current time.
	 * 
	 * @return a snapshot to use with {@link Bundle#restoreSnapshot(Object)}
	 */
	public Object takeSnapshot() {
		return new HashMap<String, String>(changeMap);
	}

	/**
	 * Restore a snapshot taken with {@link Bundle}, or reset the current
	 * changes if the snapshot is NULL.
	 * 
	 * @param snap
	 *            the snapshot or NULL
	 */
	@SuppressWarnings("unchecked")
	public void restoreSnapshot(Object snap) {
		if (snap == null) {
			changeMap.clear();
		} else {
			if (snap instanceof Map) {
				changeMap = (Map<String, String>) snap;
			} else {
				throw new RuntimeException(
						"Restoring changes in a Bundle must be done on a changes snapshot, "
								+ "or NULL to discard current changes");
			}
		}
	}

	/**
	 * Return the resource file that is closer to the {@link Locale}.
	 * 
	 * @param dir
	 *            the directory to look into
	 * @param name
	 *            the file base name (without <tt>.properties</tt>)
	 * @param locale
	 *            the {@link Locale}
	 * 
	 * @return the closest match or NULL if none
	 */
	private File getPropertyFile(String dir, String name, Locale locale) {
		List<String> locales = new ArrayList<String>();
		if (locale != null) {
			String country = locale.getCountry() == null ? "" : locale
					.getCountry();
			String language = locale.getLanguage() == null ? "" : locale
					.getLanguage();
			if (!language.isEmpty() && !country.isEmpty()) {
				locales.add("_" + language + "-" + country);
			}
			if (!language.isEmpty()) {
				locales.add("_" + language);
			}
		}

		locales.add("");

		File file = null;
		for (String loc : locales) {
			file = new File(dir, name + loc + ".properties");
			if (file.exists()) {
				break;
			}

			file = null;
		}

		return file;
	}
}
