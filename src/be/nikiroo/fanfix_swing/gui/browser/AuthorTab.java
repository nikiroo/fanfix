package be.nikiroo.fanfix_swing.gui.browser;

import java.util.ArrayList;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;

public class AuthorTab extends BasicTab<List<String>> {
	public AuthorTab(int index, String listenerCommand) {
		super(index, listenerCommand);
	}

	@Override
	protected List<String> createEmptyData() {
		return new ArrayList<String>();
	}

	@Override
	protected void fillData(List<String> data) {
		data.clear();
		try {
			List<String> authors = Instance.getInstance().getLibrary()
					.getAuthors();
			for (String author : authors) {
				data.add(author);
			}

			sort(data);
		} catch (Exception e) {
			// TODO
			e.printStackTrace();
		}
	}

	@Override
	protected String keyToElement(String key) {
		return key;
	}

	@Override
	protected String keyToDisplay(String key) {
		if (key.trim().isEmpty()) {
			key = Instance.getInstance().getTransGui()
					.getString(StringIdGui.MENU_AUTHORS_UNKNOWN);
		}

		return key;
	}

	@Override
	protected int loadData(DefaultMutableTreeNode root, List<String> authors,
			String filter) {
		for (String author : authors) {
			if (checkFilter(filter, author)) {
				DefaultMutableTreeNode sourceNode = new DefaultMutableTreeNode(
						author);
				root.add(sourceNode);
			}
		}

		return authors.size();
	}
}
