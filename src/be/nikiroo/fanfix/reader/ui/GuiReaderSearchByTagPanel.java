package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.ui.GuiReaderSearchByPanel.Waitable;
import be.nikiroo.fanfix.searchable.BasicSearchable;
import be.nikiroo.fanfix.searchable.SearchableTag;
import be.nikiroo.fanfix.supported.SupportType;

/**
 * This panel represents a search panel that works for keywords and tags based
 * searches.
 * 
 * @author niki
 */
// JCombobox<E> not 1.6 compatible
@SuppressWarnings({ "unchecked", "rawtypes" })
public class GuiReaderSearchByTagPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private BasicSearchable searchable;
	private Waitable waitable;

	private SearchableTag currentTag;
	private JPanel tagBars;
	private List<JComboBox> combos;

	private int page;
	private int maxPage;
	private List<MetaData> stories = new ArrayList<MetaData>();
	private int storyItem;

	public GuiReaderSearchByTagPanel(Waitable waitable) {
		setLayout(new BorderLayout());

		this.waitable = waitable;
		combos = new ArrayList<JComboBox>();
		page = 1;
		maxPage = -1;

		tagBars = new JPanel();
		tagBars.setLayout(new BoxLayout(tagBars, BoxLayout.Y_AXIS));
		add(tagBars, BorderLayout.NORTH);
	}

	/**
	 * The {@link BasicSearchable} object use for the searches themselves.
	 * <p>
	 * This operation can be long and should be run outside the UI thread.
	 * <p>
	 * Can be NULL, but no searches will work.
	 * 
	 * @param searchable
	 *            the new searchable
	 */
	public void setSearchable(BasicSearchable searchable) {
		this.searchable = searchable;
		page = 1;
		maxPage = -1;
		storyItem = 0;
		stories = new ArrayList<MetaData>();
		updateTags(null);
	}

	/**
	 * The currently displayed page of result for the current search (see the
	 * <tt>page</tt> parameter of
	 * {@link GuiReaderSearchByTagPanel#searchTag(SupportType, int, int, SearchableTag)}
	 * ).
	 * 
	 * @return the currently displayed page of results
	 */
	public int getPage() {
		return page;
	}

	/**
	 * The number of pages of result for the current search (see the
	 * <tt>page</tt> parameter of
	 * {@link GuiReaderSearchByPanel#searchTag(SupportType, int, int, SearchableTag)}
	 * ).
	 * <p>
	 * For an unknown number or when not applicable, -1 is returned.
	 * 
	 * @return the number of pages of results or -1
	 */
	public int getMaxPage() {
		return maxPage;
	}

	/**
	 * Return the tag used for the current search.
	 * 
	 * @return the tag (which can be NULL, for "base tags")
	 */
	public SearchableTag getCurrentTag() {
		return currentTag;
	}

	/**
	 * The currently loaded stories (the result of the latest search).
	 * 
	 * @return the stories
	 */
	public List<MetaData> getStories() {
		return stories;
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
		return storyItem;
	}

	/**
	 * Update the tags displayed on screen and reset the tags bar.
	 * <p>
	 * This operation can be long and should be run outside the UI thread.
	 * 
	 * @param tag
	 *            the tag to use, or NULL for base tags
	 */
	private void updateTags(final SearchableTag tag) {
		final List<SearchableTag> parents = new ArrayList<SearchableTag>();
		SearchableTag parent = (tag == null) ? null : tag;
		while (parent != null) {
			parents.add(parent);
			parent = parent.getParent();
		}

		List<SearchableTag> rootTags = new ArrayList<SearchableTag>();
		SearchableTag selectedRootTag = null;
		selectedRootTag = parents.isEmpty() ? null : parents
				.get(parents.size() - 1);

		if (searchable != null) {
			try {
				rootTags = searchable.getTags();
			} catch (IOException e) {
				GuiReaderSearchFrame.error(e);
			}
		}

		final List<SearchableTag> rootTagsF = rootTags;
		final SearchableTag selectedRootTagF = selectedRootTag;

		GuiReaderSearchFrame.inUi(new Runnable() {
			@Override
			public void run() {
				tagBars.invalidate();
				tagBars.removeAll();

				addTagBar(rootTagsF, selectedRootTagF);

				for (int i = parents.size() - 1; i >= 0; i--) {
					SearchableTag selectedChild = null;
					if (i > 0) {
						selectedChild = parents.get(i - 1);
					}

					SearchableTag parent = parents.get(i);
					addTagBar(parent.getChildren(), selectedChild);
				}

				tagBars.validate();
			}
		});
	}

	/**
	 * Add a tags bar (do not remove possible previous ones).
	 * <p>
	 * Will always add an "empty" (NULL) tag as first option.
	 * 
	 * @param tags
	 *            the tags to display
	 * @param selected
	 *            the selected tag if any, or NULL for none
	 */
	private void addTagBar(List<SearchableTag> tags,
			final SearchableTag selected) {
		tags.add(0, null);

		final int comboIndex = combos.size();

		final JComboBox combo = new JComboBox(
				tags.toArray(new SearchableTag[] {}));
		combo.setSelectedItem(selected);

		final ListCellRenderer basic = combo.getRenderer();

		combo.setRenderer(new ListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list,
					Object value, int index, boolean isSelected,
					boolean cellHasFocus) {

				Object displayValue = value;
				if (value instanceof SearchableTag) {
					displayValue = ((SearchableTag) value).getName();
				} else {
					displayValue = "Select a tag...";
					cellHasFocus = false;
					isSelected = false;
				}

				Component rep = basic.getListCellRendererComponent(list,
						displayValue, index, isSelected, cellHasFocus);

				if (value == null) {
					rep.setForeground(Color.GRAY);
				}

				return rep;
			}
		});

		combo.addActionListener(createComboTagAction(comboIndex));

		combos.add(combo);
		tagBars.add(combo);
	}

	/**
	 * The action to do on {@link JComboBox} selection.
	 * <p>
	 * The content of the action is:
	 * <ul>
	 * <li>Remove all tags bar below this one</li>
	 * <li>Load the subtags if any in anew tags bar</li>
	 * <li>Load the related stories if the tag was a leaf tag and notify the
	 * {@link Waitable} (via {@link Waitable#fireEvent()})</li>
	 * </ul>
	 * 
	 * @param comboIndex
	 *            the index of the related {@link JComboBox}
	 * 
	 * @return the action
	 */
	private ActionListener createComboTagAction(final int comboIndex) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				List<JComboBox> combos = GuiReaderSearchByTagPanel.this.combos;
				if (combos == null || comboIndex < 0
						|| comboIndex >= combos.size()) {
					return;
				}

				// Tag can be NULL
				final SearchableTag tag = (SearchableTag) combos
						.get(comboIndex).getSelectedItem();

				while (comboIndex + 1 < combos.size()) {
					JComboBox combo = combos.remove(comboIndex + 1);
					tagBars.remove(combo);
				}

				new Thread(new Runnable() {
					@Override
					public void run() {
						waitable.setWaiting(true);
						try {
							final List<SearchableTag> children = getChildrenForTag(tag);
							if (children != null) {
								GuiReaderSearchFrame.inUi(new Runnable() {
									@Override
									public void run() {
										addTagBar(children, tag);
									}
								});
							}

							if (tag != null && tag.isLeaf()) {
								storyItem = 0;
								try {
									searchable.fillTag(tag);
									page = 1;
									stories = searchable.search(tag, 1);
									maxPage = searchable.searchPages(tag);
								} catch (IOException e) {
									GuiReaderSearchFrame.error(e);
									page = 0;
									maxPage = -1;
									stories = new ArrayList<MetaData>();
								}

								waitable.fireEvent();
							}
						} finally {
							waitable.setWaiting(false);
						}
					}
				}).start();
			}
		};
	}

	/**
	 * Get the children of the given tag (or the base tags if the given tag is
	 * NULL).
	 * <p>
	 * This action will "fill" ({@link BasicSearchable#fillTag(SearchableTag)})
	 * the given tag if needed first.
	 * <p>
	 * This operation can be long and should be run outside the UI thread.
	 * 
	 * @param tag
	 *            the tag to search into or NULL for the base tags
	 * @return the children
	 */
	private List<SearchableTag> getChildrenForTag(final SearchableTag tag) {
		List<SearchableTag> children = new ArrayList<SearchableTag>();
		if (tag == null) {
			try {
				List<SearchableTag> baseTags = searchable.getTags();
				children = baseTags;
			} catch (IOException e) {
				GuiReaderSearchFrame.error(e);
			}
		} else {
			try {
				searchable.fillTag(tag);
			} catch (IOException e) {
				GuiReaderSearchFrame.error(e);
			}

			if (!tag.isLeaf()) {
				children = tag.getChildren();
			} else {
				children = null;
			}
		}

		return children;
	}

	/**
	 * Search for the given tag on the currently selected searchable.
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
	public void searchTag(SearchableTag tag, int page, int item) {
		List<MetaData> stories = new ArrayList<MetaData>();
		int storyItem = 0;

		currentTag = tag;
		updateTags(tag);

		int maxPage = -1;
		if (tag != null) {
			try {
				searchable.fillTag(tag);

				if (!tag.isLeaf()) {
					List<SearchableTag> subtags = tag.getChildren();
					if (item > 0 && item <= subtags.size()) {
						SearchableTag subtag = subtags.get(item - 1);
						try {
							tag = subtag;
							searchable.fillTag(tag);
						} catch (IOException e) {
							GuiReaderSearchFrame.error(e);
						}
					} else if (item > 0) {
						GuiReaderSearchFrame.error(String.format(
								"Tag item does not exist: Tag [%s], item %d",
								tag.getFqName(), item));
					}
				}

				maxPage = searchable.searchPages(tag);
				if (page > 0 && tag.isLeaf()) {
					if (maxPage >= 0 && (page <= 0 || page > maxPage)) {
						throw new IndexOutOfBoundsException("Page " + page
								+ " out of " + maxPage);
					}

					try {
						stories = searchable.search(tag, page);
						if (item > 0 && item <= stories.size()) {
							storyItem = item;
						} else if (item > 0) {
							GuiReaderSearchFrame
									.error(String
											.format("Story item does not exist: Tag [%s], item %d",
													tag.getFqName(), item));
						}
					} catch (IOException e) {
						GuiReaderSearchFrame.error(e);
					}
				}
			} catch (IOException e) {
				GuiReaderSearchFrame.error(e);
				maxPage = 0;
			}
		}

		this.stories = stories;
		this.storyItem = storyItem;
		this.page = page;
		this.maxPage = maxPage;
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
		tagBars.setEnabled(b);
		for (JComboBox combo : combos) {
			combo.setEnabled(b);
		}
	}
}
