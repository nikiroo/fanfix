package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.ui.GuiReaderSearchByPanel.Waitable;
import be.nikiroo.fanfix.searchable.BasicSearchable;

/**
 * This panel represents a search panel that works for keywords and tags based
 * searches.
 * 
 * @author niki
 */
public class GuiReaderSearchByNamePanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private BasicSearchable searchable;

	private JTextField keywordsField;
	private JButton submitKeywords;

	private int page;
	private int maxPage;
	private List<MetaData> stories = new ArrayList<MetaData>();
	private int storyItem;

	public GuiReaderSearchByNamePanel(final Waitable waitable) {
		super(new BorderLayout());

		keywordsField = new JTextField();
		add(keywordsField, BorderLayout.CENTER);

		submitKeywords = new JButton("Search");
		add(submitKeywords, BorderLayout.EAST);

		// should be done out of UI
		final Runnable go = new Runnable() {
			@Override
			public void run() {
				waitable.setWaiting(true);
				try {
					search(keywordsField.getText(), 1, 0);
					waitable.fireEvent();
				} finally {
					waitable.setWaiting(false);
				}
			}
		};

		keywordsField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					new Thread(go).start();
				} else {
					super.keyReleased(e);
				}
			}
		});

		submitKeywords.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(go).start();
			}
		});

		setSearchable(null);
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
		page = 0;
		maxPage = -1;
		storyItem = 0;
		stories = new ArrayList<MetaData>();
		updateKeywords("");
	}

	/**
	 * The currently displayed page of result for the current search (see the
	 * <tt>page</tt> parameter of
	 * {@link GuiReaderSearchByNamePanel#search(String, int, int)}).
	 * 
	 * @return the currently displayed page of results
	 */
	public int getPage() {
		return page;
	}

	/**
	 * The number of pages of result for the current search (see the
	 * <tt>page</tt> parameter of
	 * {@link GuiReaderSearchByPanel#search(String, int, int)}).
	 * <p>
	 * For an unknown number or when not applicable, -1 is returned.
	 * 
	 * @return the number of pages of results or -1
	 */
	public int getMaxPage() {
		return maxPage;
	}

	/**
	 * Return the keywords used for the current search.
	 * 
	 * @return the keywords
	 */
	public String getCurrentKeywords() {
		return keywordsField.getText();
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
	 * Update the keywords displayed on screen.
	 * 
	 * @param keywords
	 *            the keywords
	 */
	private void updateKeywords(final String keywords) {
		if (!keywords.equals(keywordsField.getText())) {
			GuiReaderSearchFrame.inUi(new Runnable() {
				@Override
				public void run() {
					keywordsField.setText(keywords);
				}
			});
		}
	}

	/**
	 * Search for the given terms on the currently selected searchable.
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
	public void search(String keywords, int page, int item) {
		List<MetaData> stories = new ArrayList<MetaData>();
		int storyItem = 0;

		updateKeywords(keywords);

		int maxPage = -1;
		if (searchable != null) {
			try {
				maxPage = searchable.searchPages(keywords);
			} catch (IOException e) {
				GuiReaderSearchFrame.error(e);
			}
		}

		if (page > 0) {
			if (maxPage >= 0 && (page <= 0 || page > maxPage)) {
				throw new IndexOutOfBoundsException("Page " + page + " out of "
						+ maxPage);
			}

			if (searchable != null) {
				try {
					stories = searchable.search(keywords, page);
				} catch (IOException e) {
					GuiReaderSearchFrame.error(e);
				}
			}

			if (item > 0 && item <= stories.size()) {
				storyItem = item;
			} else if (item > 0) {
				GuiReaderSearchFrame.error(String.format(
						"Story item does not exist: Search [%s], item %d",
						keywords, item));
			}
		}

		this.page = page;
		this.maxPage = maxPage;
		this.stories = stories;
		this.storyItem = storyItem;
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
		keywordsField.setEnabled(b);
		submitKeywords.setEnabled(b);
	}
}
