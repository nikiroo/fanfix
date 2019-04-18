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
	private GuiReaderSearchByPanel searchPanel;

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
				setWaiting(true);
				updateSupportType(
						(SupportType) comboSupportTypes.getSelectedItem(),
						new Runnable() {
							@Override
							public void run() {
								setWaiting(false);
							}
						});
			}
		});
		JPanel searchSites = new JPanel(new BorderLayout());
		searchSites.add(comboSupportTypes, BorderLayout.CENTER);
		searchSites.add(new JLabel(" " + "Website : "), BorderLayout.WEST);

		searchPanel = new GuiReaderSearchByPanel(supportType,
				new GuiReaderSearchByPanel.Waitable() {
					@Override
					public void setWaiting(boolean waiting) {
						GuiReaderSearchFrame.this.setWaiting(waiting);
					}
				});

		searchPanel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updatePages(searchPanel.getPage(), searchPanel.getMaxPage());
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
	}

	private void updateSupportType(final SupportType supportType,
			final Runnable inUi) {
		this.supportType = supportType;
		comboSupportTypes.setSelectedItem(supportType);
		books.clear();

		new Thread(new Runnable() {
			@Override
			public void run() {
				searchPanel.setSupportType(supportType);
				inUi(inUi);
			}
		}).start();
	}

	private void updatePages(final int page, final int maxPage) {
		inUi(new Runnable() {
			@Override
			public void run() {
				GuiReaderSearchFrame.this.page = page;
				GuiReaderSearchFrame.this.maxPage = maxPage;

				// TODO: gui
				System.out.println("page: " + page);
				System.out.println("max page: " + maxPage);
			}
		});
	}

	private void updateBooks(final List<GuiReaderBookInfo> infos) {
		setWaiting(true);
		inUi(new Runnable() {
			@Override
			public void run() {
				books.refreshBooks(infos, seeWordcount);
				setWaiting(false);
			}
		});
	}

	// item 0 = no selection, else = default selection
	public void search(final SupportType searchOn, final String keywords,
			final int page, final int item) {
		setWaiting(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				searchPanel.setSupportType(searchOn);
				searchPanel.search(keywords, page, item);
				inUi(new Runnable() {
					@Override
					public void run() {
						setWaiting(false);
					}
				});
			}
		}).start();
	}

	// tag: null = base tags
	public void searchTag(final SupportType searchOn, final int page,
			final int item, final SearchableTag tag) {
		setWaiting(true);
		new Thread(new Runnable() {
			@Override
			public void run() {
				searchPanel.setSupportType(searchOn);
				searchPanel.searchTag(tag, page, item);
				inUi(new Runnable() {
					@Override
					public void run() {
						setWaiting(false);
					}
				});
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
		books.setEnabled(b);
		searchPanel.setEnabled(b);
	}

	private void setWaiting(final boolean waiting) {
		inUi(new Runnable() {
			@Override
			public void run() {
				GuiReaderSearchFrame.this.setEnabled(!waiting);
			}
		});
	}
}
