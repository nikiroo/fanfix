package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.resources.Bundle;
import be.nikiroo.utils.resources.MetaInfo;

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
	private List<MetaInfo<E>> items;

	/**
	 * Create a new {@link ConfigEditor} for this {@link Bundle}.
	 * 
	 * @param type
	 *            a class instance of the item type to work on
	 * @param bundle
	 *            the {@link Bundle} to sort through
	 * @param title
	 *            the title to display before the options
	 */
	public ConfigEditor(Class<E> type, final Bundle<E> bundle, String title) {
		this.setLayout(new BorderLayout());
		JPanel main = new JPanel();

		JScrollPane scroll = new JScrollPane(main);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		this.add(scroll, BorderLayout.CENTER);

		main.setLayout(new BoxLayout(main, BoxLayout.PAGE_AXIS));
		main.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		main.add(new JLabel(title));

		items = MetaInfo.getItems(type, bundle);
		for (MetaInfo<E> item : items) {
			addItem(main, item, 0);
		}

		main.add(createButton("Reset", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (MetaInfo<E> item : items) {
					item.reload();
				}
			}
		}));

		main.add(createButton("Default", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object snap = bundle.takeSnapshot();
				bundle.reload(true);
				for (MetaInfo<E> item : items) {
					item.reload();
				}
				bundle.reload(false);
				bundle.restoreSnapshot(snap);
			}
		}));

		main.add(createButton("Save", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				for (MetaInfo<E> item : items) {
					item.save();
				}

				try {
					bundle.updateFile();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}));
	}

	private void addItem(JPanel main, MetaInfo<E> item, int nhgap) {
		if (item.isGroup()) {
			JPanel bpane = new JPanel(new BorderLayout());
			bpane.setBorder(new TitledBorder(item.getName()));
			JPanel pane = new JPanel();
			pane.setBorder(new EmptyBorder(5, 5, 5, 5));
			pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

			String info = item.getDescription();
			info = StringUtils.justifyTexts(info, 100);
			if (!info.isEmpty()) {
				info = info + "\n";
				JTextArea text = new JTextArea(info);
				text.setWrapStyleWord(true);
				text.setOpaque(false);
				text.setForeground(new Color(100, 100, 180));
				text.setEditable(false);
				pane.add(text);
			}

			for (MetaInfo<E> subitem : item) {
				addItem(pane, subitem, nhgap + 11);
			}
			bpane.add(pane, BorderLayout.CENTER);
			main.add(bpane);
		} else {
			main.add(new ConfigItem<E>(item, nhgap));
		}
	}

	/**
	 * Add an action button for this action.
	 * 
	 * @param title
	 *            the action title
	 * @param listener
	 *            the action
	 */
	private JComponent createButton(String title, ActionListener listener) {
		JButton button = new JButton(title);
		button.addActionListener(listener);

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(new EmptyBorder(2, 10, 2, 10));
		panel.add(button, BorderLayout.CENTER);

		return panel;
	}
}
