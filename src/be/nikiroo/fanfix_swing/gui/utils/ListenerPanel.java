package be.nikiroo.fanfix_swing.gui.utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import be.nikiroo.fanfix_swing.gui.SearchBar;

/**
 * A {@link JPanel} with the default {@link ActionListener} add/remove/fire
 * methods.
 * 
 * @author niki
 */
public class ListenerPanel extends JPanel {
	private static final long serialVersionUID = 1L;

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
	 * 
	 * @param listenerCommand A string that may specify a command (possibly one of
	 *                        several) associated with the event
	 */
	protected void fireActionPerformed(String listenerCommand) {
		ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, listenerCommand);
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ActionListener.class) {
				((ActionListener) listeners[i + 1]).actionPerformed(e);
			}
		}
	}
}
