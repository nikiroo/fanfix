package be.nikiroo.utils.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.JPanel;

/**
 * A {@link JPanel} with the default {@link ActionListener} add/remove/fire
 * methods.
 * <p>
 * Note that it will queue all events until at least one listener comes (or
 * comes back!); this first (or at least currently unique) listener will drain
 * the queue.
 * 
 * @author niki
 */
public class ListenerPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	/** Waiting queue until at least one listener is here to get the events. */
	private final Queue<ActionEvent> waitingQueue;

	/**
	 * Create a new {@link ListenerPanel}.
	 */
	public ListenerPanel() {
		waitingQueue = new LinkedList<ActionEvent>();
	}

	/**
	 * Check that this {@link ListenerPanel} currently has
	 * {@link ActionListener}s that listen on it.
	 * 
	 * @return TRUE if it has
	 */
	public synchronized boolean hasListeners() {
		return listenerList.getListenerList().length > 1;
	}

	/**
	 * Check how many events are currently waiting for an
	 * {@link ActionListener}.
	 * 
	 * @return the number of waiting events (can be 0)
	 */
	public synchronized int getWaitingEventCount() {
		return waitingQueue.size();
	}

	/**
	 * Adds the specified action listener to receive action events from this
	 * {@link ListenerPanel}.
	 *
	 * @param listener
	 *            the action listener to be added
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
	 * Removes the specified action listener so that it no longer receives
	 * action events from this {@link ListenerPanel}.
	 *
	 * @param listener
	 *            the action listener to be removed
	 */
	public synchronized void removeActionListener(ActionListener listener) {
		listenerList.remove(ActionListener.class, listener);
	}

	/**
	 * Notify the listeners of an action.
	 * 
	 * @param listenerCommand
	 *            A string that may specify a command (possibly one of several)
	 *            associated with the event
	 */
	protected synchronized void fireActionPerformed(String listenerCommand) {
		ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
				listenerCommand);

		ActionListener[] listeners = getListeners(ActionListener.class);
		if (listeners.length > 0) {
			for (ActionListener action : listeners) {
				action.actionPerformed(e);
			}
		} else {
			waitingQueue.add(e);
		}
	}
}
