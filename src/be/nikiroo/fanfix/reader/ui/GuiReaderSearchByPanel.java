package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
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

	private int actionEventId = ActionEvent.ACTION_FIRST;

	private Waitable waitable;

	private boolean searchByTags;

	private JTabbedPane searchTabs;
	private GuiReaderSearchByNamePanel byName;
	private GuiReaderSearchByTagPanel byTag;

	private List<ActionListener> actions = new ArrayList<ActionListener>();

	public interface Waitable {
		public void setWaiting(boolean waiting);
	}

	// will throw illegalArgEx if bad support type, NULL allowed
	public GuiReaderSearchByPanel(final SupportType supportType,
			Waitable waitable) {
		setLayout(new BorderLayout());

		this.waitable = waitable;
		searchByTags = false;

		Runnable fireEvent = new Runnable() {
			@Override
			public void run() {
				fireAction();
			}
		};

		byName = new GuiReaderSearchByNamePanel(fireEvent);
		byTag = new GuiReaderSearchByTagPanel(fireEvent);

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
		setSupportType(supportType);
	}

	public void setSupportType(SupportType supportType) {
		BasicSearchable searchable = BasicSearchable.getSearchable(supportType);
		if (searchable == null && supportType != null) {
			throw new java.lang.IllegalArgumentException(
					"Unupported support type: " + supportType);
		}

		byName.setSearchable(searchable);
		byTag.setSearchable(searchable);
	}

	public int getPage() {
		if (!searchByTags) {
			return byName.getPage();
		}

		return byTag.getPage();
	}

	public int getMaxPage() {
		if (!searchByTags) {
			return byName.getMaxPage();
		}

		return byTag.getMaxPage();
	}

	// throw outOfBounds if needed
	public void setPage(int page) {
		if (searchByTags) {
			searchTag(byTag.getCurrentTag(), page, 0);
		} else {
			search(byName.getCurrentKeywords(), page, 0);
		}
	}

	// actions will be fired in UIthread
	public void addActionListener(ActionListener action) {
		actions.add(action);
	}

	public boolean removeActionListener(ActionListener action) {
		return actions.remove(action);
	}

	public List<MetaData> getStories() {
		if (!searchByTags) {
			return byName.getStories();
		}

		return byTag.getStories();
	}

	// selected item or 0 if none ! one-based !
	public int getStoryItem() {
		if (!searchByTags) {
			return byName.getStoryItem();
		}

		return byTag.getStoryItem();
	}

	private void fireAction() {
		GuiReaderSearchFrame.inUi(new Runnable() {
			@Override
			public void run() {
				ActionEvent ae = new ActionEvent(GuiReaderSearchByPanel.this,
						actionEventId, "stories found");

				actionEventId++;
				if (actionEventId > ActionEvent.ACTION_LAST) {
					actionEventId = ActionEvent.ACTION_FIRST;
				}

				for (ActionListener action : actions) {
					try {
						action.actionPerformed(ae);
					} catch (Exception e) {
						GuiReaderSearchFrame.error(e);
					}
				}
			}
		});
	}

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

	// slow, start in UI mode
	public void search(final String keywords, final int page, final int item) {
		waitable.setWaiting(true);
		updateSearchBy(false);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					byName.search(keywords, page, item);
					fireAction();
				} finally {
					waitable.setWaiting(false);
				}
			}
		}).start();
	}

	// slow, start in UI mode
	// tag: null = base tags
	public void searchTag(final SearchableTag tag, final int page,
			final int item) {
		waitable.setWaiting(true);
		updateSearchBy(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					byTag.searchTag(tag, page, item);
					fireAction();
				} finally {
					waitable.setWaiting(false);
				}
			}
		}).start();
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
