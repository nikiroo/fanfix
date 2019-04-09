package be.nikiroo.fanfix.searchable;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.jsoup.helper.DataUtil;
import org.jsoup.nodes.Document;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.SupportType;

/**
 * This class supports browsing through stories on the supported websites. It
 * will fetch some {@link MetaData} that satisfy a search query or some tags if
 * supported.
 * 
 * @author niki
 */
public abstract class BasicSearchable {
	private SupportType type;
	private BasicSupport support;

	/**
	 * Create a new {@link BasicSearchable} of the given type.
	 * 
	 * @param type
	 *            the type, must not be NULL
	 */
	public BasicSearchable(SupportType type) {
		setType(type);
		support = BasicSupport.getSupport(getType(), null);
	}

	/**
	 * The support type.
	 * 
	 * @return the type
	 */
	public SupportType getType() {
		return type;
	}

	/**
	 * The support type.
	 * 
	 * @param type
	 *            the new type
	 */
	protected void setType(SupportType type) {
		this.type = type;
	}

	/**
	 * The associated {@link BasicSupport}.
	 * <p>
	 * Mostly used to download content.
	 * 
	 * @return the support
	 */
	protected BasicSupport getSupport() {
		return support;
	}

	/**
	 * Get a list of tags that can be browsed here.
	 * 
	 * @return the list of tags
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	abstract public List<SearchableTag> getTags() throws IOException;

	/**
	 * Fill the tag (set it 'complete') with more information from the support.
	 * 
	 * @param tag
	 *            the tag to fill
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	abstract protected void fillTag(SearchableTag tag) throws IOException;

	/**
	 * Search for the given term and return a list of stories satisfying this
	 * search term.
	 * <p>
	 * Not that the returned stories will <b>NOT</b> be complete, but will only
	 * contain enough information to present them to the user and retrieve them.
	 * <p>
	 * URL is guaranteed to be usable, LUID will always be NULL.
	 * 
	 * @param search
	 *            the term to search for
	 * 
	 * @return a list of stories that satisfy that search term
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	abstract public List<MetaData> search(String search) throws IOException;

	/**
	 * Search for the given tag and return a list of stories satisfying this
	 * tag.
	 * <p>
	 * Not that the returned stories will <b>NOT</b> be complete, but will only
	 * contain enough information to present them to the user and retrieve them.
	 * <p>
	 * URL is guaranteed to be usable, LUID will always be NULL.
	 * 
	 * @param tagId
	 *            the tag to search for
	 * 
	 * @return a list of stories that satisfy that search term
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	abstract public List<MetaData> search(SearchableTag tag) throws IOException;

	/**
	 * Load a document from its url.
	 * 
	 * @param url
	 *            the URL to load
	 * @return the document
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected Document load(String url) throws IOException {
		return load(new URL(url));
	}

	/**
	 * Load a document from its url.
	 * 
	 * @param url
	 *            the URL to load
	 * @return the document
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected Document load(URL url) throws IOException {
		return DataUtil.load(Instance.getCache().open(url, support, false),
				"UTF-8", url.toString());
	}

	/**
	 * Return a {@link BasicSearchable} implementation supporting the given
	 * type, or NULL if it does not exist.
	 * 
	 * @param type
	 *            the type, must not be NULL
	 * 
	 * @return an implementation that supports it, or NULL
	 */
	public static BasicSearchable getSearchable(SupportType type) {
		BasicSearchable support = null;

		switch (type) {
		case FIMFICTION:
			// TODO
			break;
		case FANFICTION:
			support = new Fanfiction(type);
			break;
		case MANGAFOX:
			// TODO
			break;
		case E621:
			// TODO
			break;
		case YIFFSTAR:
			// TODO
			break;
		case E_HENTAI:
			// TODO
			break;
		case MANGA_LEL:
			// TODO
			break;
		case CBZ:
		case HTML:
		case INFO_TEXT:
		case TEXT:
		case EPUB:
			break;
		}

		return support;
	}
}
