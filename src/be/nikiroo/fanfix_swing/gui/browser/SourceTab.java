package be.nikiroo.fanfix_swing.gui.browser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import be.nikiroo.fanfix.Instance;

public class SourceTab extends BasicTab<Map<String, List<String>>> {
	public SourceTab(int index, String listenerCommand) {
		super(index, listenerCommand);
	}

	@Override
	protected Map<String, List<String>> createEmptyData() {
		return new HashMap<String, List<String>>();
	}

	@Override
	protected void fillData(Map<String, List<String>> data) {
		data.clear();
		try {
			Map<String, List<String>> sourcesGrouped = Instance.getInstance()
					.getLibrary().getSourcesGrouped();
			for (String group : sourcesGrouped.keySet()) {
				data.put(group, sourcesGrouped.get(group));
			}
		} catch (Exception e) {
			// TODO
			e.printStackTrace();
		}
	}

	@Override
	protected String keyToElement(String key) {
		return key.substring(1);
	}

	@Override
	protected String keyToDisplay(String key) {
		if (key.trim().isEmpty()) {
			return "[*]"; // Root node
		}

		// Get and remove type
		String type = key.substring(0, 1);
		key = key.substring(1);

		if (!type.equals(">")) {
			// Only display the final name
			int pos = key.toString().lastIndexOf("/");
			if (pos >= 0) {
				key = key.toString().substring(pos + 1);
			}
		}

		if (key.toString().isEmpty()) {
			key = " ";
		}

		return key;
	}

	@Override
	protected int loadData(DefaultMutableTreeNode root,
			Map<String, List<String>> sourcesGrouped, String filter) {
		int count = 0;
		List<String> sources = new ArrayList<String>(sourcesGrouped.keySet());
		sort(sources);
		for (String source : sources) {
			if (checkFilter(filter, source)
					|| checkFilter(filter, sourcesGrouped.get(source))) {
				List<String> children = sourcesGrouped.get(source);
				boolean hasChildren = (children.size() > 1)
						|| (children.size() == 1
								&& !children.get(0).trim().isEmpty());
				DefaultMutableTreeNode sourceNode = new DefaultMutableTreeNode(
						">" + source + (hasChildren ? "/" : ""));
				root.add(sourceNode);
				sort(children);
				for (String subSource : children) {
					if (checkFilter(filter, source)
							|| checkFilter(filter, subSource)) {
						count = count + 1;
						if (subSource.isEmpty()
								&& sourcesGrouped.get(source).size() > 1) {
							sourceNode.add(
									new DefaultMutableTreeNode(" " + source));
						} else if (!subSource.isEmpty()) {
							sourceNode.add(new DefaultMutableTreeNode(
									" " + source + "/" + subSource));
						}
					}
				}
			}
		}

		return count;
	}
}
