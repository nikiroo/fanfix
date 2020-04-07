package be.nikiroo.fanfix_swing.gui.browser;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.UIDefaults;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.plaf.TreeUI;
import javax.swing.plaf.basic.BasicTreeUI;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix_swing.gui.SearchBar;
import be.nikiroo.fanfix_swing.gui.utils.TreeCellSpanner;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;

public abstract class BasicTab<T> extends JPanel {
	private int totalCount = 0;
	private List<String> selectedElements = new ArrayList<String>();
	private T data;
	private String baseTitle;
	private String listenerCommand;
	private int index;

	private JTree tree;
	private SearchBar searchBar;

	public BasicTab(int index, String listenerCommand) {
		setLayout(new BorderLayout());

		this.index = index;
		this.listenerCommand = listenerCommand;

		data = createEmptyData();
		totalCount = 0;

		final DefaultMutableTreeNode root = new DefaultMutableTreeNode();

		tree = new JTree(root);
		tree.setUI(new BasicTreeUI());
		TreeCellSpanner spanner = new TreeCellSpanner(tree, generateCellRenderer());
		tree.setCellRenderer(spanner);
		tree.setRootVisible(false);
		tree.setShowsRootHandles(false);

		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				List<String> elements = new ArrayList<String>();
				TreePath[] paths = tree.getSelectionPaths();
				if (paths != null) {
					for (TreePath path : paths) {
						String key = path.getLastPathComponent().toString();
						elements.add(keyToElement(key));
					}
				}

				List<String> selectedElements = new ArrayList<String>();
				for (String element : elements) {
					if (!selectedElements.contains(element)) {
						selectedElements.add(element);
					}
				}

				BasicTab.this.selectedElements = selectedElements;

				fireActionPerformed();
			}
		});

		add(UiHelper.scroll(tree), BorderLayout.CENTER);

		searchBar = new SearchBar();
		add(searchBar, BorderLayout.NORTH);
		searchBar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				root.removeAllChildren();
				loadData(root, data, searchBar.getText());
				((DefaultTreeModel) tree.getModel()).reload();
				fireActionPerformed();
			}
		});

		SwingWorker<Map<String, List<String>>, Integer> worker = new SwingWorker<Map<String, List<String>>, Integer>() {
			@Override
			protected Map<String, List<String>> doInBackground() throws Exception {
				return Instance.getInstance().getLibrary().getSourcesGrouped();
			}

			@Override
			protected void done() {
				fillData(data);
				root.removeAllChildren();
				totalCount = loadData(root, data, searchBar.getText());
				((DefaultTreeModel) tree.getModel()).reload();

				fireActionPerformed();
			}
		};
		worker.execute();
	}

	/**
	 * The currently selected elements, or an empty list.
	 * 
	 * @return the sources (cannot be NULL)
	 */
	public List<String> getSelectedElements() {
		return selectedElements;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public String getBaseTitle() {
		return baseTitle;
	}

	public void setBaseTitle(String baseTitle) {
		this.baseTitle = baseTitle;
	}

	public String getTitle() {
		String title = getBaseTitle();
		String count = "";
		if (totalCount > 0) {
			int selected = selectedElements.size();
			count = " (" + (selected > 0 ? selected + "/" : "") + totalCount + ")";
		}

		return title + count;
	}

	public int getIndex() {
		return index;
	}

	public void unselect() {
		tree.clearSelection();
	}

	/**
	 * Adds the specified action listener to receive action events from this
	 * {@link SearchBar}.
	 *
	 * @param listener the action listener to be added
	 */
	public synchronized void addActionListener(ActionListener listener) {
		listenerList.add(ActionListener.class, listener);
	}

	/**
	 * Removes the specified action listener so that it no longer receives action
	 * events from this {@link SearchBar}.
	 *
	 * @param listener the action listener to be removed
	 */
	public synchronized void removeActionListener(ActionListener listener) {
		listenerList.remove(ActionListener.class, listener);
	}

	/**
	 * Notify the listeners of an action.
	 */
	protected void fireActionPerformed() {
		ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, listenerCommand);
		Object[] listeners = listenerList.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ActionListener.class) {
				((ActionListener) listeners[i + 1]).actionPerformed(e);
			}
		}
	}

	protected boolean checkFilter(String filter, String value) {
		return (filter == null || filter.isEmpty() || value.toLowerCase().contains(filter.toLowerCase()));
	}

	protected boolean checkFilter(String filter, List<String> list) {
		for (String value : list) {
			if (checkFilter(filter, value))
				return true;
		}
		return false;
	}

	protected abstract T createEmptyData();

	protected abstract void fillData(T data);

	protected abstract String keyToElement(String key);

	protected abstract String keyToDisplay(String key);

	protected abstract int loadData(DefaultMutableTreeNode root, T data, String filter);

	private TreeCellRenderer generateCellRenderer() {
		DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
					boolean leaf, int row, boolean hasFocus) {
				if (value instanceof DefaultMutableTreeNode) {
					if (((DefaultMutableTreeNode) value).getLevel() > 1) {
						setLeafIcon(null);
						setLeafIcon(IconGenerator.get(Icon.empty, Size.x4));
					} else {
						setLeafIcon(IconGenerator.get(Icon.empty, Size.x16));
					}
				}

				String display = value == null ? "" : value.toString();
				if (!display.isEmpty())
					display = keyToDisplay(display);

				return super.getTreeCellRendererComponent(tree, display, selected, expanded, leaf, row, hasFocus);
			}
		};

		renderer.setClosedIcon(IconGenerator.get(Icon.arrow_right, Size.x16));
		renderer.setOpenIcon(IconGenerator.get(Icon.arrow_down, Size.x16));
		renderer.setLeafIcon(IconGenerator.get(Icon.empty, Size.x16));

		return renderer;
	}
}
