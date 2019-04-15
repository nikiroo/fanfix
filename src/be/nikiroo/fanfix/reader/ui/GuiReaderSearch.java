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
	private List<SearchableTag> tags;
	private String keywords;
	private int page;
	private int maxPage;

	private JComboBox<SupportType> comboSupportTypes;
	private JTabbedPane searchTabs;

	private boolean seeWordcount;
	private GuiReaderGroup books;

	public GuiReaderSearch(final GuiReader reader) {
		// TODO: i18n
		super("Browse stories");
		setLayout(new BorderLayout());
		setSize(800, 600);

		tags = new ArrayList<SearchableTag>();
		page = 1; // TODO
		maxPage = -1;

		supportTypes = new ArrayList<SupportType>();
		for (SupportType type : SupportType.values()) {
			if (BasicSearchable.getSearchable(type) != null) {
				supportTypes.add(type);
			}
		}
		supportType = supportTypes.isEmpty() ? null : supportTypes.get(0);

		JPanel top = new JPanel(new BorderLayout());
		comboSupportTypes = new JComboBox<SupportType>(
				supportTypes.toArray(new SupportType[] {}));
		comboSupportTypes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setSupportType((SupportType) comboSupportTypes
						.getSelectedItem());
			}
		});
		top.add(comboSupportTypes, BorderLayout.NORTH);

		// TODO: i18n
		searchTabs = new JTabbedPane();
		searchTabs.addTab("By name", createByNameSearchPanel());
		searchTabs.addTab("By tags", createByTagSearchPanel());

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

	public void setSupportType(SupportType supportType) {
		this.supportType = supportType;
		comboSupportTypes.setSelectedItem(supportType);
		// TODO: reset all
	}

	private JPanel createByNameSearchPanel() {
		JPanel byName = new JPanel(new BorderLayout());

		final JTextField keywordsField = new JTextField();
		byName.add(keywordsField, BorderLayout.CENTER);

		// TODO: i18n
		JButton submit = new JButton("Search");
		byName.add(submit, BorderLayout.EAST);

		// TODO: ENTER -> search

		submit.addActionListener(new ActionListener() {
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

	// item 0 = no selction, else = default selection
	public void search(final SupportType searchOn, final String keywords,
			final int page, final int item) {
		setSupportType(searchOn);
		this.keywords = keywords;
		this.page = page;

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
			}
		}).start();
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

	private void updateBooks(final List<GuiReaderBookInfo> infos) {
		inUi(new Runnable() {
			@Override
			public void run() {
				books.refreshBooks(infos, seeWordcount);
			}
		});
	}

	private void searchTag(SupportType searchOn, int page, int item,
			boolean sync, Integer... tags) throws IOException {

		BasicSearchable search = BasicSearchable.getSearchable(searchOn);
		SearchableTag stag = search.getTag(tags);

		if (stag == null) {
			// TODO i18n
			System.out.println("Known tags: ");
			int i = 1;
			for (SearchableTag s : search.getTags()) {
				System.out.println(String.format("%d: %s", i, s.getName()));
				i++;
			}
		} else {
			if (page <= 0) {
				if (stag.isLeaf()) {
					search.search(stag, 1);
					System.out.println(stag.getPages());
				} else {
					System.out.println(stag.getCount());
				}
			} else {
				List<MetaData> metas = null;
				List<SearchableTag> subtags = null;
				int count;

				if (stag.isLeaf()) {
					metas = search.search(stag, page);
					count = metas.size();
				} else {
					subtags = stag.getChildren();
					count = subtags.size();
				}

				if (item > 0) {
					if (item <= count) {
						if (metas != null) {
							MetaData meta = metas.get(item - 1);
							// displayStory(meta);
						} else {
							SearchableTag subtag = subtags.get(item - 1);
							// displayTag(subtag);
						}
					} else {
						System.out.println("Invalid item: only " + count
								+ " items found");
					}
				} else {
					if (metas != null) {
						// TODO i18n
						System.out.println(String.format("Content of %s: ",
								stag.getFqName()));
						// displayStories(metas);
					} else {
						// TODO i18n
						System.out.println(String.format("Subtags of %s: ",
								stag.getFqName()));
						// displayTags(subtags);
					}
				}
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
	public void inUi(final Runnable run) {
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
}
