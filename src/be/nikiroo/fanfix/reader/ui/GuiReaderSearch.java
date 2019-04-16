package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.ui.GuiReaderBook.BookActionListener;
import be.nikiroo.fanfix.searchable.BasicSearchable;
import be.nikiroo.fanfix.searchable.SearchableTag;
import be.nikiroo.fanfix.supported.SupportType;

/**
 * This frame will allow you to search through the supported websites for new
 * stories/comics.
 * 
 * @author niki
 */
public class GuiReaderSearch extends JFrame {
	private static final long serialVersionUID = 1L;

	private List<SupportType> supportTypes;
	private SupportType supportType;
	private boolean searchByTags;
	private List<SearchableTag> tags;
	private String keywords;
	private int page;
	private int maxPage;

	private JComboBox<SupportType> comboSupportTypes;
	private JTabbedPane searchTabs;
	private JTextField keywordsField;
	private JButton submitKeywords;

	private boolean seeWordcount;
	private GuiReaderGroup books;

	public GuiReaderSearch(final GuiReader reader) {
		super("Browse stories");
		setLayout(new BorderLayout());
		setSize(800, 600);

		tags = new ArrayList<SearchableTag>();
		page = 1; // TODO
		maxPage = -1;
		searchByTags = false;

		supportTypes = new ArrayList<SupportType>();
		for (SupportType type : SupportType.values()) {
			if (BasicSearchable.getSearchable(type) != null) {
				supportTypes.add(type);
			}
		}
		supportType = supportTypes.isEmpty() ? null : supportTypes.get(0);

		comboSupportTypes = new JComboBox<SupportType>(
				supportTypes.toArray(new SupportType[] {}));
		comboSupportTypes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateSupportType((SupportType) comboSupportTypes
						.getSelectedItem());
			}
		});
		JPanel searchSites = new JPanel(new BorderLayout());
		searchSites.add(comboSupportTypes, BorderLayout.CENTER);
		searchSites.add(new JLabel(" " + "Website : "), BorderLayout.WEST);

		searchTabs = new JTabbedPane();
		searchTabs.addTab("By name", createByNameSearchPanel());
		searchTabs.addTab("By tags", createByTagSearchPanel());

		JPanel top = new JPanel(new BorderLayout());
		top.add(searchSites, BorderLayout.NORTH);
		top.add(searchTabs, BorderLayout.CENTER);

		add(top, BorderLayout.NORTH);

		books = new GuiReaderGroup(reader, null, null);
		books.setActionListener(new BookActionListener() {
			@Override
			public void select(GuiReaderBook book) {
			}

			@Override
			public void popupRequested(GuiReaderBook book, Component target,
					int x, int y) {
			}

			@Override
			public void action(GuiReaderBook book) {
				new GuiReaderSearchAction(reader.getLibrary(), book.getInfo())
						.setVisible(true);
			}
		});
		JScrollPane scroll = new JScrollPane(books);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		add(scroll, BorderLayout.CENTER);
	}

	private JPanel createByNameSearchPanel() {
		JPanel byName = new JPanel(new BorderLayout());

		keywordsField = new JTextField();
		byName.add(keywordsField, BorderLayout.CENTER);

		submitKeywords = new JButton("Search");
		byName.add(submitKeywords, BorderLayout.EAST);

		// TODO: ENTER -> search

		submitKeywords.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				search(supportType, keywordsField.getText(), page, 0);
			}
		});

		return byName;
	}

	private JPanel createByTagSearchPanel() {
		JPanel byTag = new JPanel();
		JPanel searchBars = new JPanel();
		add(searchBars, BorderLayout.NORTH);

		return byTag;
	}

	private void updateSupportType(SupportType supportType) {
		if (supportType != this.supportType) {
			this.supportType = supportType;
			comboSupportTypes.setSelectedItem(supportType);
			books.clear();
			// TODO: reset all tags
		}
	}

	private void updateSearchBy(final boolean byTag) {
		if (byTag != this.searchByTags) {
			inUi(new Runnable() {
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
	}

	private void updatePages(final int page, final Integer maxPage) {
		inUi(new Runnable() {
			@Override
			public void run() {
				GuiReaderSearch.this.page = page;
				GuiReaderSearch.this.maxPage = maxPage;
				// TODO: gui
				System.out.println("page: " + page);
				System.out.println("max page: " + maxPage);
			}
		});
	}

	// cannot be NULL
	private void updateKeywords(final String keywords) {
		if (!keywords.equals(this.keywords)) {
			inUi(new Runnable() {
				@Override
				public void run() {
					GuiReaderSearch.this.keywords = keywords;
					keywordsField.setText(keywords);
				}
			});
		}
	}

	// can be NULL
	private void updateTags(final SearchableTag tag) {
		inUi(new Runnable() {
			@Override
			public void run() {
				// TODO
			}
		});
	}

	private void updateBooks(final List<GuiReaderBookInfo> infos) {
		setWaitingScreen(true);
		inUi(new Runnable() {
			@Override
			public void run() {
				books.refreshBooks(infos, seeWordcount);
				setWaitingScreen(false);
			}
		});
	}

	// item 0 = no selection, else = default selection
	public void search(final SupportType searchOn, final String keywords,
			final int page, final int item) {

		setWaitingScreen(true);

		updateSupportType(searchOn);
		updateSearchBy(false);
		updateKeywords(keywords);
		updatePages(page, maxPage);

		new Thread(new Runnable() {
			@Override
			public void run() {
				BasicSearchable search = BasicSearchable
						.getSearchable(searchOn);

				if (page <= 0) {
					int maxPage = -1;
					try {
						maxPage = search.searchPages(keywords);
					} catch (IOException e) {
						Instance.getTraceHandler().error(e);
					}
					updateBooks(new ArrayList<GuiReaderBookInfo>());
					updatePages(0, maxPage);
				} else {
					List<GuiReaderBookInfo> infos = new ArrayList<GuiReaderBookInfo>();
					try {
						for (MetaData meta : search.search(keywords, page)) {
							infos.add(GuiReaderBookInfo.fromMeta(meta));
						}
					} catch (IOException e) {
						Instance.getTraceHandler().error(e);
					}

					updateBooks(infos);
					updatePages(page, maxPage);

					// ! 1-based index !
					if (item > 0 && item <= books.getBooksCount()) {
						// TODO: "click" on item ITEM
					}
				}

				setWaitingScreen(false);
			}
		}).start();
	}

	public void searchTag(final SupportType searchOn, final int page,
			final int item, final SearchableTag tag) {

		setWaitingScreen(true);

		updateSupportType(searchOn);
		updateSearchBy(true);
		updateTags(tag);
		updatePages(page, maxPage);

		new Thread(new Runnable() {
			@Override
			public void run() {
				BasicSearchable search = BasicSearchable
						.getSearchable(searchOn);

				if (tag != null) {
					int maxPage = 0;
					try {
						maxPage = search.searchPages(tag);
					} catch (IOException e) {
						Instance.getTraceHandler().error(e);
					}

					updatePages(page, maxPage);

					if (page > 0) {
						List<MetaData> metas = null;
						List<SearchableTag> subtags = null;
						int count;

						if (tag.isLeaf()) {
							try {
								metas = search.search(tag, page);
							} catch (IOException e) {
								metas = new ArrayList<MetaData>();
								Instance.getTraceHandler().error(e);
							}
							count = metas.size();
						} else {
							subtags = tag.getChildren();
							count = subtags.size();
						}

						if (item > 0) {
							if (item <= count) {
								if (metas != null) {
									MetaData meta = metas.get(item - 1);
									// TODO: select story
								} else {
									SearchableTag subtag = subtags
											.get(item - 1);
									// TODO: search on tag
								}
							}
						}
					}
				}
				setWaitingScreen(false);
			}
		}).start();
	}

	/**
	 * Process the given action in the main Swing UI thread.
	 * <p>
	 * The code will make sure the current thread is the main UI thread and, if
	 * not, will switch to it before executing the runnable.
	 * <p>
	 * Synchronous operation.
	 * 
	 * @param run
	 *            the action to run
	 */
	private void inUi(final Runnable run) {
		if (EventQueue.isDispatchThread()) {
			run.run();
		} else {
			try {
				EventQueue.invokeAndWait(run);
			} catch (InterruptedException e) {
				Instance.getTraceHandler().error(e);
			} catch (InvocationTargetException e) {
				Instance.getTraceHandler().error(e);
			}
		}
	}

	private void setWaitingScreen(final boolean waiting) {
		inUi(new Runnable() {
			@Override
			public void run() {
				GuiReaderSearch.this.setEnabled(!waiting);
				books.setEnabled(!waiting);
				submitKeywords.setEnabled(!waiting);
			}
		});
	}
}
