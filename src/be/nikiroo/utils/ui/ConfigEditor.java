package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import be.nikiroo.utils.resources.Bundle;

/**
 * A configuration panel for a {@link Bundle}.
 * <p>
 * All the items in the given {@link Bundle} will be displayed in editable
 * controls, with options to Save, Reset and/or Reset to the application default
 * values.
 * 
 * @author niki
 *
 * @param <E>
 *            the type of {@link Bundle} to edit
 */
public class ConfigEditor<E extends Enum<E>> extends JPanel {
	private static final long serialVersionUID = 1L;
	private List<ConfigItem<E>> items;

	/**
	 * Create a new {@link ConfigEditor} for this {@link Bundle}.
	 * 
	 * @param type
	 *            a class instance of the item type to work on
	 * @param bundle
	 *            the {@link Bundle} to sort through
	 */
	public ConfigEditor(Class<E> type, final Bundle<E> bundle) {
		this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		items = ConfigItem.getItems(type, bundle);
		for (ConfigItem<E> item : items) {
			this.add(item);
		}

		addButton("Reset", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (ConfigItem<E> item : items) {
					item.reload();
				}
			}
		});

		addButton("Default", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object snap = bundle.takeSnapshot();
				bundle.reload(true);
				for (ConfigItem<E> item : items) {
					item.reload();
				}
				bundle.reload(false);
				bundle.restoreSnapshot(snap);
			}
		});

		addButton("Save", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				for (ConfigItem<E> item : items) {
					item.save();
				}

				try {
					bundle.updateFile();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}

	/**
	 * Add an action button for this action.
	 * 
	 * @param title
	 *            the action title
	 * @param listener
	 *            the action
	 */
	private void addButton(String title, ActionListener listener) {
		JButton button = new JButton(title);
		button.addActionListener(listener);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(new EmptyBorder(2, 10, 2, 10));
		panel.add(button, BorderLayout.CENTER);

		this.add(panel);
	}
}
