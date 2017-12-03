package be.nikiroo.fanfix.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The main data class, where the whole story resides.
 * 
 * @author niki
 */
public class Story implements Iterable<Chapter>, Cloneable {
	private MetaData meta;
	private List<Chapter> chapters = new ArrayList<Chapter>();
	private List<Chapter> empty = new ArrayList<Chapter>();

	/**
	 * The metadata about this {@link Story}.
	 * 
	 * @return the meta
	 */
	public MetaData getMeta() {
		return meta;
	}

	/**
	 * The metadata about this {@link Story}.
	 * 
	 * @param meta
	 *            the meta to set
	 */
	public void setMeta(MetaData meta) {
		this.meta = meta;
	}

	/**
	 * The chapters of the story.
	 * 
	 * @return the chapters
	 */
	public List<Chapter> getChapters() {
		return chapters;
	}

	/**
	 * The chapters of the story.
	 * 
	 * @param chapters
	 *            the chapters to set
	 */
	public void setChapters(List<Chapter> chapters) {
		this.chapters = chapters;
	}

	/**
	 * Get an iterator on the {@link Chapter}s.
	 */
	@Override
	public Iterator<Chapter> iterator() {
		return chapters == null ? empty.iterator() : chapters.iterator();
	}

	/**
	 * Display a DEBUG {@link String} representation of this object.
	 * <p>
	 * This is not efficient, nor intended to be.
	 */
	@Override
	public String toString() {
		String title = "";
		if (meta != null && meta.getTitle() != null) {
			title = meta.getTitle();
		}

		String tags = "";
		if (meta != null && meta.getTags() != null) {
			for (String tag : meta.getTags()) {
				if (!tags.isEmpty()) {
					tags += ", ";
				}
				tags += tag;
			}
		}

		String resume = "";
		if (meta != null && meta.getResume() != null) {
			for (Paragraph para : meta.getResume()) {
				resume += "\n\t";
				resume += para.toString().substring(0,
						Math.min(para.toString().length(), 120));
			}
			resume += "\n";
		}

		String cover = "none";
		if (meta != null && meta.getCover() != null) {
			cover = " bytes";

			int size = meta.getCover().getData().length;
			if (size > 1000) {
				size /= 1000;
				cover = " kb";
				if (size > 1000) {
					size /= 1000;
					cover = " mb";
				}
			}

			cover = size + cover;
		}
		return String.format(
				"Title: [%s]\nAuthor: [%s]\nDate: [%s]\nTags: [%s]\n"
						+ "Resume: [%s]\nCover: [%s]", title, meta == null ? ""
						: meta.getAuthor(), meta == null ? "" : meta.getDate(),
				tags, resume, cover);
	}

	@Override
	public Story clone() {
		Story story = null;
		try {
			story = (Story) super.clone();
		} catch (CloneNotSupportedException e) {
			// Did the clones rebel?
			System.err.println(e);
		}

		if (meta != null) {
			story.meta = meta.clone();
		}

		if (chapters != null) {
			story.chapters = new ArrayList<Chapter>();
			for (Chapter chap : chapters) {
				story.chapters.add(chap.clone());
			}
		}

		return story;
	}
}
