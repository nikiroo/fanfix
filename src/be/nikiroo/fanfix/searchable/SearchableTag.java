package be.nikiroo.fanfix.searchable;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a tag that can be searched on a supported website.
 * 
 * @author niki
 */
public class SearchableTag {
	private String id;
	private String name;
	private boolean complete;
	private long count;
	private List<SearchableTag> children;

	/**
	 * Create a new {@link SearchableTag}.
	 * 
	 * @param id
	 *            the ID (usually a way to find the linked stories later on)
	 * @param name
	 *            the tag name, which can be displayed to the user
	 * @param complete
	 *            TRUE for a {@link SearchableTag} that cannot be "filled" by
	 *            the {@link BasicSearchable} in order to get (more?) subtag
	 *            children
	 */
	public SearchableTag(String id, String name, boolean complete) {
		this.id = id;
		this.name = name;
		this.complete = complete;

		children = new ArrayList<SearchableTag>();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	/**
	 * This tag can still be completed via a "fill" tag operation from a
	 * {@link BasicSearchable}, in order to gain (more?) subtag children.
	 * 
	 * @return TRUE if it can
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * This tag can still be completed via a "fill" tag operation from a
	 * {@link BasicSearchable}, in order to gain (more?) subtag children.
	 * 
	 * @param complete
	 *            TRUE if it can
	 */
	public void setComplete(boolean complete) {
		this.complete = complete;
	}

	/**
	 * The number of items that can be found with this tag if it is searched.
	 * <p>
	 * Will report the number of subtags by default.
	 * 
	 * @return the number of items
	 */
	public long getCount() {
		long count = this.count;
		if (count <= 0) {
			count = children.size();
		}

		return count;
	}

	/**
	 * The number of items that can be found with this tag if it is searched,
	 * displayable format.
	 * <p>
	 * Will report the number of subtags by default.
	 * 
	 * @return the number of items
	 */
	public String getCountDisplay() {
		long count = this.count;
		if (count <= 0) {
			count = children.size();
		}

		if (count > 999999) {
			return count / 1000000 + "M";
		}

		if (count > 2000) {
			return count / 1000 + "k";
		}

		return Long.toString(count);
	}

	/**
	 * The number of items that can be found with this tag if it is searched.
	 * 
	 * @param count
	 *            the new count
	 */
	public void setCount(long count) {
		this.count = count;
	}

	/**
	 * The subtag children of this {@link SearchableTag}.
	 * <p>
	 * Never NULL.
	 * <p>
	 * Note that if {@link SearchableTag#isComplete()} returns false, you can
	 * still fill (more?) subtag children with a {@link BasicSearchable}.
	 * 
	 * @return the subtag children, never NULL
	 */
	public List<SearchableTag> getChildren() {
		return children;
	}

	/**
	 * Add the given {@link SearchableTag} as a subtag child.
	 * 
	 * @param tag
	 *            the tag to add
	 */
	public void add(SearchableTag tag) {
		children.add(tag);
	}

	/**
	 * Display a DEBUG {@link String} representation of this object.
	 */
	@Override
	public String toString() {
		String rep = name + " [" + id + "]";
		if (!complete) {
			rep += "*";
		}

		if (getCount() > 0) {
			rep += " (" + getCountDisplay() + ")";
		}

		if (!children.isEmpty()) {
			String tags = "";
			int i = 1;
			for (SearchableTag tag : children) {
				if (!tags.isEmpty()) {
					tags += ", ";
				}

				if (i > 10) {
					tags += "...";
					break;
				}

				tags += tag;
				i++;
			}

			rep += ": " + tags;
		}

		return rep;
	}
}
