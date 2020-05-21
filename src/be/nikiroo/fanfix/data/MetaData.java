package be.nikiroo.fanfix.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import be.nikiroo.fanfix.supported.SupportType;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.StringUtils;

/**
 * The meta data associated to a {@link Story} object.
 * <p>
 * Note that some earlier version of the program did not save the resume as an
 * external file; for those stories, the resume is not fetched until the story
 * is.
 * <p>
 * The cover is never fetched until the story is.
 * 
 * @author niki
 */
public class MetaData implements Cloneable, Comparable<MetaData>, Serializable {
	private static final long serialVersionUID = 1L;

	private String title;
	private String author;
	private String date;
	private Chapter resume;
	private List<String> tags;
	private Image cover;
	private String subject;
	private String source;
	private String url;
	private String uuid;
	private String luid;
	private String lang;
	private String publisher;
	private String type;
	private boolean imageDocument;
	private long words;
	private String creationDate;
	private boolean fakeCover;

	/**
	 * Create an empty {@link MetaData}.
	 */
	public MetaData() {
	}
	
	/**
	 * The title of the story.
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * The title of the story.
	 * 
	 * @param title
	 *            the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * The author of the story.
	 * 
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * The author of the story.
	 * 
	 * @param author
	 *            the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * The story publication date, we try to use "YYYY-mm-dd" when possible.
	 * 
	 * @return the date
	 */
	public String getDate() {
		return date;
	}

	/**
	 * The story publication date, we try to use "YYYY-mm-dd" when possible.
	 * 
	 * @param date
	 *            the date to set
	 */
	public void setDate(String date) {
		this.date = date;
	}

	/**
	 * The tags associated with this story.
	 * 
	 * @return the tags
	 */
	public List<String> getTags() {
		return tags;
	}

	/**
	 * The tags associated with this story.
	 * 
	 * @param tags
	 *            the tags to set
	 */
	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	/**
	 * The story resume (a.k.a. description).
	 * <p>
	 * This can be NULL if we don't have a resume for this {@link Story}.
	 * <p>
	 * Note that some earlier version of the program did not save the resume as
	 * an external file; for those stories, the resume is not fetched until the
	 * story is.
	 * 
	 * @return the resume
	 */
	public Chapter getResume() {
		return resume;
	}

	/**
	 * The story resume (a.k.a. description).
	 * <p>
	 * Note that some earlier version of the program did not save the resume as
	 * an external file; for those stories, the resume is not fetched until the
	 * story is.
	 * 
	 * @param resume
	 *            the resume to set
	 */
	public void setResume(Chapter resume) {
		this.resume = resume;
	}

	/**
	 * The cover image of the story, if any (can be NULL).
	 * <p>
	 * The cover is not fetched until the story is.
	 * 
	 * @return the cover
	 */
	public Image getCover() {
		return cover;
	}

	/**
	 * The cover image of the story, if any (can be NULL).
	 * <p>
	 * The cover is not fetched until the story is.
	 * 
	 * @param cover
	 *            the cover to set
	 */
	public void setCover(Image cover) {
		this.cover = cover;
	}

	/**
	 * The subject of the story (for instance, if it is a fanfiction, what is the
	 * original work; if it is a technical text, what is the technical
	 * subject...).
	 * 
	 * @return the subject
	 */
	public String getSubject() {
		return subject;
	}

	/**
	 * The subject of the story (for instance, if it is a fanfiction, what is
	 * the original work; if it is a technical text, what is the technical
	 * subject...).
	 * 
	 * @param subject
	 *            the subject to set
	 */
	public void setSubject(String subject) {
		this.subject = subject;
	}

	/**
	 * The source of this story -- a very user-visible piece of data.
	 * <p>
	 * It is initialised with the same value as {@link MetaData#getPublisher()},
	 * but the user is allowed to change it into any value -- this is a sort of
	 * 'category'.
	 * 
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * The source of this story -- a very user-visible piece of data.
	 * <p>
	 * It is initialised with the same value as {@link MetaData#getPublisher()},
	 * but the user is allowed to change it into any value -- this is a sort of
	 * 'category'.
	 * 
	 * @param source
	 *            the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * The original URL from which this {@link Story} was imported.
	 * 
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * The original URL from which this {@link Story} was imported.
	 * 
	 * @param url
	 *            the new url to set
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * A unique value representing the story (it is often a URL).
	 * 
	 * @return the uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * A unique value representing the story (it is often a URL).
	 * 
	 * @param uuid
	 *            the uuid to set
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * A unique value representing the story in the local library (usually a
	 * numerical value 0-padded with a minimum size of 3; but this is subject to
	 * change and you can also obviously have more than 1000 stories --
	 * <strong>a luid may potentially be anything else, including non-numeric
	 * characters</strong>).
	 * <p>
	 * A NULL or empty luid represents an incomplete, corrupted or fake
	 * {@link Story}.
	 * 
	 * @return the luid
	 */
	public String getLuid() {
		return luid;
	}

	/**
	 * A unique value representing the story in the local library (usually a
	 * numerical value 0-padded with a minimum size of 3; but this is subject to
	 * change and you can also obviously have more than 1000 stories --
	 * <strong>a luid may potentially be anything else, including non-numeric
	 * characters</strong>).
	 * <p>
	 * A NULL or empty luid represents an incomplete, corrupted or fake
	 * {@link Story}.
	 * 
	 * @param luid
	 *            the luid to set
	 */
	public void setLuid(String luid) {
		this.luid = luid;
	}

	/**
	 * The 2-letter code language of this story.
	 * 
	 * @return the lang
	 */
	public String getLang() {
		return lang;
	}

	/**
	 * The 2-letter code language of this story.
	 * 
	 * @param lang
	 *            the lang to set
	 */
	public void setLang(String lang) {
		this.lang = lang;
	}

	/**
	 * The story publisher -- which is also the user representation of the
	 * output type this {@link Story} is in (see {@link SupportType}).
	 * <p>
	 * It allows you to know where the {@link Story} comes from, and is not
	 * supposed to change, even when re-imported.
	 * <p>
	 * It's the user representation of the enum
	 * ({@link SupportType#getSourceName()}, not
	 * {@link SupportType#toString()}).
	 * 
	 * @return the publisher
	 */
	public String getPublisher() {
		return publisher;
	}

	/**
	 * The story publisher -- which is also the user representation of the
	 * output type this {@link Story} is in (see {@link SupportType}).
	 * <p>
	 * It allows you to know where the {@link Story} comes from, and is not
	 * supposed to change, even when re-imported.
	 * <p>
	 * It's the user representation of the enum
	 * ({@link SupportType#getSourceName()}, not
	 * {@link SupportType#toString()}).
	 * 
	 * @param publisher
	 *            the publisher to set
	 */
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	/**
	 * The output type this {@link Story} is in (see {@link SupportType}).
	 * <p>
	 * It allows you to know where the {@link Story} comes from, and is supposed
	 * to only change when it is imported anew.
	 * <p>
	 * It's the direct representation of the enum
	 * ({@link SupportType#toString()}, not
	 * {@link SupportType#getSourceName()}).
	 * 
	 * @return the type the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * The output type this {@link Story} is in (see {@link SupportType}).
	 * <p>
	 * It allows you to know where the {@link Story} comes from, and is supposed
	 * to only change when it is imported anew.
	 * <p>
	 * It's the direct representation of the enum
	 * ({@link SupportType#toString()}, not
	 * {@link SupportType#getSourceName()}).
	 * 
	 * @param type
	 *            the new type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Document catering mostly to image files.
	 * <p>
	 * I.E., this is a comics or a manga, not a textual story with actual words.
	 * <p>
	 * In image documents, all the paragraphs are supposed to be images.
	 * 
	 * @return the imageDocument state
	 */
	public boolean isImageDocument() {
		return imageDocument;
	}

	/**
	 * Document catering mostly to image files.
	 * <p>
	 * I.E., this is a comics or a manga, not a textual story with actual words.
	 * <p>
	 * In image documents, all the paragraphs are supposed to be images.
	 * 
	 * @param imageDocument
	 *            the imageDocument state to set
	 */
	public void setImageDocument(boolean imageDocument) {
		this.imageDocument = imageDocument;
	}

	/**
	 * The number of words (or images if this is an image document -- see
	 * {@link MetaData#isImageDocument()}) in the related {@link Story}.
	 * 
	 * @return the number of words/images
	 */
	public long getWords() {
		return words;
	}

	/**
	 * The number of words (or images if this is an image document -- see
	 * {@link MetaData#isImageDocument()}) in the related {@link Story}.
	 * 
	 * @param words
	 *            the number of words/images to set
	 */
	public void setWords(long words) {
		this.words = words;
	}

	/**
	 * The (Fanfix) {@link Story} creation date, i.e., when the {@link Story}
	 * was fetched via Fanfix.
	 * 
	 * @return the creation date
	 */
	public String getCreationDate() {
		return creationDate;
	}

	/**
	 * The (Fanfix) {@link Story} creation date, i.e., when the {@link Story}
	 * was fetched via Fanfix.
	 * 
	 * @param creationDate
	 *            the creation date to set
	 */
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * The cover in this {@link MetaData} object is "fake", in the sense that it
	 * comes from the actual content images.
	 * 
	 * @return TRUE for a fake cover
	 */
	public boolean isFakeCover() {
		return fakeCover;
	}

	/**
	 * The cover in this {@link MetaData} object is "fake", in the sense that it
	 * comes from the actual content images
	 * 
	 * @param fakeCover
	 *            TRUE for a fake cover
	 */
	public void setFakeCover(boolean fakeCover) {
		this.fakeCover = fakeCover;
	}

	@Override
	public int compareTo(MetaData o) {
		if (o == null) {
			return 1;
		}

		String id = (getTitle() == null ? "" : getTitle())
				+ (getUuid() == null ? "" : getUuid())
				+ (getLuid() == null ? "" : getLuid());
		String oId = (getTitle() == null ? "" : o.getTitle())
				+ (getUuid() == null ? "" : o.getUuid())
				+ (o.getLuid() == null ? "" : o.getLuid());

		return id.compareToIgnoreCase(oId);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MetaData)) {
			return false;
		}

		return compareTo((MetaData) obj) == 0;
	}

	@Override
	public int hashCode() {
		String uuid = getUuid();
		if (uuid == null) {
			uuid = "" + title + author + source;
		}

		return uuid.hashCode();
	}

	@Override
	public MetaData clone() {
		MetaData meta = null;
		try {
			meta = (MetaData) super.clone();
		} catch (CloneNotSupportedException e) {
			// Did the clones rebel?
			System.err.println(e);
		}

		if (tags != null) {
			meta.tags = new ArrayList<String>(tags);
		}

		if (resume != null) {
			meta.resume = resume.clone();
		}

		return meta;
	}

	/**
	 * Display a DEBUG {@link String} representation of this object.
	 * <p>
	 * This is not efficient, nor intended to be.
	 */
	@Override
	public String toString() {
		String title = "";
		if (getTitle() != null) {
			title = getTitle();
		}

		StringBuilder tags = new StringBuilder();
		if (getTags() != null) {
			for (String tag : getTags()) {
				if (tags.length() > 0) {
					tags.append(", ");
				}
				tags.append(tag);
			}
		}

		String resume = "";
		if (getResume() != null) {
			for (Paragraph para : getResume()) {
				resume += "\n\t";
				resume += para.toString().substring(0,
						Math.min(para.toString().length(), 120));
			}
			resume += "\n";
		}

		String cover = "none";
		if (getCover() != null) {
			cover = StringUtils.formatNumber(getCover().getSize())
					+ "bytes";
		}

		return String.format(
				"Meta %s:\n\tTitle: [%s]\n\tAuthor: [%s]\n\tDate: [%s]\n\tTags: [%s]\n\tWord count: [%s]"
						+ "\n\tResume: [%s]\n\tCover: [%s]",
				luid, title, getAuthor(), getDate(), tags.toString(),
				"" + words, resume, cover);
	}
}
