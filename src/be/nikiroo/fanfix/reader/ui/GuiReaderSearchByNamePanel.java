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

	public GuiReaderSearchByNamePanel(final Runnable fireEvent) {
		super(new BorderLayout());

		keywordsField = new JTextField();
		add(keywordsField, BorderLayout.CENTER);

		submitKeywords = new JButton("Search");
		add(submitKeywords, BorderLayout.EAST);

		keywordsField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					search(keywordsField.getText(), 1, 0);
				} else {
					super.keyReleased(e);
				}
			}
		});

		submitKeywords.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						search(keywordsField.getText(), 1, 0);
						fireEvent.run();
					}
				}).start();
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
		page = 1;
		maxPage = -1;
		storyItem = 0;
		stories = new ArrayList<MetaData>();
		updateKeywords("");
	}

	public int getPage() {
		return page;
	}

	public int getMaxPage() {
		return maxPage;
	}

	public String getCurrentKeywords() {
		return keywordsField.getText();
	}

	public List<MetaData> getStories() {
		return stories;
	}

	// selected item or 0 if none ! one-based !
	public int getStoryItem() {
		return storyItem;
	}

	// cannot be NULL
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

	// item 0 = no selection, else = default selection
	// throw if page > max
	public void search(String keywords, int page, int item) {
		List<MetaData> stories = new ArrayList<MetaData>();
		int storyItem = 0;

		updateKeywords(keywords);

		int maxPage = -1;
		try {
			maxPage = searchable.searchPages(keywords);
		} catch (IOException e) {
			GuiReaderSearchFrame.error(e);
		}

		if (page > 0) {
			if (maxPage >= 0 && (page <= 0 || page > maxPage)) {
				throw new IndexOutOfBoundsException("Page " + page + " out of "
						+ maxPage);
			}

			try {
				stories = searchable.search(keywords, page);
			} catch (IOException e) {
				GuiReaderSearchFrame.error(e);
				stories = new ArrayList<MetaData>();
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
