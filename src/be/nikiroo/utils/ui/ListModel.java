package be.nikiroo.utils.ui;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;

import be.nikiroo.utils.compat.DefaultListModel6;
import be.nikiroo.utils.compat.JList6;
import be.nikiroo.utils.compat.ListCellRenderer6;

/**
 * A {@link javax.swing.ListModel} that can maintain 2 lists; one with the
 * actual data (the elements), and a second one with the items that are
 * currently displayed (the items).
 * <p>
 * It also offers filter options, supports hovered changes and some more utility
 * functions.
 * 
 * @author niki
 *
 * @param <T>
 *            the type of elements and items (the same type)
 */
public class ListModel<T> extends DefaultListModel6<T> {
	private static final long serialVersionUID = 1L;

	/**
	 * A filter interface, to check for a condition (note that a Predicate class
	 * already exists in Java 1.8+, and is compatible with this one if you
	 * change the signatures -- but I support java 1.6+).
	 * 
	 * @author niki
	 *
	 * @param <T>
	 *            the type of elements and items (the same type)
	 */
	public interface Predicate<T> {
		/**
		 * Check if an item or an element pass a filter.
		 * 
		 * @param item
		 *            the item to test
		 * 
		 * @return TRUE if the test passed, FALSE if not
		 */
		public boolean test(T item);
	}

	/**
	 * A simple interface your elements must implement if you want to use
	 * {@link ListModel#generateRenderer(ListModel)}.
	 * 
	 * @author niki
	 */
	public interface Hoverable {
		/**
		 * The element is currently selected.
		 * 
		 * @param selected
		 *            TRUE for selected, FALSE for unselected
		 */
		public void setSelected(boolean selected);

		/**
		 * The element is currently under the mouse cursor.
		 * 
		 * @param hovered
		 *            TRUE if it is, FALSE if not
		 */
		public void setHovered(boolean hovered);
	}

	private int hoveredIndex;
	private List<T> items = new ArrayList<T>();
	private JList6<T> list;

	/**
	 * Create a new {@link ListModel}.
	 * 
	 * @param list
	 *            the {@link JList} we will handle the data of (cannot be NULL)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" }) // not compatible Java 1.6
	public ListModel(JList list) {
		this((JList6<T>) list);
	}

	/**
	 * Create a new {@link ListModel}.
	 * 
	 * @param list
	 *            the {@link JList} we will handle the data of (cannot be NULL)
	 * @param popup
	 *            the popup to use and keep track of (can be NULL)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" }) // not compatible Java 1.6
	public ListModel(final JList list, final JPopupMenu popup) {
		this((JList6<T>) list, popup);
	}

	/**
	 * Create a new {@link ListModel}.
	 * 
	 * @param list
	 *            the {@link JList6} we will handle the data of (cannot be NULL)
	 */
	public ListModel(JList6<T> list) {
		this(list, null);
	}

	/**
	 * Create a new {@link ListModel}.
	 * 
	 * @param list
	 *            the {@link JList6} we will handle the data of (cannot be NULL)
	 * @param popup
	 *            the popup to use and keep track of (can be NULL)
	 */
	public ListModel(final JList6<T> list, final JPopupMenu popup) {
		this.list = list;
		list.setModel(this);

		list.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent me) {
				if (popup != null && popup.isShowing())
					return;

				Point p = new Point(me.getX(), me.getY());
				int index = list.locationToIndex(p);
				if (index != hoveredIndex) {
					int oldIndex = hoveredIndex;
					hoveredIndex = index;
					fireElementChanged(oldIndex);
					fireElementChanged(index);
				}
			}
		});

		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				check(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				check(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (popup != null && popup.isShowing())
					return;

				if (hoveredIndex > -1) {
					int oldIndex = hoveredIndex;
					hoveredIndex = -1;
					fireElementChanged(oldIndex);
				}
			}

			private void check(MouseEvent e) {
				if (popup == null) {
					return;
				}

				if (e.isPopupTrigger()) {
					if (list.getSelectedIndices().length <= 1) {
						list.setSelectedIndex(
								list.locationToIndex(e.getPoint()));
					}

					popup.show(list, e.getX(), e.getY());
				}
			}
		});
	}

	/**
	 * Check if this element is currently under the mouse.
	 * 
	 * @param element
	 *            the element to check
	 * 
	 * @return TRUE if it is
	 */
	public boolean isHovered(T element) {
		return indexOf(element) == hoveredIndex;
	}

	/**
	 * Check if this element is currently under the mouse.
	 * 
	 * @param index
	 *            the index of the element to check
	 * 
	 * @return TRUE if it is
	 */
	public boolean isHovered(int index) {
		return index == hoveredIndex;
	}

	/**
	 * Add an item to the model.
	 * 
	 * @param item
	 *            the new item to add
	 */
	public void addItem(T item) {
		items.add(item);
	}

	/**
	 * Add items to the model.
	 * 
	 * @param items
	 *            the new items to add
	 */
	public void addAllItems(Collection<T> items) {
		this.items.addAll(items);
	}

	/**
	 * Removes the first occurrence of the specified element from this list, if
	 * it is present (optional operation).
	 * 
	 * @param item
	 *            the item to remove if possible (can be NULL)
	 * 
	 * @return TRUE if one element was removed, FALSE if not found
	 */
	public boolean removeItem(T item) {
		return items.remove(item);
	}

	/**
	 * Remove the items that pass the given filter (or all items if the filter
	 * is NULL).
	 * 
	 * @param filter
	 *            the filter (if the filter returns TRUE, the item will be
	 *            removed)
	 * 
	 * @return TRUE if at least one item was removed
	 */
	public boolean removeItemIf(Predicate<T> filter) {
		boolean changed = false;
		if (filter == null) {
			changed = !items.isEmpty();
			clearItems();
		} else {
			for (int i = 0; i < items.size(); i++) {
				if (filter.test(items.get(i))) {
					items.remove(i--);
					changed = true;
				}
			}
		}

		return changed;
	}

	/**
	 * Removes all the items from this model.
	 */
	public void clearItems() {
		items.clear();
	}

	/**
	 * Filter the current elements.
	 * <p>
	 * This method will clear all the elements then look into all the items:
	 * those that pass the given filter will be copied as elements.
	 * 
	 * @param filter
	 *            the filter to select which elements to keep; an item that pass
	 *            the filter will be copied as an element (can be NULL, in that
	 *            case all items will be copied as elements)
	 */
	@SuppressWarnings("unchecked") // ListModel<T> and JList<T> are not java 1.6
	public void filter(Predicate<T> filter) {
		clear();
		for (T item : items) {
			if (filter == null || filter.test(item)) {
				addElement(item);
			}
		}

		list.repaint();
	}

	/**
	 * Return the currently selected elements.
	 * 
	 * @return the selected elements
	 */
	public List<T> getSelectedElements() {
		List<T> selected = new ArrayList<T>();
		for (int index : list.getSelectedIndices()) {
			selected.add(get(index));
		}

		return selected;
	}

	/**
	 * Return the selected element if <b>one</b> and <b>only one</b> element is
	 * selected. I.E., if zero, two or more elements are selected, NULL will be
	 * returned.
	 * 
	 * @return the element if it is the only selected element, NULL otherwise
	 */
	public T getUniqueSelectedElement() {
		List<T> selected = getSelectedElements();
		if (selected.size() == 1) {
			return selected.get(0);
		}

		return null;
	}

	/**
	 * Notify that this element has been changed.
	 * 
	 * @param index
	 *            the index of the element
	 */
	public void fireElementChanged(int index) {
		if (index >= 0) {
			fireContentsChanged(this, index, index);
		}
	}

	/**
	 * Notify that this element has been changed.
	 * 
	 * @param element
	 *            the element
	 */
	public void fireElementChanged(T element) {
		int index = indexOf(element);
		if (index >= 0) {
			fireContentsChanged(this, index, index);
		}
	}

	@SuppressWarnings("unchecked") // ListModel<T> and JList<T> are not java 1.6
	@Override
	public T get(int index) {
		return (T) super.get(index);
	}

	/**
	 * Generate a {@link ListCellRenderer} that supports {@link Hoverable}
	 * elements.
	 * 
	 * @param <T>
	 *            the type of elements and items (the same type), which should
	 *            implement {@link Hoverable} (it will not cause issues if not,
	 *            but then, it will be a default renderer)
	 * @param model
	 *            the model to use
	 * 
	 * @return a suitable, {@link Hoverable} compatible renderer
	 */
	static public <T extends Component> ListCellRenderer6<T> generateRenderer(
			final ListModel<T> model) {
		return new ListCellRenderer6<T>() {
			@Override
			public Component getListCellRendererComponent(JList6<T> list,
					T item, int index, boolean isSelected,
					boolean cellHasFocus) {
				if (item instanceof Hoverable) {
					Hoverable hoverable = (Hoverable) item;
					hoverable.setSelected(isSelected);
					hoverable.setHovered(model.isHovered(index));
				}

				return item;
			}
		};
	}
}
