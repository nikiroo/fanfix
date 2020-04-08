package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.browser.AuthorTab;
import be.nikiroo.fanfix_swing.gui.browser.BasicTab;
import be.nikiroo.fanfix_swing.gui.browser.SourceTab;
import be.nikiroo.fanfix_swing.gui.browser.TagsTab;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;

/**
 * Panel dedicated to browse the stories through different means: by authors, by
 * tags or by sources.
 * 
 * @author niki
 */
public class BrowserPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * The {@link ActionEvent} you receive from
	 * {@link BrowserPanel#addActionListener(ActionListener)} can return this as a
	 * command (see {@link ActionEvent#getActionCommand()}) if they were created in
	 * the scope of a source.
	 */
	static public final String SOURCE_SELECTION = "source_selection";
	/**
	 * The {@link ActionEvent} you receive from
	 * {@link BrowserPanel#addActionListener(ActionListener)} can return this as a
	 * command (see {@link ActionEvent#getActionCommand()}) if they were created in
	 * the scope of an author.
	 */
	static public final String AUTHOR_SELECTION = "author_selection";
	/**
	 * The {@link ActionEvent} you receive from
	 * {@link BrowserPanel#addActionListener(ActionListener)} can return this as a
	 * command (see {@link ActionEvent#getActionCommand()}) if they were created in
	 * the scope of a tag.
	 */
	static public final String TAGS_SELECTION = "tags_selection";

	private JTabbedPane tabs;
	private SourceTab sourceTab;
	private AuthorTab authorTab;
	private TagsTab tagsTab;

	private boolean keepSelection;

	/**
	 * Create a nesw {@link BrowserPanel}.
	 */
	public BrowserPanel() {
		this.setPreferredSize(new Dimension(200, 800));

		this.setLayout(new BorderLayout());
		tabs = new JTabbedPane();

		int index = 0;
		tabs.add(sourceTab = new SourceTab(index++, SOURCE_SELECTION));
		tabs.add(authorTab = new AuthorTab(index++, AUTHOR_SELECTION));
		tabs.add(tagsTab = new TagsTab(index++, TAGS_SELECTION));

		setText(tabs, sourceTab, "Sources", "Tooltip for Sources");
		setText(tabs, authorTab, "Authors", "Tooltip for Authors");
		setText(tabs, tagsTab, "Tags", "Tooltip for Tags");

		JPanel options = new JPanel();
		options.setLayout(new BorderLayout());

		final JButton keep = new JButton("Keep selection");
		UiHelper.setButtonPressed(keep, keepSelection);
		keep.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				keepSelection = !keepSelection;
				UiHelper.setButtonPressed(keep, keepSelection);
				keep.setSelected(keepSelection);
				if (!keepSelection) {
					unselect();
				}
			}
		});

		options.add(keep, BorderLayout.CENTER);

		add(tabs, BorderLayout.CENTER);
		add(options, BorderLayout.SOUTH);

		tabs.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (!keepSelection) {
					unselect();
				}
			}
		});
	}

	@SuppressWarnings("rawtypes")
	private void unselect() {
		for (int i = 0; i < tabs.getTabCount(); i++) {
			if (i == tabs.getSelectedIndex())
				continue;

			BasicTab tab = (BasicTab) tabs.getComponent(i);
			tab.unselect();
		}
	}

	private void setText(JTabbedPane tabs, @SuppressWarnings("rawtypes") BasicTab tab, String name, String tooltip) {
		tab.setBaseTitle(name);
		tabs.setTitleAt(tab.getIndex(), tab.getTitle());
		tabs.setToolTipTextAt(tab.getIndex(), tooltip);
		listenTitleChange(tabs, tab);
	}

	private void listenTitleChange(final JTabbedPane tabs, @SuppressWarnings("rawtypes") final BasicTab tab) {
		tab.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabs.setTitleAt(tab.getIndex(), tab.getTitle());
			}
		});
	}

	/**
	 * Get the {@link BookInfo} to highlight, even if more than one are selected.
	 * <p>
	 * Return NULL when nothing is selected.
	 * 
	 * @return the {@link BookInfo} to highlight, can be NULL
	 */
	public BookInfo getHighlight() {
		BasicLibrary lib = Instance.getInstance().getLibrary();
		List<String> sel = sourceTab.getSelectedElements();
		if (!sel.isEmpty()) {
			if (tabs.getSelectedComponent() == sourceTab) {
				return BookInfo.fromSource(lib, sel.get(0));
			} else if (tabs.getSelectedComponent() == authorTab) {
				return BookInfo.fromAuthor(lib, sel.get(0));
			} else if (tabs.getSelectedComponent() == tagsTab) {
				return BookInfo.fromTag(lib, sel.get(0));
			}
		}

		return null;
	}

	/**
	 * The currently selected sources, or an empty list.
	 * 
	 * @return the sources (cannot be NULL)
	 */
	public List<String> getSelectedSources() {
		return sourceTab.getSelectedElements();
	}

	/**
	 * The currently selected authors, or an empty list.
	 * 
	 * @return the sources (cannot be NULL)
	 */
	public List<String> getSelectedAuthors() {
		return authorTab.getSelectedElements();
	}

	/**
	 * The currently selected tags, or an empty list.
	 * 
	 * @return the sources (cannot be NULL)
	 */
	public List<String> getSelectedTags() {
		return tagsTab.getSelectedElements();
	}

	/**
	 * Reload all the data from the 3 tabs.
	 */
	public void reloadData() {
		sourceTab.reloadData();
		authorTab.reloadData();
		tagsTab.reloadData();
	}

	/**
	 * Adds the specified action listener to receive action events from this
	 * {@link SearchBar}.
	 *
	 * @param listener the action listener to be added
	 */
	public synchronized void addActionListener(ActionListener listener) {
		sourceTab.addActionListener(listener);
		authorTab.addActionListener(listener);
		tagsTab.addActionListener(listener);
	}

	/**
	 * Removes the specified action listener so that it no longer receives action
	 * events from this {@link SearchBar}.
	 *
	 * @param listener the action listener to be removed
	 */
	public synchronized void removeActionListener(ActionListener listener) {
		sourceTab.removeActionListener(listener);
		authorTab.removeActionListener(listener);
		tagsTab.removeActionListener(listener);
	}
}
