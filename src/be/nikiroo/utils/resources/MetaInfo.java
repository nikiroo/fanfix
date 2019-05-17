package be.nikiroo.utils.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
			if (meta.info() != null && !meta.info().isEmpty()) {
				if (!description.isEmpty()) {
					description += "\n\n";
				}
				description += meta.info();
			}
		}

		String name = id.toString();
		if (name.length() > 1) {
			name = name.substring(0, 1) + name.substring(1).toLowerCase();
			name = name.replace("_", " ");
		}

		this.name = name;
		this.description = description;

		reload();
	}

	/**
	 * THe name of this item, deduced from its ID.
	 * <p>
	 * In other words, it is the ID but presented in a displayable form.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * The description of this item (information to present to the user).
	 * 
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	public Format getFormat() {
		return meta.format();
	}

	// for ComboBox, this is mostly a suggestion
	public String[] getAllowedValues() {
		return meta.list();
	}

	// TODO: use it!
	public boolean isArray() {
		return meta.array();
	}

	/**
	 * The value stored by this item, as a {@link String}.
	 * 
	 * @return the value
	 */
	public String getString() {
		return value;
	}

	public String getDefaultString() {
		return meta.def();
	}

	public Boolean getBoolean() {
		return BundleHelper.parseBoolean(getString());
	}

	public Boolean getDefaultBoolean() {
		return BundleHelper.parseBoolean(getDefaultString());
	}

	public Character getCharacter() {
		return BundleHelper.parseCharacter(getString());
	}

	public Character getDefaultCharacter() {
		return BundleHelper.parseCharacter(getDefaultString());
	}

	public Integer getInteger() {
		return BundleHelper.parseInteger(getString());
	}

	public Integer getDefaultInteger() {
		return BundleHelper.parseInteger(getDefaultString());
	}

	public Integer getColor() {
		return BundleHelper.parseColor(getString());
	}

	public Integer getDefaultColor() {
		return BundleHelper.parseColor(getDefaultString());
	}

	public List<String> getList() {
		return BundleHelper.parseList(getString());
	}

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

	public void setBoolean(boolean value) {
		setString(BundleHelper.fromBoolean(value));
	}

	public void setCharacter(char value) {
		setString(BundleHelper.fromCharacter(value));
	}

	public void setInteger(int value) {
		setString(BundleHelper.fromInteger(value));
	}

	public void setColor(int value) {
		setString(BundleHelper.fromColor(value));
	}

	public void setList(List<String> value) {
		setString(BundleHelper.fromList(value));
	}

	/**
	 * Reload the value from the {@link Bundle}.
	 */
	public void reload() {
		value = bundle.getString(id);
		for (Runnable listener : reloadedListeners) {
			try {
				listener.run();
			} catch (Exception e) {
				// TODO: error management?
				e.printStackTrace();
			}
		}
	}

	// listeners will be called AFTER reload
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

	// listeners will be called BEFORE save
	public void addSaveListener(Runnable listener) {
		saveListeners.add(listener);
	}

	@Override
	public Iterator<MetaInfo<E>> iterator() {
		return children.iterator();
	}

	public List<MetaInfo<E>> getChildren() {
		return children;
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
		for (E id : type.getEnumConstants()) {
			list.add(new MetaInfo<E>(type, bundle, id));
		}

		return list;
	}

	// TODO: multiple levels?
	static public <E extends Enum<E>> Map<MetaInfo<E>, List<MetaInfo<E>>> getGroupedItems(
			Class<E> type, Bundle<E> bundle) {
		Map<MetaInfo<E>, List<MetaInfo<E>>> map = new TreeMap<MetaInfo<E>, List<MetaInfo<E>>>();
		Map<MetaInfo<E>, List<MetaInfo<E>>> map1 = new TreeMap<MetaInfo<E>, List<MetaInfo<E>>>();

		List<MetaInfo<E>> ungrouped = new ArrayList<MetaInfo<E>>();
		for (MetaInfo<E> info : getItems(type, bundle)) {
			if (info.meta.group()) {
				List<MetaInfo<E>> list = new ArrayList<MetaInfo<E>>();
				map.put(info, list);
				map1.put(info, list);
			} else {
				ungrouped.add(info);
			}
		}

		for (int i = 0; i < ungrouped.size(); i++) {
			MetaInfo<E> info = ungrouped.get(i);
			MetaInfo<E> group = findParent(info, map.keySet());
			if (group != null) {
				map.get(group).add(info);
				ungrouped.remove(i--);
			}
		}

		if (ungrouped.size() > 0) {
			map.put(null, ungrouped);
		}

		return map;
	}

	static private <E extends Enum<E>> MetaInfo<E> findParent(MetaInfo<E> info,
			Set<MetaInfo<E>> candidates) {
		MetaInfo<E> group = null;
		for (MetaInfo<E> pcandidate : candidates) {
			if (info.id.toString().startsWith(pcandidate.id.toString())) {
				if (group == null
						|| group.id.toString().length() < pcandidate.id
								.toString().length()) {
					group = pcandidate;
				}
			}
		}

		return group;
	}
}
