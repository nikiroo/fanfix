package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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
// JCombobox<E> not 1.6 compatible
@SuppressWarnings({ "unchecked", "rawtypes" })
public class GuiReaderSearchFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private List<SupportType> supportTypes;
	private SupportType supportType;
	private int page;
	private int maxPage;

	private JComboBox comboSupportTypes;
	private GuiReaderSearchByNamePanel searchPanel;

	private boolean seeWordcount;
	private GuiReaderGroup books;

	public GuiReaderSearchFrame(final GuiReader reader) {
		super("Browse stories");
		setLayout(new BorderLayout());
		setSize(800, 600);

		page = 1;
		maxPage = -1;

		supportTypes = new ArrayList<SupportType>();
		for (SupportType type : SupportType.values()) {
			if (BasicSearchable.getSearchable(type) != null) {
				supportTypes.add(type);
			}
		}
		supportType = supportTypes.isEmpty() ? null : supportTypes.get(0);

		comboSupportTypes = new JComboBox(
				supportTypes.toArray(new SupportType[] {}));
		comboSupportTypes.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setWaitingScreen(true);
				updateSupportType(
						(SupportType) comboSupportTypes.getSelectedItem(),
						new Runnable() {
							@Override
							public void run() {
								setWaitingScreen(false);
							}
						});
			}
		});
		JPanel searchSites = new JPanel(new BorderLayout());
		searchSites.add(comboSupportTypes, BorderLayout.CENTER);
		searchSites.add(new JLabel(" " + "Website : "), BorderLayout.WEST);

		searchPanel = new GuiReaderSearchByNamePanel(supportType,
				new Runnable() {
					@Override
					public void run() {
						setWaitingScreen(false);
					}
				});

		searchPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateMaxPage(searchPanel.getMaxPage());
				List<GuiReaderBookInfo> infos = new ArrayList<GuiReaderBookInfo>();
				for (MetaData meta : searchPanel.getStories()) {
					infos.add(GuiReaderBookInfo.fromMeta(meta));
				}

				updateBooks(infos);

				// ! 1-based index !
				int item = searchPanel.getStoryItem();
				if (item > 0 && item <= books.getBooksCount()) {
					// TODO: "click" on item ITEM
				}
			}
		});

		JPanel top = new JPanel(new BorderLayout());
		top.add(searchSites, BorderLayout.NORTH);
		top.add(searchPanel, BorderLayout.CENTER);

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

		setWaitingScreen(true);
	}

	private void updateSupportType(SupportType supportType, Runnable inUi) {
		if (supportType != this.supportType) {
			this.supportType = supportType;
			comboSupportTypes.setSelectedItem(supportType);
			books.clear();
			searchPanel.setSupportType(supportType, inUi);
		}
	}

	private void updatePage(final int page) {
		inUi(new Runnable() {
			@Override
			public void run() {
				GuiReaderSearchFrame.this.page = page;

				// TODO: gui
				System.out.println("page: " + page);
			}
		});
	}

	private void updateMaxPage(final int maxPage) {
		inUi(new Runnable() {
			@Override
			public void run() {
				GuiReaderSearchFrame.this.maxPage = maxPage;

				// TODO: gui
				System.out.println("max page: " + maxPage);
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

		searchPanel.setSupportType(searchOn, new Runnable() {
			@Override
			public void run() {
				if (keywords != null) {
					setWaitingScreen(true);
					searchPanel.search(keywords, page, item, new Runnable() {
						@Override
						public void run() {
							updateMaxPage(searchPanel.getMaxPage());
							updatePage(page);
							setWaitingScreen(false);
						}
					});
				}
			}
		});
	}

	// tag: null = base tags
	public void searchTag(final SupportType searchOn, final int page,
			final int item, final SearchableTag tag) {

		setWaitingScreen(true);

		searchPanel.setSupportType(searchOn, new Runnable() {
			@Override
			public void run() {
				setWaitingScreen(true);
				searchPanel.searchTag(tag, page, item, new Runnable() {
					@Override
					public void run() {
						updateMaxPage(searchPanel.getMaxPage());
						updatePage(page);
						setWaitingScreen(false);
					}
				});
			}
		});
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
	static void inUi(final Runnable run) {
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

	static void error(Exception e) {
		Instance.getTraceHandler().error(e);
	}

	static void error(String e) {
		Instance.getTraceHandler().error(e);
	}

	private void setWaitingScreen(final boolean waiting) {
		inUi(new Runnable() {
			@Override
			public void run() {
				GuiReaderSearchFrame.this.setEnabled(!waiting);
				books.setEnabled(!waiting);
				searchPanel.setEnabled(!waiting);
			}
		});
	}
}
