package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import be.nikiroo.utils.resources.Bundle;
import be.nikiroo.utils.resources.Meta;

/**
 * A graphical item that reflect a configuration option from the given
 * {@link Bundle}.
 * 
 * @author niki
 * 
 * @param <E>
 *            the type of {@link Bundle} to edit
 */
public class ConfigItem<E extends Enum<E>> extends JPanel {
	private static final long serialVersionUID = 1L;
	private Class<E> type;
	private final Bundle<E> bundle;
	private final E id;

	private Meta meta;
	private String value;

	private JTextField valueField;

	public ConfigItem(Class<E> type, Bundle<E> bundle, E id) {
		this.type = type;
		this.bundle = bundle;
		this.id = id;

		try {
			this.meta = type.getDeclaredField(id.name()).getAnnotation(
					Meta.class);
		} catch (NoSuchFieldException e) {
		} catch (SecurityException e) {
		}

		this.setLayout(new BorderLayout());
		this.setBorder(new EmptyBorder(2, 10, 2, 10));

		String tooltip = null;
		if (bundle.getDescriptionBundle() != null) {
			tooltip = bundle.getDescriptionBundle().getString(id);
			if (tooltip != null && tooltip.trim().isEmpty()) {
				tooltip = null;
			}
		}

		String name = id.toString();
		if (name.length() > 1) {
			name = name.substring(0, 1) + name.substring(1).toLowerCase();
			name = name.replace("_", " ");
		}

		JLabel nameLabel = new JLabel(name);
		nameLabel.setToolTipText(tooltip);
		nameLabel.setPreferredSize(new Dimension(400, 0));
		this.add(nameLabel, BorderLayout.WEST);

		valueField = new JTextField();
		valueField.setText(value);

		reload();
		this.add(valueField, BorderLayout.CENTER);
	}

	/**
	 * Reload the value from the {@link Bundle}.
	 */
	public void reload() {
		value = bundle.getString(id);
		valueField.setText(value);
	}

	/**
	 * Save the current value to the {@link Bundle}.
	 */
	public void save() {
		value = valueField.getText();
		bundle.setString(id, value);
	}

	/**
	 * Create a list of {@link ConfigItem}, one for each of the item in the
	 * given {@link Bundle}.
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
	static public <E extends Enum<E>> List<ConfigItem<E>> getItems(
			Class<E> type, Bundle<E> bundle) {
		List<ConfigItem<E>> list = new ArrayList<ConfigItem<E>>();
		for (E id : type.getEnumConstants()) {
			list.add(new ConfigItem<E>(type, bundle, id));
		}

		return list;
	}
}
