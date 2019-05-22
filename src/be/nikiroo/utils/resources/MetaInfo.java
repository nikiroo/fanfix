package be.nikiroo.utils.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import be.nikiroo.utils.resources.Meta.Format;

/**
 * A graphical item that reflect a configuration option from the given
 * {@link Bundle}.
 * 
 * @author niki
 * 
 * @param <E>
 *            the type of {@link Bundle} to edit
 */
public class MetaInfo<E extends Enum<E>> implements Iterable<MetaInfo<E>> {
	private final Bundle<E> bundle;
	private final E id;

	private Meta meta;
	private List<MetaInfo<E>> children = new ArrayList<MetaInfo<E>>();

	private String value;
	private List<Runnable> reloadedListeners = new ArrayList<Runnable>();
	private List<Runnable> saveListeners = new ArrayList<Runnable>();

	private String name;
	private String description;

	/**
	 * Create a new {@link MetaInfo} from a value (without children).
	 * <p>
	 * For instance, you can call
	 * <tt>new MetaInfo(Config.class, configBundle, Config.MY_VALUE)</tt>.
	 * 
	 * @param type
	 *            the type of enum the value is
	 * @param bundle
	 *            the bundle this value belongs to
	 * @param id
	 *            the value itself
	 */
	public MetaInfo(Class<E> type, Bundle<E> bundle, E id) {
		this.bundle = bundle;
		this.id = id;

		try {
			this.meta = type.getDeclaredField(id.name()).getAnnotation(
					Meta.class);
		} catch (NoSuchFieldException e) {
		} catch (SecurityException e) {
		}

		// We consider that if a description bundle is used, everything is in it

		String description = null;
		if (bundle.getDescriptionBundle() != null) {
			description = bundle.getDescriptionBundle().getString(id);
			if (description != null && description.trim().isEmpty()) {
				description = null;
			}
		}
		if (description == null) {
			description = meta.description();
			if (description == null) {
				description = "";
			}
		}

		String name = idToName(id, null);

		// Special rules for groups:
		if (meta.group()) {
			String groupName = description.split("\n")[0];
			description = description.substring(groupName.length()).trim();
			if (!groupName.isEmpty()) {
				name = groupName;
			}
		}

		if (meta.def() != null && !meta.def().isEmpty()) {
			if (!description.isEmpty()) {
				description += "\n\n";
			}
			description += "(Default value: " + meta.def() + ")";
		}

		this.name = name;
		this.description = description;

		reload();
	}

	/**
	 * For normal items, this is the name of this item, deduced from its ID (or
	 * in other words, it is the ID but presented in a displayable form).
	 * <p>
	 * For group items, this is the first line of the description if it is not
	 * empty (else, it is the ID in the same way as normal items).
	 * <p>
	 * Never NULL.
	 * 
	 * 
	 * @return the name, never NULL
	 */
	public String getName() {
		return name;
	}

	/**
	 * A description for this item: what it is or does, how to explain that item
	 * to the user including what can be used here (i.e., %s = file name, %d =
	 * file size...).
	 * <p>
	 * For group, the first line ('\\n'-separated) will be used as a title while
	 * the rest will be the description.
	 * <p>
	 * If a default value is known, it will be specified here, too.
	 * <p>
	 * Never NULL.
	 * 
	 * @return the description, not NULL
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * The format this item is supposed to follow
	 * 
	 * @return the format
	 */
	public Format getFormat() {
		return meta.format();
	}

	/**
	 * The allowed list of values that a {@link Format#FIXED_LIST} item is
	 * allowed to be, or a list of suggestions for {@link Format#COMBO_LIST}
	 * items.
	 * <p>
	 * Will always allow an empty string in addition to the rest.
	 * 
	 * @return the list of values
	 */
	public String[] getAllowedValues() {
		String[] list = meta.list();

		String[] withEmpty = new String[list.length + 1];
		withEmpty[0] = "";
		for (int i = 0; i < list.length; i++) {
			withEmpty[i + 1] = list[i];
		}

		return withEmpty;
	}

	/**
	 * This item is a comma-separated list of values instead of a single value.
	 * <p>
	 * The list items are separated by a comma, each surrounded by
	 * double-quotes, with backslashes and double-quotes escaped by a backslash.
	 * <p>
	 * Example: <tt>"un", "deux"</tt>
	 * 
	 * @return TRUE if it is
	 */
	public boolean isArray() {
		return meta.array();
	}

	/**
	 * This item is only used as a group, not as an option.
	 * <p>
	 * For instance, you could have LANGUAGE_CODE as a group for which you won't
	 * use the value in the program, and LANGUAGE_CODE_FR, LANGUAGE_CODE_EN
	 * inside for which the value must be set.
	 * 
	 * @return TRUE if it is a group
	 */
	public boolean isGroup() {
		return meta.group();
	}

	/**
	 * The value stored by this item, as a {@link String}.
	 * 
	 * @param useDefaultIfEmpty
	 *            use the default value instead of NULL if the setting is not
	 *            set
	 * 
	 * @return the value
	 */
	public String getString(boolean useDefaultIfEmpty) {
		if (value == null && useDefaultIfEmpty) {
			return getDefaultString();
		}

		return value;
	}

	/**
	 * The default value of this item, as a {@link String}.
	 * 
	 * @return the default value
	 */
	public String getDefaultString() {
		return meta.def();
	}

	/**
	 * The value stored by this item, as a {@link Boolean}.
	 * 
	 * @param useDefaultIfEmpty
	 *            use the default value instead of NULL if the setting is not
	 *            set
	 * 
	 * @return the value
	 */
	public Boolean getBoolean(boolean useDefaultIfEmpty) {
		return BundleHelper.parseBoolean(getString(useDefaultIfEmpty));
	}

	/**
	 * The default value of this item, as a {@link Boolean}.
	 * 
	 * @return the default value
	 */
	public Boolean getDefaultBoolean() {
		return BundleHelper.parseBoolean(getDefaultString());
	}

	/**
	 * The value stored by this item, as a {@link Character}.
	 * 
	 * @param useDefaultIfEmpty
	 *            use the default value instead of NULL if the setting is not
	 *            set
	 * 
	 * @return the value
	 */
	public Character getCharacter(boolean useDefaultIfEmpty) {
		return BundleHelper.parseCharacter(getString(useDefaultIfEmpty));
	}

	/**
	 * The default value of this item, as a {@link Character}.
	 * 
	 * @return the default value
	 */
	public Character getDefaultCharacter() {
		return BundleHelper.parseCharacter(getDefaultString());
	}

	/**
	 * The value stored by this item, as an {@link Integer}.
	 * 
	 * @param useDefaultIfEmpty
	 *            use the default value instead of NULL if the setting is not
	 *            set
	 * 
	 * @return the value
	 */
	public Integer getInteger(boolean useDefaultIfEmpty) {
		return BundleHelper.parseInteger(getString(useDefaultIfEmpty));
	}

	/**
	 * The default value of this item, as an {@link Integer}.
	 * 
	 * @return the default value
	 */
	public Integer getDefaultInteger() {
		return BundleHelper.parseInteger(getDefaultString());
	}

	/**
	 * The value stored by this item, as a colour (represented here as an
	 * {@link Integer}) if it represents a colour, or NULL if it doesn't.
	 * <p>
	 * The returned colour value is an ARGB value.
	 * 
	 * @param useDefaultIfEmpty
	 *            use the default value instead of NULL if the setting is not
	 *            set
	 * 
	 * @return the value
	 */
	public Integer getColor(boolean useDefaultIfEmpty) {
		return BundleHelper.parseColor(getString(useDefaultIfEmpty));
	}

	/**
	 * The default value stored by this item, as a colour (represented here as
	 * an {@link Integer}) if it represents a colour, or NULL if it doesn't.
	 * <p>
	 * The returned colour value is an ARGB value.
	 * 
	 * @return the value
	 */
	public Integer getDefaultColor() {
		return BundleHelper.parseColor(getDefaultString());
	}

	/**
	 * A {@link String} representation of the list of values.
	 * <p>
	 * The list of values is comma-separated and each value is surrounded by
	 * double-quotes; backslashes and double-quotes are escaped by a backslash.
	 * 
	 * @param useDefaultIfEmpty
	 *            use the default value instead of NULL if the setting is not
	 *            set
	 * 
	 * @return the value
	 */
	public List<String> getList(boolean useDefaultIfEmpty) {
		return BundleHelper.parseList(getString(useDefaultIfEmpty));
	}

	/**
	 * A {@link String} representation of the default list of values.
	 * <p>
	 * The list of values is comma-separated and each value is surrounded by
	 * double-quotes; backslashes and double-quotes are escaped by a backslash.
	 * 
	 * @return the value
	 */
	public List<String> getDefaultList() {
		return BundleHelper.parseList(getDefaultString());
	}

	/**
	 * The value stored by this item, as a {@link String}.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setString(String value) {
		this.value = value;
	}

	/**
	 * The value stored by this item, as a {@link Boolean}.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setBoolean(boolean value) {
		setString(BundleHelper.fromBoolean(value));
	}

	/**
	 * The value stored by this item, as a {@link Character}.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setCharacter(char value) {
		setString(BundleHelper.fromCharacter(value));
	}

	/**
	 * The value stored by this item, as an {@link Integer}.
	 * 
	 * @param value
	 *            the new value
	 */
	public void setInteger(int value) {
		setString(BundleHelper.fromInteger(value));
	}

	/**
	 * The value stored by this item, as a colour (represented here as an
	 * {@link Integer}) if it represents a colour, or NULL if it doesn't.
	 * <p>
	 * The returned colour value is an ARGB value.
	 * 
	 * @param value
	 *            the value
	 */
	public void setColor(int value) {
		setString(BundleHelper.fromColor(value));
	}

	/**
	 * A {@link String} representation of the default list of values.
	 * <p>
	 * The list of values is comma-separated and each value is surrounded by
	 * double-quotes; backslashes and double-quotes are escaped by a backslash.
	 * 
	 * @param value
	 *            the {@link String} representation
	 * 
	 */
	public void setList(List<String> value) {
		setString(BundleHelper.fromList(value));
	}

	/**
	 * Reload the value from the {@link Bundle}, so the last value that was
	 * saved will be used.
	 */
	public void reload() {
		if (bundle.isSet(id, false)) {
			value = bundle.getString(id);
		} else {
			value = null;
		}

		for (Runnable listener : reloadedListeners) {
			try {
				listener.run();
			} catch (Exception e) {
				// TODO: error management?
				e.printStackTrace();
			}
		}
	}

	/**
	 * Add a listener that will be called <b>after</b> a reload operation.
	 * <p>
	 * You could use it to refresh the UI for instance.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void addReloadedListener(Runnable listener) {
		reloadedListeners.add(listener);
	}

	/**
	 * Save the current value to the {@link Bundle}.
	 */
	public void save() {
		for (Runnable listener : saveListeners) {
			try {
				listener.run();
			} catch (Exception e) {
				// TODO: error management?
				e.printStackTrace();
			}
		}
		bundle.setString(id, value);
	}

	/**
	 * Add a listener that will be called <b>before</b> a save operation.
	 * <p>
	 * You could use it to make some modification to the stored value before it
	 * is saved.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void addSaveListener(Runnable listener) {
		saveListeners.add(listener);
	}

	/**
	 * The sub-items if any (if no sub-items, will return an empty list).
	 * <p>
	 * Sub-items are declared when a {@link Meta} has an ID that starts with the
	 * ID of a {@link Meta#group()} {@link MetaInfo}.
	 * <p>
	 * For instance:
	 * <ul>
	 * <li>{@link Meta} <tt>MY_PREFIX</tt> is a {@link Meta#group()}</li>
	 * <li>{@link Meta} <tt>MY_PREFIX_DESCRIPTION</tt> is another {@link Meta}</li>
	 * <li><tt>MY_PREFIX_DESCRIPTION</tt> will be a child of <tt>MY_PREFIX</tt></li>
	 * </ul>
	 * 
	 * @return the sub-items if any
	 */
	public List<MetaInfo<E>> getChildren() {
		return children;
	}

	@Override
	public Iterator<MetaInfo<E>> iterator() {
		return children.iterator();
	}

	/**
	 * Create a list of {@link MetaInfo}, one for each of the item in the given
	 * {@link Bundle}.
	 * 
	 * @param <E>
	 *            the type of {@link Bundle} to edit
	 * @param type
	 *            a class instance of the item type to work on
	 * @param bundle
	 *            the {@link Bundle} to sort through
	 * 
	 * @return the list
	 */
	static public <E extends Enum<E>> List<MetaInfo<E>> getItems(Class<E> type,
			Bundle<E> bundle) {
		List<MetaInfo<E>> list = new ArrayList<MetaInfo<E>>();
		List<MetaInfo<E>> shadow = new ArrayList<MetaInfo<E>>();
		for (E id : type.getEnumConstants()) {
			MetaInfo<E> info = new MetaInfo<E>(type, bundle, id);
			list.add(info);
			shadow.add(info);
		}

		for (int i = 0; i < list.size(); i++) {
			MetaInfo<E> info = list.get(i);

			MetaInfo<E> parent = findParent(info, shadow);
			if (parent != null) {
				list.remove(i--);
				parent.children.add(info);
				info.name = idToName(info.id, parent.id);
			}
		}

		return list;
	}

	/**
	 * Find the longest parent of the given {@link MetaInfo}, which means:
	 * <ul>
	 * <li>the parent is a {@link Meta#group()}</li>
	 * <li>the parent Id is a substring of the Id of the given {@link MetaInfo}</li>
	 * <li>there is no other parent sharing a substring for this
	 * {@link MetaInfo} with a longer Id</li>
	 * </ul>
	 * 
	 * @param <E>
	 *            the kind of enum
	 * @param info
	 *            the info to look for a parent for
	 * @param candidates
	 *            the list of potential parents
	 * 
	 * @return the longest parent or NULL if no parent is found
	 */
	static private <E extends Enum<E>> MetaInfo<E> findParent(MetaInfo<E> info,
			List<MetaInfo<E>> candidates) {
		String id = info.id.toString();
		MetaInfo<E> group = null;
		for (MetaInfo<E> pcandidate : candidates) {
			if (pcandidate.isGroup()) {
				String candidateId = pcandidate.id.toString();
				if (!id.equals(candidateId) && id.startsWith(candidateId)) {
					if (group == null
							|| group.id.toString().length() < candidateId
									.length()) {
						group = pcandidate;
					}
				}
			}
		}

		return group;
	}

	static private <E extends Enum<E>> String idToName(E id, E prefix) {
		String name = id.toString();
		if (prefix != null && name.startsWith(prefix.toString())) {
			name = name.substring(prefix.toString().length());
		}

		if (name.length() > 0) {
			name = name.substring(0, 1).toUpperCase()
					+ name.substring(1).toLowerCase();
		}

		name = name.replace("_", " ");

		return name.trim();
	}
}
