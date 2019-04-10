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
	 * The number of stories result pages this tag can get.
	 * <p>
	 * We keep more information than what the getter/setter returns/accepts.
	 * <ul>
	 * <li>-2: this tag does not support stories results (not a leaf tag)</li>
	 * <li>-1: the number is not yet known, but will be known after a
	 * {@link BasicSearchable#fillTag(SearchableTag)} operation</li>
	 * <li>X: the number of pages</li>
	 * </ul>
	 */
	private int pages;

	/**
	 * Create a new {@link SearchableTag}.
	 * <p>
	 * Note that tags are complete by default.
	 * 
	 * @param id
	 *            the ID (usually a way to find the linked stories later on)
	 * @param name
	 *            the tag name, which can be displayed to the user
	 * @param leaf
	 *            the tag is a leaf tag, that is, it will not return subtags
	 *            with {@link BasicSearchable#fillTag(SearchableTag)} but will
	 *            return stories with
	 *            {@link BasicSearchable#search(SearchableTag)}
	 */
	public SearchableTag(String id, String name, boolean leaf) {
		this(id, name, leaf, true);
	}

	/**
	 * Create a new {@link SearchableTag}.
	 * 
	 * @param id
	 *            the ID (usually a way to find the linked stories later on)
	 * @param name
	 *            the tag name, which can be displayed to the user
	 * @param leaf
	 *            the tag is a leaf tag, that is, it will not return subtags
	 *            with {@link BasicSearchable#fillTag(SearchableTag)} but will
	 *            return stories with
	 *            {@link BasicSearchable#search(SearchableTag)}
	 * @param complete
	 *            the tag {@link SearchableTag#isComplete()} or not
	 */
	public SearchableTag(String id, String name, boolean leaf, boolean complete) {
		this.id = id;
		this.name = name;
		this.complete = complete;

		setLeaf(leaf);

		children = new ArrayList<SearchableTag>();
	}

	/**
	 * The ID (usually a way to find the linked stories later on).
	 * 
	 * @return the ID
	 */
	public String getId() {
		return id;
	}

	/**
	 * The tag name, which can be displayed to the user.
	 * 
	 * @return then name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Non-complete, non-leaf tags can still be completed via a
	 * {@link BasicSearchable#fillTag(SearchableTag)} operation from a
	 * {@link BasicSearchable}, in order to gain (more?) subtag children.
	 * <p>
	 * This method does not make sense for leaf tags.
	 * 
	 * @return TRUE if it is complete
	 */
	public boolean isComplete() {
		return complete;
	}

	/**
	 * Non-complete, non-leaf tags can still be completed via a
	 * {@link BasicSearchable#fillTag(SearchableTag)} operation from a
	 * {@link BasicSearchable}, in order to gain (more?) subtag children.
	 * <p>
	 * This method does not make sense for leaf tags.
	 * 
	 * @param complete
	 *            TRUE if it is complete
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
	 * The number of stories result pages this tag contains, only make sense if
	 * {@link SearchableTag#isLeaf()} returns TRUE.
	 * <p>
	 * Will return -1 if the number is not yet known.
	 * 
	 * @return the number of pages, or -1
	 */
	public int getPages() {
		return Math.max(-1, pages);
	}

	/**
	 * The number of stories result pages this tag contains, only make sense if
	 * {@link SearchableTag#isLeaf()} returns TRUE.
	 * 
	 * @param pages
	 *            the (positive or 0) number of pages
	 */
	public void setPages(int pages) {
		this.pages = Math.max(-1, pages);
	}

	/**
	 * This tag is a leaf tag, that is, it will not return other subtags with
	 * {@link BasicSearchable#fillTag(SearchableTag)} but will return stories
	 * with {@link BasicSearchable#search(SearchableTag)}.
	 * 
	 * @return TRUE if it is
	 */
	public boolean isLeaf() {
		return pages > -2;
	}

	/**
	 * This tag is a leaf tag, that is, it will not return other subtags with
	 * {@link BasicSearchable#fillTag(SearchableTag)} but will return stories
	 * with {@link BasicSearchable#search(SearchableTag)}.
	 * <p>
	 * Will reset the number of pages to -1.
	 * 
	 * @param leaf
	 *            TRUE if it is
	 */
	public void setLeaf(boolean leaf) {
		pages = leaf ? -1 : -2;
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
