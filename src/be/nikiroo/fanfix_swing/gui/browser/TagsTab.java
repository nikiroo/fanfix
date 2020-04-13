package be.nikiroo.fanfix_swing.gui.browser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.library.MetaResultList;

public class TagsTab extends BasicTab<List<String>> {
	public TagsTab(int index, String listenerCommand) {
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
			MetaResultList metas = Instance.getInstance().getLibrary()
					.getList();
			// TODO: getTagList, getAuthorList... ?
			for (MetaData meta : metas.getMetas()) {
				List<String> tags = meta.getTags();
				if (tags != null) {
					for (String tag : tags) {
						if (!data.contains(tag)) {
							data.add(tag);
						}
					}
				}
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
			// TODO: new TAG_UNKNOWN needed
			key = Instance.getInstance().getTransGui()
					.getString(StringIdGui.MENU_AUTHORS_UNKNOWN);
		}

		return key;
	}

	@Override
	protected int loadData(DefaultMutableTreeNode root, List<String> tags,
			String filter) {
		for (String tag : tags) {
			if (checkFilter(filter, tag)) {
				DefaultMutableTreeNode sourceNode = new DefaultMutableTreeNode(
						tag);
				root.add(sourceNode);
			}
		}

		return tags.size();
	}
}
