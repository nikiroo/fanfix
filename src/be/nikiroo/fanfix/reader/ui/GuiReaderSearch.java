package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;

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

	private JPanel tagBars;

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

		updateTags(null);
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
		tagBars = new JPanel();
		tagBars.setLayout(new BoxLayout(tagBars, BoxLayout.Y_AXIS));
		byTag.add(tagBars, BorderLayout.NORTH);

		return byTag;
	}

	private void updateSupportType(SupportType supportType) {
		if (supportType != this.supportType) {
			this.supportType = supportType;
			comboSupportTypes.setSelectedItem(supportType);
			books.clear();
			updateTags(null);
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

	// can be NULL, for base tags
	private void updateTags(final SearchableTag tag) {
		final List<SearchableTag> parents = new ArrayList<SearchableTag>();
		SearchableTag parent = (tag == null) ? null : tag;
		while (parent != null) {
			parents.add(parent);
			parent = parent.getParent();
		}

		inUi(new Runnable() {
			@Override
			public void run() {
				tagBars.invalidate();
				tagBars.removeAll();

				// TODO: Slow UI
				// TODO: select the right one
				try {
					addTagBar(BasicSearchable.getSearchable(supportType)
							.getTags(), tag);
				} catch (IOException e) {
					error(e);
				}

				for (int i = parents.size() - 1; i >= 0; i--) {
					SearchableTag parent = parents.get(i);
					addTagBar(parent.getChildren(), parent);
				}

				tagBars.validate();
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

	private void addTagBar(List<SearchableTag> tags,
			final SearchableTag selected) {
		tags.add(0, null);

		final JComboBox<SearchableTag> combo = new JComboBox<SearchableTag>(
				tags.toArray(new SearchableTag[] {}));
		combo.setSelectedItem(selected);

		// We want to pass it a String
		@SuppressWarnings({ "rawtypes" })
		final ListCellRenderer basic = combo.getRenderer();

		combo.setRenderer(new ListCellRenderer<SearchableTag>() {
			@Override
			public Component getListCellRendererComponent(
					JList<? extends SearchableTag> list, SearchableTag value,
					int index, boolean isSelected, boolean cellHasFocus) {

				Object displayValue = value;
				if (value == null) {
					displayValue = "Select a tag...";
					cellHasFocus = false;
					isSelected = false;
				} else {
					displayValue = value.getName();
				}

				// We willingly pass a String here
				@SuppressWarnings("unchecked")
				Component rep = basic.getListCellRendererComponent(list,
						displayValue, index, isSelected, cellHasFocus);

				if (value == null) {
					rep.setForeground(Color.GRAY);
				}

				return rep;
			}
		});

		combo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final SearchableTag tag = (SearchableTag) combo
						.getSelectedItem();
				if (tag != null) {
					addTagBar(tag, new Runnable() {
						@Override
						public void run() {
							// TODO: stories if needed
							setWaitingScreen(false);
						}
					});
				}
			}
		});

		tagBars.add(combo);
	}

	// async, add children of tag, NULL = base tags
	private void addTagBar(final SearchableTag tag, final Runnable inUi) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				BasicSearchable searchable = BasicSearchable
						.getSearchable(supportType);

				List<SearchableTag> children = new ArrayList<SearchableTag>();
				if (tag == null) {
					try {
						List<SearchableTag> baseTags = searchable.getTags();
						children = baseTags;
					} catch (IOException e) {
						error(e);
					}
				} else {
					try {
						searchable.fillTag(tag);
					} catch (IOException e) {
						error(e);
					}

					if (!tag.isLeaf()) {
						children = tag.getChildren();
					} else {
						children = null;
						// TODO: stories
					}
				}

				final List<SearchableTag> fchildren = children;
				inUi(new Runnable() {
					@Override
					public void run() {
						if (fchildren != null) {
							addTagBar(fchildren, tag);
						}

						if (inUi != null) {
							inUi.run();
						}
					}
				});
			}
		}).start();
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

				int maxPage = -1;
				try {
					maxPage = search.searchPages(keywords);
				} catch (IOException e) {
					error(e);
				}

				if (page <= 0) {
					updateBooks(new ArrayList<GuiReaderBookInfo>());
					updatePages(0, maxPage);
				} else {
					List<MetaData> results;
					try {
						results = search.search(keywords, page);
					} catch (IOException e) {
						error(e);
						results = new ArrayList<MetaData>();
					}

					search(results, page, maxPage, item);

					// ! 1-based index !
					if (item > 0 && item <= books.getBooksCount()) {
						// TODO: "click" on item ITEM
					}
				}

				setWaitingScreen(false);
			}
		}).start();
	}

	// tag: must be filled (or NULL for base tags)
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
						error(e);
					}

					updatePages(page, maxPage);

					if (page > 0) {
						List<MetaData> metas = new ArrayList<MetaData>();

						if (tag.isLeaf()) {
							try {
								metas = search.search(tag, page);
							} catch (IOException e) {
								error(e);
							}
						} else {
							List<SearchableTag> subtags = tag.getChildren();
							if (item > 0 && item <= subtags.size()) {
								SearchableTag subtag = subtags.get(item - 1);
								try {
									metas = search.search(subtag, page);
									maxPage = subtag.getPages();
								} catch (IOException e) {
									error(e);
								}
							}
						}

						updatePages(page, maxPage);
						search(metas, page, maxPage, item);
					}
				}

				setWaitingScreen(false);
			}
		}).start();
	}

	// item 0 = no selection, else = default selection
	public void search(final List<MetaData> results, final int page,
			final int maxPage, final int item) {

		updatePages(page, maxPage);

		if (page <= 0) {
			updateBooks(new ArrayList<GuiReaderBookInfo>());
			updatePages(0, maxPage);
		} else {
			List<GuiReaderBookInfo> infos = new ArrayList<GuiReaderBookInfo>();
			for (MetaData meta : results) {
				infos.add(GuiReaderBookInfo.fromMeta(meta));
			}

			updateBooks(infos);

			// ! 1-based index !
			if (item > 0 && item <= books.getBooksCount()) {
				// TODO: "click" on item ITEM
			}
		}
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
				error(e);
			} catch (InvocationTargetException e) {
				error(e);
			}
		}
	}

	private void error(Exception e) {
		Instance.getTraceHandler().error(e);
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
