package be.nikiroo.fanfix.data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * The meta data associated to a {@link Story} object.
 * 
 * @author niki
 */
public class MetaData implements Cloneable {
	private String title;
	private String author;
	private String date;
	private Chapter resume;
	private List<String> tags;
	private BufferedImage cover;
	private String subject;
	private String source;
	private String url;
	private String uuid;
	private String luid;
	private String lang;
	private String publisher;
	private String type;
	private boolean imageDocument;

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
	 * The story publication date.
	 * 
	 * @return the date
	 */
	public String getDate() {
		return date;
	}

	/**
	 * The story publication date.
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
	 * 
	 * @return the resume
	 */
	public Chapter getResume() {
		return resume;
	}

	/**
	 * The story resume (a.k.a. description).
	 * 
	 * @param resume
	 *            the resume to set
	 */
	public void setResume(Chapter resume) {
		this.resume = resume;
	}

	/**
	 * The cover image of the story if any (can be NULL).
	 * 
	 * @return the cover
	 */
	public BufferedImage getCover() {
		return cover;
	}

	/**
	 * The cover image of the story if any (can be NULL).
	 * 
	 * @param cover
	 *            the cover to set
	 */
	public void setCover(BufferedImage cover) {
		this.cover = cover;
	}

	/**
	 * The subject of the story (or instance, if it is a fanfiction, what is the
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
	 * The source of this story (which online library it was downloaded from).
	 * 
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * The source of this story (which online library it was downloaded from).
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
	 * A unique value representing the story in the local library.
	 * 
	 * @return the luid
	 */
	public String getLuid() {
		return luid;
	}

	/**
	 * A unique value representing the story in the local library.
	 * 
	 * @param uuid
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
	 * The story publisher (other the same as the source).
	 * 
	 * @return the publisher
	 */
	public String getPublisher() {
		return publisher;
	}

	/**
	 * The story publisher (other the same as the source).
	 * 
	 * @param publisher
	 *            the publisher to set
	 */
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}

	/**
	 * The output type this {@link Story} is in.
	 * 
	 * @return the type the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * The output type this {@link Story} is in.
	 * 
	 * @param type
	 *            the new type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Document catering mostly to image files.
	 * 
	 * @return the imageDocument state
	 */
	public boolean isImageDocument() {
		return imageDocument;
	}

	/**
	 * Document catering mostly to image files.
	 * 
	 * @param imageDocument
	 *            the imageDocument state to set
	 */
	public void setImageDocument(boolean imageDocument) {
		this.imageDocument = imageDocument;
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
			meta.tags = new ArrayList<String>();
			meta.tags.addAll(tags);
		}
		if (resume != null) {
			meta.resume = new Chapter(resume.getNumber(), resume.getName());
			for (Paragraph para : resume) {
				meta.resume.getParagraphs().add(para);
			}
		}

		return meta;
	}
}
