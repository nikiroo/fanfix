package be.nikiroo.fanfix_swing.gui.utils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JPanel;

import be.nikiroo.fanfix_swing.gui.SearchBar;

/**
 * A {@link JPanel} with the default {@link ActionListener} add/remove/fire
 * methods.
 * <p>
 * Note that it will queue all events until at least one listener comes (or
 * comes back!); this first (or at least curently unique) listener will drain
 * the queue.
 * 
 * @author niki
 */
public class ListenerPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private final Queue<ActionEvent> waitingQueue;

	/**
	 * Create a new {@link ListenerPanel}.
	 */
	public ListenerPanel() {
		waitingQueue = new LinkedList<ActionEvent>();
	}

	/**
	 * Check that this {@link ListenerPanel} currently has {@link ActionListener}s
	 * that listen on it.
	 * 
	 * @return TRUE if it has
	 */
	public synchronized boolean hasListeners() {
		return listenerList.getListenerList().length > 1;
	}

	/**
	 * Check how many events are currently waiting for an {@link ActionListener}.
	 * 
	 * @return the number of waiting events (can be 0)
	 */
	public synchronized int getWaitingEventCount() {
		return waitingQueue.size();
	}

	/**
	 * Adds the specified action listener to receive action events from this
	 * {@link SearchBar}.
	 *
	 * @param listener the action listener to be added
	 */
	public synchronized void addActionListener(ActionListener listener) {
		if (!hasListeners()) {
			while (!waitingQueue.isEmpty()) {
				listener.actionPerformed(waitingQueue.remove());
			}
		}

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
	protected synchronized void fireActionPerformed(String listenerCommand) {
		ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, listenerCommand);
		if (hasListeners()) {
			Object[] listeners = listenerList.getListenerList();
			for (int i = listeners.length - 2; i >= 0; i -= 2) {
				if (listeners[i] == ActionListener.class) {
					((ActionListener) listeners[i + 1]).actionPerformed(e);
				}
			}
		} else {
			waitingQueue.add(e);
		}
	}
}
