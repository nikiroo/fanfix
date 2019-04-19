package be.nikiroo.fanfix.reader.ui;

import java.awt.Color;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import be.nikiroo.fanfix.Instance;

/**
 * A Swing-based navigation bar, that displays first/previous/next/last page
 * buttons.
 * 
 * @author niki
 */
public class GuiReaderNavBar extends JPanel {
	private static final long serialVersionUID = 1L;

	private JLabel label;
	private int index = 0;
	private int min = 0;
	private int max = 0;
	private JButton[] navButtons;
	String extraLabel = null;

	private List<ActionListener> listeners = new ArrayList<ActionListener>();

	/**
	 * Create a new navigation bar.
	 * <p>
	 * The minimum must be lower or equal to the maximum.
	 * 
	 * @param min
	 *            the minimum page number
	 * @param max
	 *            the maximum page number
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if min &gt; max
	 */
	public GuiReaderNavBar(int min, int max) {
		if (min > max) {
			throw new IndexOutOfBoundsException(String.format(
					"min (%d) > max (%d)", min, max));
		}

		LayoutManager layout = new BoxLayout(this, BoxLayout.X_AXIS);
		setLayout(layout);

		navButtons = new JButton[4];

		navButtons[0] = createNavButton("<<", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setIndex(GuiReaderNavBar.this.min);
			}
		});
		navButtons[1] = createNavButton(" < ", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setIndex(index - 1);
			}
		});
		navButtons[2] = createNavButton(" > ", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setIndex(index + 1);
			}
		});
		navButtons[3] = createNavButton(">>", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setIndex(GuiReaderNavBar.this.max);
			}
		});

		for (JButton navButton : navButtons) {
			add(navButton);
		}

		label = new JLabel("");
		add(label);

		this.min = min;
		this.max = max;
		this.index = min;

		updateEnabled();
		updateLabel();
		fireEvent();
	}

	/**
	 * The current index, must be between {@link GuiReaderNavBar#min} and
	 * {@link GuiReaderNavBar#max}, both inclusive.
	 * 
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * The current index, must be between {@link GuiReaderNavBar#min} and
	 * {@link GuiReaderNavBar#max}, both inclusive.
	 * 
	 * @param index
	 *            the new index
	 */
	private void setIndex(int index) {
		if (index != this.index) {
			if (index < min || index > max) {
				throw new IndexOutOfBoundsException(String.format(
						"Index %d but min/max is [%d/%d]", index, min, max));
			}

			this.index = index;
			fireEvent();
			updateLabel();
		}

		updateEnabled();
	}

	/**
	 * The minimun page number.
	 * 
	 * @return the min
	 */
	public int getMin() {
		return min;
	}

	/**
	 * The minimum page number.
	 * <p>
	 * May update the index if needed (if the index is &lt; the new min).
	 * <p>
	 * Will also (always) update the label and enable/disable the required
	 * buttons.
	 * 
	 * @param min
	 *            the new min
	 */
	public void setMin(int min) {
		this.min = min;
		if (index < min) {
			index = min;
			updateEnabled();
			updateLabel();
			fireEvent();
		} else {
			updateEnabled();
			updateLabel();
		}
	}

	/**
	 * The maximum page number.
	 * 
	 * @return the max
	 */
	public int getMax() {
		return max;
	}

	/**
	 * The maximum page number.
	 * <p>
	 * May update the index if needed (if the index is &gt; the new max).
	 * <p>
	 * Will also (always) update the label and enable/disable the required
	 * buttons.
	 * 
	 * @param max
	 *            the new max
	 */
	public void setMax(int max) {
		this.max = max;
		if (index > max) {
			index = max;
			updateEnabled();
			updateLabel();
			fireEvent();
		} else {
			updateEnabled();
			updateLabel();
		}
	}

	/**
	 * The current extra label to display with the default
	 * {@link GuiReaderNavBar#computeLabel(int, int, int)} implementation.
	 * 
	 * @return the current label
	 */
	public String getExtraLabel() {
		return extraLabel;
	}

	/**
	 * The current extra label to display with the default
	 * {@link GuiReaderNavBar#computeLabel(int, int, int)} implementation.
	 * 
	 * @param currentLabel
	 *            the new current label
	 */
	public void setExtraLabel(String currentLabel) {
		this.extraLabel = currentLabel;
		updateLabel();
	}

	/**
	 * Add a listener that will be called on each page change.
	 * 
	 * @param listener
	 *            the new listener
	 */
	public void addActionListener(ActionListener listener) {
		listeners.add(listener);
	}

	/**
	 * Remove the given listener if possible.
	 * 
	 * @param listener
	 *            the listener to remove
	 * @return TRUE if it was removed, FALSE if it was not found
	 */
	public boolean removeActionListener(ActionListener listener) {
		return listeners.remove(listener);
	}

	/**
	 * Remove all the listeners.
	 */
	public void clearActionsListeners() {
		listeners.clear();
	}

	/**
	 * Notify a chnge of page.
	 */
	private void fireEvent() {
		for (ActionListener listener : listeners) {
			try {
				listener.actionPerformed(new ActionEvent(this,
						ActionEvent.ACTION_FIRST, "page changed"));
			} catch (Exception e) {
				Instance.getTraceHandler().error(e);
			}
		}
	}

	/**
	 * Create a single navigation button.
	 * 
	 * @param text
	 *            the text to display
	 * @param action
	 *            the action to take on click
	 * @return the button
	 */
	private JButton createNavButton(String text, ActionListener action) {
		JButton navButton = new JButton(text);
		navButton.addActionListener(action);
		navButton.setForeground(Color.BLUE);
		return navButton;
	}

	/**
	 * Update the label displayed in the UI.
	 */
	private void updateLabel() {
		label.setText(computeLabel(index, min, max));
	}

	/**
	 * Update the navigation buttons "enabled" state according to the current
	 * index value.
	 */
	private void updateEnabled() {
		navButtons[0].setEnabled(index > min);
		navButtons[1].setEnabled(index > min);
		navButtons[2].setEnabled(index < max);
		navButtons[3].setEnabled(index < max);
	}

	/**
	 * Return the label to display for the given index.
	 * <p>
	 * Swing HTML (HTML3) is supported if surrounded by &lt;HTML&gt; and
	 * &lt;/HTML&gt;.
	 * <p>
	 * By default, return "Page 1/5: current_label" (with the current index and
	 * {@link GuiReaderNavBar#getCurrentLabel()}).
	 * 
	 * @param index
	 *            the new index number
	 * @param mix
	 *            the minimum index (inclusive)
	 * @param max
	 *            the maximum index (inclusive)
	 * @return the label
	 */
	protected String computeLabel(int index,
			@SuppressWarnings("unused") int min, int max) {

		String base = "&nbsp;&nbsp;<B>Page <SPAN COLOR='#444466'>%d</SPAN>&nbsp;/&nbsp;%d</B>";
		String ifLabel = ": %s";

		String display = base;
		String label = getExtraLabel();
		if (label != null && !label.trim().isEmpty()) {
			display += ifLabel;
		}

		display = "<HTML>" + display + "</HTML>";

		return String.format(display, index, max, label);
	}
}
