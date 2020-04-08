package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.fanfix_swing.gui.book.BookBlock;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;
import be.nikiroo.fanfix_swing.gui.book.BookLine;
import be.nikiroo.fanfix_swing.gui.book.BookPopup;
import be.nikiroo.fanfix_swing.gui.utils.ListenerPanel;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;

public class BooksPanel extends ListenerPanel {
	private class ListModel extends DefaultListModel<BookInfo> {
		public void fireElementChanged(BookInfo element) {
			int index = indexOf(element);
			if (index >= 0) {
				fireContentsChanged(element, index, index);
			}
		}
	}

	static public final String INVALIDATE_CACHE = "invalidate_cache";

	private List<BookInfo> bookInfos = new ArrayList<BookInfo>();
	private Map<BookInfo, BookLine> books = new HashMap<BookInfo, BookLine>();
	private boolean seeWordCount;
	private boolean listMode;

	private JList<BookInfo> list;
	private int hoveredIndex = -1;
	private ListModel data = new ListModel();

	private SearchBar searchBar;

	private Queue<BookBlock> updateBookQueue = new LinkedList<BookBlock>();
	private Object updateBookQueueLock = new Object();

	public BooksPanel(boolean listMode) {
		setLayout(new BorderLayout());

		searchBar = new SearchBar();
		add(searchBar, BorderLayout.NORTH);

		searchBar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				filter(searchBar.getText());
			}
		});

		add(UiHelper.scroll(initList(listMode)), BorderLayout.CENTER);

		Thread bookBlocksUpdater = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					BasicLibrary lib = Instance.getInstance().getLibrary();
					while (true) {
						final BookBlock book;
						synchronized (updateBookQueueLock) {
							if (!updateBookQueue.isEmpty()) {
								book = updateBookQueue.remove();
							} else {
								book = null;
								break;
							}
						}

						try {
							final Image coverImage = BookBlock.generateCoverImage(lib, book.getInfo());
							SwingUtilities.invokeLater(new Runnable() {
								@Override
								public void run() {
									try {
										book.setCoverImage(coverImage);
										data.fireElementChanged(book.getInfo());
									} catch (Exception e) {
									}
								}
							});
						} catch (Exception e) {
						}
					}

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					}
				}
			}
		});
		bookBlocksUpdater.setName("BookBlocks visual updater");
		bookBlocksUpdater.setDaemon(true);
		bookBlocksUpdater.start();
	}

	// null or empty -> all sources
	// sources hierarchy supported ("source/" will includes all "source" and
	// "source/*")
	public void load(final List<String> sources, final List<String> authors, final List<String> tags) {
		new SwingWorker<List<BookInfo>, Void>() {
			@Override
			protected List<BookInfo> doInBackground() throws Exception {
				List<BookInfo> bookInfos = new ArrayList<BookInfo>();
				BasicLibrary lib = Instance.getInstance().getLibrary();
				for (MetaData meta : lib.getList(null).filter(sources, authors, tags)) {
					bookInfos.add(BookInfo.fromMeta(lib, meta));
				}

				return bookInfos;
			}

			@Override
			protected void done() {
				try {
					load(get());
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				// TODO: error
			}
		}.execute();
	}

	public void load(List<BookInfo> bookInfos) {
		this.bookInfos.clear();
		this.bookInfos.addAll(bookInfos);
		synchronized (updateBookQueueLock) {
			updateBookQueue.clear();
		}

		filter(searchBar.getText());
	}

	// cannot be NULL
	private void filter(String filter) {
		data.clear();
		for (BookInfo bookInfo : bookInfos) {
			if (filter.isEmpty() || bookInfo.getMainInfo().toLowerCase().contains(filter.toLowerCase())) {
				data.addElement(bookInfo);
			}
		}
		list.repaint();
	}

	/**
	 * The secondary value content: word count or author.
	 * 
	 * @return TRUE to see word counts, FALSE to see authors
	 */
	public boolean isSeeWordCount() {
		return seeWordCount;
	}

	/**
	 * The secondary value content: word count or author.
	 * 
	 * @param seeWordCount TRUE to see word counts, FALSE to see authors
	 */
	public void setSeeWordCount(boolean seeWordCount) {
		if (this.seeWordCount != seeWordCount) {
			if (books != null) {
				for (BookLine book : books.values()) {
					book.setSeeWordCount(seeWordCount);
				}

				list.repaint();
			}
		}
	}

	private JList<BookInfo> initList(boolean listMode) {
		final JList<BookInfo> list = new JList<BookInfo>(data);

		final JPopupMenu popup = new BookPopup(Instance.getInstance().getLibrary(), new BookPopup.Informer() {
			@Override
			public void setCached(BookInfo book, boolean cached) {
				book.setCached(cached);
				fireElementChanged(book);
			}

			public void fireElementChanged(BookInfo book) {
				data.fireElementChanged(book);
			}

			@Override
			public List<BookInfo> getSelected() {
				List<BookInfo> selected = new ArrayList<BookInfo>();
				for (int index : list.getSelectedIndices()) {
					selected.add(data.get(index));
				}

				return selected;
			}

			@Override
			public BookInfo getUniqueSelected() {
				List<BookInfo> selected = getSelected();
				if (selected.size() == 1) {
					return selected.get(0);
				}
				return null;
			}

			@Override
			public void invalidateCache() {
				fireActionPerformed(INVALIDATE_CACHE);
			}
		});

		list.addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent me) {
				if (popup.isShowing())
					return;

				Point p = new Point(me.getX(), me.getY());
				int index = list.locationToIndex(p);
				if (index != hoveredIndex) {
					hoveredIndex = index;
					list.repaint();
				}
			}
		});
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				check(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				check(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				if (popup.isShowing())
					return;

				if (hoveredIndex > -1) {
					hoveredIndex = -1;
					list.repaint();
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				if (e.getClickCount() == 2) {
					int index = list.locationToIndex(e.getPoint());
					list.setSelectedIndex(index);

					final BookInfo book = data.get(index);
					BasicLibrary lib = Instance.getInstance().getLibrary();

					Actions.openExternal(lib, book.getMeta(), BooksPanel.this, new Runnable() {
						@Override
						public void run() {
							book.setCached(true);
							data.fireElementChanged(book);
						}
					});
				}
			}

			private void check(MouseEvent e) {
				if (e.isPopupTrigger()) {
					if (list.getSelectedIndices().length <= 1) {
						list.setSelectedIndex(list.locationToIndex(e.getPoint()));
					}

					popup.show(list, e.getX(), e.getY());
				}
			}
		});

		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		list.setSelectedIndex(0);
		list.setCellRenderer(generateRenderer());
		list.setVisibleRowCount(0);

		this.list = list;
		setListMode(listMode);
		return this.list;
	}

	private ListCellRenderer<BookInfo> generateRenderer() {
		return new ListCellRenderer<BookInfo>() {
			@Override
			public Component getListCellRendererComponent(JList<? extends BookInfo> list, BookInfo value, int index,
					boolean isSelected, boolean cellHasFocus) {
				BookLine book = books.get(value);
				if (book == null) {
					if (listMode) {
						book = new BookLine(value, seeWordCount);
					} else {
						book = new BookBlock(value, seeWordCount);
						synchronized (updateBookQueueLock) {
							updateBookQueue.add((BookBlock) book);
						}
					}
					books.put(value, book);
				}

				book.setSelected(isSelected);
				book.setHovered(index == hoveredIndex);
				return book;
			}
		};
	}

	public boolean isListMode() {
		return listMode;
	}

	public void setListMode(boolean listMode) {
		this.listMode = listMode;
		books.clear();
		list.setLayoutOrientation(listMode ? JList.VERTICAL : JList.HORIZONTAL_WRAP);

		if (listMode) {
			synchronized (updateBookQueueLock) {
				updateBookQueue.clear();
			}
		}
	}
}
