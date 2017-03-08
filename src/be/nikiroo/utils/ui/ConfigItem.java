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
	private final Bundle<E> bundle;
	private final E id;
	private String value;

	private JTextField valueField;

	public ConfigItem(Class<E> type, Bundle<E> bundle, E id) {
		this.bundle = bundle;
		this.id = id;

		this.setLayout(new BorderLayout());
		this.setBorder(new EmptyBorder(2, 10, 2, 10));

		JLabel nameLabel = new JLabel(id.toString());
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
