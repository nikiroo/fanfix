package be.nikiroo.utils.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * A Swing-based navigation bar, that displays first/previous/next/last page
 * buttons.
 * 
 * @author niki
 */
public class NavBar extends ListenerPanel {
	private static final long serialVersionUID = 1L;

	/** The event that is fired on page change. */
	public static final String PAGE_CHANGED = "page changed";

	private JTextField page;
	private JLabel pageLabel;
	private JLabel maxPage;
	private JLabel label;

	private int index = 0;
	private int min = 0;
	private int max = 0;
	private String extraLabel = null;

	private boolean vertical;

	private JButton first;
	private JButton previous;
	private JButton next;
	private JButton last;

	/**
	 * Create a new navigation bar.
	 * <p>
	 * The minimum must be lower or equal to the maximum, but a max of "-1"
	 * means "infinite".
	 * <p>
	 * A {@link NavBar#PAGE_CHANGED} event will be fired on startup.
	 * 
	 * @param min
	 *            the minimum page number (cannot be negative)
	 * @param max
	 *            the maximum page number (cannot be lower than min, except if
	 *            -1 (infinite))
	 * 
	 * @throws IndexOutOfBoundsException
	 *             if min &gt; max and max is not "-1"
	 */
	public NavBar(int min, int max) {
		if (min > max && max != -1) {
			throw new IndexOutOfBoundsException(
					String.format("min (%d) > max (%d)", min, max));
		}

		// Page navigation
		first = new JButton();
		first.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				first();
			}
		});

		previous = new JButton();
		previous.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				previous();
			}
		});

		final int defaultHeight = new JButton("dummy")
				.getPreferredSize().height;
		final int width4 = new JButton("1234").getPreferredSize().width;
		page = new JTextField(Integer.toString(min));
		page.setPreferredSize(new Dimension(width4, defaultHeight));
		page.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					int pageNb = Integer.parseInt(page.getText());
					if (pageNb < NavBar.this.min || pageNb > NavBar.this.max) {
						throw new NumberFormatException("invalid");
					}

					if (setIndex(pageNb))
						fireActionPerformed(PAGE_CHANGED);
				} catch (NumberFormatException nfe) {
					page.setText(Integer.toString(index));
				}
			}
		});

		pageLabel = new JLabel(Integer.toString(min));
		pageLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

		maxPage = new JLabel("of " + max);
		maxPage.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

		next = new JButton();
		next.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				next();
			}
		});

		last = new JButton();
		last.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				last();
			}
		});

		label = new JLabel("");

		// Set the << < > >> "icons"
		setIcons(null, null, null, null);

		this.min = min;
		this.max = max;
		this.index = min;

		updateEnabled();
		updateLabel();
		setOrientation(vertical);

		fireActionPerformed(PAGE_CHANGED);
	}

	/**
	 * The current index, must be between {@link NavBar#min} and
	 * {@link NavBar#max}, both inclusive.
	 * 
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * The current index, should be between {@link NavBar#min} and
	 * {@link NavBar#max}, both inclusive.
	 * 
	 * @param index
	 *            the new index
	 * 
	 * @return TRUE if the index changed, FALSE if not (either it was already at
	 *         that value, or it is outside of the bounds set by
	 *         {@link NavBar#min} and {@link NavBar#max})
	 */
	public synchronized boolean setIndex(int index) {
		if (index != this.index) {
			if (index < min || (index > max && max != -1)) {
				return false;
			}

			this.index = index;
			updateLabel();
			updateEnabled();

			return true;
		}

		return false;
	}

	/**
	 * The minimun page number. Cannot be negative.
	 * 
	 * @return the min
	 */
	public int getMin() {
		return min;
	}

	/**
	 * The minimum page number. Cannot be negative.
	 * <p>
	 * May update the index if needed (if the index is &lt; the new min).
	 * <p>
	 * Will also (always) update the label and enable/disable the required
	 * buttons.
	 * 
	 * @param min
	 *            the new min
	 */
	public synchronized void setMin(int min) {
		this.min = min;
		if (index < min) {
			index = min;
		}

		updateEnabled();
		updateLabel();
	}

	/**
	 * The maximum page number. Cannot be lower than min, except if -1
	 * (infinite).
	 * 
	 * @return the max
	 */
	public int getMax() {
		return max;
	}

	/**
	 * The maximum page number. Cannot be lower than min, except if -1
	 * (infinite).
	 * <p>
	 * May update the index if needed (if the index is &gt; the new max).
	 * <p>
	 * Will also (always) update the label and enable/disable the required
	 * buttons.
	 * 
	 * @param max
	 *            the new max
	 */
	public synchronized void setMax(int max) {
		this.max = max;
		if (index > max && max != -1) {
			index = max;
		}

		maxPage.setText("of " + max);
		updateEnabled();
		updateLabel();
	}

	/**
	 * The current extra label to display.
	 * 
	 * @return the current label
	 */
	public String getExtraLabel() {
		return extraLabel;
	}

	/**
	 * The current extra label to display.
	 * 
	 * @param currentLabel
	 *            the new current label
	 */
	public void setExtraLabel(String currentLabel) {
		this.extraLabel = currentLabel;
		updateLabel();
	}

	/**
	 * Change the page to the next one.
	 * 
	 * @return TRUE if it changed
	 */
	public synchronized boolean next() {
		if (setIndex(index + 1)) {
			fireActionPerformed(PAGE_CHANGED);
			return true;
		}

		return false;
	}

	/**
	 * Change the page to the previous one.
	 * 
	 * @return TRUE if it changed
	 */
	public synchronized boolean previous() {
		if (setIndex(index - 1)) {
			fireActionPerformed(PAGE_CHANGED);
			return true;
		}

		return false;
	}

	/**
	 * Change the page to the first one.
	 * 
	 * @return TRUE if it changed
	 */
	public synchronized boolean first() {
		if (setIndex(min)) {
			fireActionPerformed(PAGE_CHANGED);
			return true;
		}

		return false;
	}

	/**
	 * Change the page to the last one.
	 * 
	 * @return TRUE if it changed
	 */
	public synchronized boolean last() {
		if (setIndex(max)) {
			fireActionPerformed(PAGE_CHANGED);
			return true;
		}

		return false;
	}

	/**
	 * Set icons for the buttons instead of square brackets.
	 * <p>
	 * Any NULL value will make the button use square brackets again.
	 * 
	 * @param first
	 *            the icon of the button "go to first page"
	 * @param previous
	 *            the icon of the button "go to previous page"
	 * @param next
	 *            the icon of the button "go to next page"
	 * @param last
	 *            the icon of the button "go to last page"
	 */
	public void setIcons(Icon first, Icon previous, Icon next, Icon last) {
		this.first.setIcon(first);
		this.first.setText(first == null ? "<<" : "");
		this.previous.setIcon(previous);
		this.previous.setText(previous == null ? "<" : "");
		this.next.setIcon(next);
		this.next.setText(next == null ? ">" : "");
		this.last.setIcon(last);
		this.last.setText(last == null ? ">>" : "");
	}

	/**
	 * The general orientation of the component.
	 * 
	 * @return TRUE for vertical orientation, FALSE for horisontal orientation
	 */
	public boolean getOrientation() {
		return vertical;
	}

	/**
	 * Update the general orientation of the component.
	 * 
	 * @param vertical
	 *            TRUE for vertical orientation, FALSE for horisontal
	 *            orientation
	 * 
	 * @return TRUE if it changed something
	 */
	public boolean setOrientation(boolean vertical) {
		if (getWidth() == 0 || this.vertical != vertical) {
			this.vertical = vertical;

			BoxLayout layout = new BoxLayout(this,
					vertical ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS);
			this.removeAll();
			setLayout(layout);

			this.add(first);
			this.add(previous);
			if (vertical) {
				this.add(pageLabel);
			} else {
				this.add(page);
			}
			this.add(maxPage);
			this.add(next);
			this.add(last);

			if (!vertical) {
				this.add(label);
			}

			this.revalidate();
			this.repaint();

			return true;
		}

		return false;
	}

	/**
	 * Update the label displayed in the UI.
	 */
	private void updateLabel() {
		label.setText(getExtraLabel());
		pageLabel.setText(Integer.toString(index));
		page.setText(Integer.toString(index));
	}

	/**
	 * Update the navigation buttons "enabled" state according to the current
	 * index value.
	 */
	private synchronized void updateEnabled() {
		first.setEnabled(index > min);
		previous.setEnabled(index > min);
		next.setEnabled(index < max || max == -1);
		last.setEnabled(index < max || max == -1);
	}
}
