package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.searchable.BasicSearchable;
import be.nikiroo.fanfix.searchable.SearchableTag;
import be.nikiroo.fanfix.supported.SupportType;

/**
 * This panel represents a search panel that works for keywords and tags based
 * searches.
 * 
 * @author niki
 */
public class GuiReaderSearchByPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private Waitable waitable;

	private boolean searchByTags;
	private JTabbedPane searchTabs;
	private GuiReaderSearchByNamePanel byName;
	private GuiReaderSearchByTagPanel byTag;

	/**
	 * This interface represents an item that wan be put in "wait" mode. It is
	 * supposed to be used for long running operations during which we want to
	 * disable UI interactions.
	 * <p>
	 * It also allows reporting an event to the item.
	 * 
	 * @author niki
	 */
	public interface Waitable {
		/**
		 * Set the item in wait mode, blocking it from accepting UI input.
		 * 
		 * @param waiting
		 *            TRUE for wait more, FALSE to restore normal mode
		 */
		public void setWaiting(boolean waiting);

		/**
		 * Notify the {@link Waitable} that an event occured (i.e., new stories
		 * were found).
		 */
		public void fireEvent();
	}

	/**
	 * Create a new {@link GuiReaderSearchByPanel}.
	 * 
	 * @param waitable
	 *            the waitable we can wait on for long UI operations
	 */
	public GuiReaderSearchByPanel(Waitable waitable) {
		setLayout(new BorderLayout());

		this.waitable = waitable;
		searchByTags = false;

		byName = new GuiReaderSearchByNamePanel(waitable);
		byTag = new GuiReaderSearchByTagPanel(waitable);

		searchTabs = new JTabbedPane();
		searchTabs.addTab("By name", byName);
		searchTabs.addTab("By tags", byTag);
		searchTabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				searchByTags = (searchTabs.getSelectedComponent() == byTag);
			}
		});

		add(searchTabs, BorderLayout.CENTER);
		updateSearchBy(searchByTags);
	}

	/**
	 * Set the new {@link SupportType}.
	 * <p>
	 * This operation can be long and should be run outside the UI thread.
	 * <p>
	 * Note that if a non-searchable {@link SupportType} is used, an
	 * {@link IllegalArgumentException} will be thrown.
	 * 
	 * @param supportType
	 *            the support mode, must be searchable or NULL
	 * 
	 * @throws IllegalArgumentException
	 *             if the {@link SupportType} is not NULL but not searchable
	 *             (see {@link BasicSearchable#getSearchable(SupportType)})
	 */
	public void setSupportType(SupportType supportType) {
		BasicSearchable searchable = BasicSearchable.getSearchable(supportType);
		if (searchable == null && supportType != null) {
			throw new IllegalArgumentException("Unupported support type: "
					+ supportType);
		}

		byName.setSearchable(searchable);
		byTag.setSearchable(searchable);
	}

	/**
	 * The currently displayed page of result for the current search (see the
	 * <tt>page</tt> parameter of
	 * {@link GuiReaderSearchByPanel#search(String, int, int)} or
	 * {@link GuiReaderSearchByPanel#searchTag(SupportType, int, int, SearchableTag)}
	 * ).
	 * 
	 * @return the currently displayed page of results
	 */
	public int getPage() {
		if (!searchByTags) {
			return byName.getPage();
		}

		return byTag.getPage();
	}

	/**
	 * The number of pages of result for the current search (see the
	 * <tt>page</tt> parameter of
	 * {@link GuiReaderSearchByPanel#search(String, int, int)} or
	 * {@link GuiReaderSearchByPanel#searchTag(SupportType, int, int, SearchableTag)}
	 * ).
	 * <p>
	 * For an unknown number or when not applicable, -1 is returned.
	 * 
	 * @return the number of pages of results or -1
	 */
	public int getMaxPage() {
		if (!searchByTags) {
			return byName.getMaxPage();
		}

		return byTag.getMaxPage();
	}

	/**
	 * Set the page of results to display for the current search. This will
	 * cause {@link Waitable#fireEvent()} to be called if needed.
	 * <p>
	 * This operation can be long and should be run outside the UI thread.
	 * 
	 * @param page
	 *            the page of results to set
	 * 
	 * @throw IndexOutOfBoundsException if the page is out of bounds
	 */
	public void setPage(int page) {
		if (searchByTags) {
			searchTag(byTag.getCurrentTag(), page, 0);
		} else {
			search(byName.getCurrentKeywords(), page, 0);
		}
	}

	/**
	 * The currently loaded stories (the result of the latest search).
	 * 
	 * @return the stories
	 */
	public List<MetaData> getStories() {
		if (!searchByTags) {
			return byName.getStories();
		}

		return byTag.getStories();
	}

	/**
	 * Return the currently selected story (the <tt>item</tt>) if it was
	 * specified in the latest, or 0 if not.
	 * <p>
	 * Note: this is thus a 1-based index, <b>not</b> a 0-based index.
	 * 
	 * @return the item
	 */
	public int getStoryItem() {
		if (!searchByTags) {
			return byName.getStoryItem();
		}

		return byTag.getStoryItem();
	}

	/**
	 * Update the kind of searches to make: search by keywords or search by tags
	 * (it will impact what the user can see and interact with on the UI).
	 * 
	 * @param byTag
	 *            TRUE for tag-based searches, FALSE for keywords-based searches
	 */
	private void updateSearchBy(final boolean byTag) {
		GuiReaderSearchFrame.inUi(new Runnable() {
			@Override
			public void run() {
				if (!byTag) {
					searchTabs.setSelectedIndex(0);
				} else {
					searchTabs.setSelectedIndex(1);
				}
			}
		});
	}

	/**
	 * Search for the given terms on the currently selected searchable. This
	 * will cause {@link Waitable#fireEvent()} to be called if needed.
	 * <p>
	 * This operation can be long and should be run outside the UI thread.
	 * 
	 * @param keywords
	 *            the keywords to search for
	 * @param page
	 *            the page of results to load
	 * @param item
	 *            the item to select (or 0 for none by default)
	 * 
	 * @throw IndexOutOfBoundsException if the page is out of bounds
	 */
	public void search(final String keywords, final int page, final int item) {
		updateSearchBy(false);
		byName.search(keywords, page, item);
		waitable.fireEvent();
	}

	/**
	 * Search for the given tag on the currently selected searchable. This will
	 * cause {@link Waitable#fireEvent()} to be called if needed.
	 * <p>
	 * If the tag contains children tags, those will be displayed so you can
	 * select them; if the tag is a leaf tag, the linked stories will be
	 * displayed.
	 * <p>
	 * This operation can be long and should be run outside the UI thread.
	 * 
	 * @param tag
	 *            the tag to search for, or NULL for base tags
	 * @param page
	 *            the page of results to load
	 * @param item
	 *            the item to select (or 0 for none by default)
	 * 
	 * @throw IndexOutOfBoundsException if the page is out of bounds
	 */
	public void searchTag(final SearchableTag tag, final int page,
			final int item) {
		updateSearchBy(true);
		byTag.searchTag(tag, page, item);
		waitable.fireEvent();
	}

	/**
	 * Enables or disables this component, depending on the value of the
	 * parameter <code>b</code>. An enabled component can respond to user input
	 * and generate events. Components are enabled initially by default.
	 * <p>
	 * Disabling this component will also affect its children.
	 * 
	 * @param b
	 *            If <code>true</code>, this component is enabled; otherwise
	 *            this component is disabled
	 */
	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		searchTabs.setEnabled(b);
		byName.setEnabled(b);
		byTag.setEnabled(b);
	}
}
