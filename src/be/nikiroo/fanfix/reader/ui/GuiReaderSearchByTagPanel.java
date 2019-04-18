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
import be.nikiroo.fanfix.searchable.BasicSearchable;
import be.nikiroo.fanfix.searchable.SearchableTag;

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
	private Runnable fireEvent;

	private SearchableTag currentTag;
	private JPanel tagBars;
	private List<JComboBox> combos;

	private int page;
	private int maxPage;
	private List<MetaData> stories = new ArrayList<MetaData>();
	private int storyItem;

	public GuiReaderSearchByTagPanel(Runnable fireEvent) {
		setLayout(new BorderLayout());

		this.fireEvent = fireEvent;
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

	public int getPage() {
		return page;
	}

	public int getMaxPage() {
		return maxPage;
	}

	public SearchableTag getCurrentTag() {
		return currentTag;
	}

	public List<MetaData> getStories() {
		return stories;
	}

	// selected item or 0 if none ! one-based !
	public int getStoryItem() {
		return storyItem;
	}

	// update and reset the tagsbar
	// can be NULL, for base tags
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

	// must be quick and no thread change
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

							fireEvent.run();
						}
					}
				}).start();
			}
		};
	}

	// sync, add children of tag, NULL = base tags
	// return children of the tag or base tags or NULL
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

	// slow
	// tag: null = base tags
	// throw if page > max, but only if stories
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
