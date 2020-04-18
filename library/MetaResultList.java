package be.nikiroo.fanfix.library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import be.nikiroo.fanfix.data.MetaData;

public class MetaResultList {
	private List<MetaData> metas;

	// Lazy lists:
	// TODO: sync-protect them?
	private List<String> sources;
	private List<String> authors;
	private List<String> tags;

	// can be null (will consider it empty)
	public MetaResultList(List<MetaData> metas) {
		if (metas == null) {
			metas = new ArrayList<MetaData>();
		}

		Collections.sort(metas);
		this.metas = metas;
	}

	// not NULL
	// sorted
	public List<MetaData> getMetas() {
		return metas;
	}

	public List<String> getSources() {
		if (sources == null) {
			sources = new ArrayList<String>();
			for (MetaData meta : metas) {
				if (!sources.contains(meta.getSource()))
					sources.add(meta.getSource());
			}
			sort(sources);
		}
		
		return sources;
	}

	// A -> (A), A/ -> (A, A/*) if we can find something for "*"
	public List<String> getSources(String source) {
		List<String> linked = new ArrayList<String>();
		if (source != null && !source.isEmpty()) {
			if (!source.endsWith("/")) {
				linked.add(source);
			} else {
				linked.add(source.substring(0, source.length() - 1));
				for (String src : getSources()) {
					if (src.startsWith(source)) {
						linked.add(src);
					}
				}
			}
		}

		sort(linked);
		return linked;
	}

	public List<String> getAuthors() {
		if (authors == null) {
			authors = new ArrayList<String>();
			for (MetaData meta : metas) {
				if (!authors.contains(meta.getAuthor()))
					authors.add(meta.getAuthor());
			}
			sort(authors);
		}
		
		return authors;
	}

	public List<String> getTags() {
		if (tags == null) {
			tags = new ArrayList<String>();
			for (MetaData meta : metas) {
				for (String tag : meta.getTags()) {
					if (!tags.contains(tag))
						tags.add(tag);
				}
			}
			sort(tags);
		}
		
		return tags;
	}

	// helper
	public List<MetaData> filter(String source, String author, String tag) {
		List<String> sources = source == null ? null : Arrays.asList(source);
		List<String> authors = author == null ? null : Arrays.asList(author);
		List<String> tags = tag == null ? null : Arrays.asList(tag);

		return filter(sources, authors, tags);
	}

	// null or empty -> no check, rest = must be included
	// source: a source ending in "/" means "this or any source starting with
	// this",
	// i;e., to enable source hierarchy
	// + sorted
	public List<MetaData> filter(List<String> sources, List<String> authors,
			List<String> tags) {
		if (sources != null && sources.isEmpty())
			sources = null;
		if (authors != null && authors.isEmpty())
			authors = null;
		if (tags != null && tags.isEmpty())
			tags = null;

		// Quick check
		if (sources == null && authors == null && tags == null) {
			return metas;
		}

		// allow "sources/" hierarchy
		if (sources != null) {
			List<String> folders = new ArrayList<String>();
			List<String> leaves = new ArrayList<String>();
			for (String source : sources) {
				if (source.endsWith("/")) {
					if (!folders.contains(source))
						folders.add(source);
				} else {
					if (!leaves.contains(source))
						leaves.add(source);
				}
			}

			sources = leaves;
			for (String folder : folders) {
				for (String otherLeaf : getSources(folder)) {
					if (!sources.contains(otherLeaf)) {
						sources.add(otherLeaf);
					}
				}
			}
		}

		List<MetaData> result = new ArrayList<MetaData>();
		for (MetaData meta : metas) {
			if (sources != null && !sources.contains(meta.getSource())) {
				continue;
			}
			if (authors != null && !authors.contains(meta.getAuthor())) {
				continue;
			}

			if (tags != null) {
				boolean keep = false;
				for (String thisTag : meta.getTags()) {
					if (tags.contains(thisTag))
						keep = true;
				}

				if (!keep)
					continue;
			}

			result.add(meta);
		}

		Collections.sort(result);
		return result;
	}
	
	/**
	 * Sort the given {@link String} values, ignoring case.
	 * 
	 * @param values
	 *            the values to sort
	 */
	private void sort(List<String> values) {
		Collections.sort(values, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return ("" + o1).compareToIgnoreCase("" + o2);
			}
		});
	}
}
