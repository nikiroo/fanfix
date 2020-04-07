
package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;

/**
 * A generic search/filter bar.
 * 
 * @author niki
 */
public class SearchBar extends JPanel {
	static private final long serialVersionUID = 1L;

	private JButton search;
	private JTextField text;
	private JButton clear;

	private boolean realTime;

	/**
	 * Create a new {@link SearchBar}.
	 */
	public SearchBar() {
		setLayout(new BorderLayout());

		search = new JButton(IconGenerator.get(Icon.search, Size.x16));
		UiHelper.setButtonPressed(search, realTime);
		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				realTime = !realTime;
				UiHelper.setButtonPressed(search, realTime);
				text.requestFocus();

				if (realTime) {
					fireActionPerformed();
				}
			}
		});

		text = new JTextField();
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(final KeyEvent e) {
				super.keyTyped(e);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						boolean empty = (text.getText().isEmpty());
						clear.setVisible(!empty);

						if (realTime) {
							fireActionPerformed();
						}
					}
				});
			}
		});
		text.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!realTime) {
					fireActionPerformed();
				}
			}
		});

		clear = new JButton(IconGenerator.get(Icon.clear, Size.x16));
		clear.setBackground(text.getBackground());
		clear.setVisible(false);
		clear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				text.setText("");
				clear.setVisible(false);
				text.requestFocus();

				fireActionPerformed();
			}
		});

		add(search, BorderLayout.WEST);
		add(text, BorderLayout.CENTER);
		add(clear, BorderLayout.EAST);
	}

	/**
	 * Adds the specified action listener to receive action events from this
	 * {@link SearchBar}.
	 *
	 * @param listener the action listener to be added
	 */
	public synchronized void addActionListener(ActionListener listener) {
		listenerList.add(ActionListener.class, listener);
	}

	/**
	 * Removes the specified action listener so that it no longer receives action
	 * events from this {@link SearchBar}.
	 *
	 * @param listener the action listener to be removed
	 */
	public synchronized void removeActionListener(ActionListener listener) {
		listenerList.remove(ActionListener.class, listener);
	}

	/**
	 * Notify the listeners of an action.
	 */
	protected void fireActionPerformed() {
		ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, getText());
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ActionListener.class) {
				((ActionListener) listeners[i + 1]).actionPerformed(e);
			}
		}
	}

	/**
	 * Return the current text displayed by this {@link SearchBar}.
	 * 
	 * @return the text
	 */
	public String getText() {
		return text.getText();
	}
}
