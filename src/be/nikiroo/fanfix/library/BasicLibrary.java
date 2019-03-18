package be.nikiroo.fanfix.library;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.fanfix.supported.SupportType;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.StringUtils;

/**
 * Manage a library of Stories: import, export, list, modify.
 * <p>
 * Each {@link Story} object will be associated with a (local to the library)
 * unique ID, the LUID, which will be used to identify the {@link Story}.
 * <p>
 * Most of the {@link BasicLibrary} functions work on a partial (cover
 * <b>MAY</b> not be included) {@link MetaData} object.
 * 
 * @author niki
 */
abstract public class BasicLibrary {
	/**
	 * A {@link BasicLibrary} status.
	 * 
	 * @author niki
	 */
	public enum Status {
		/** The library is ready. */
		READY,
		/** The library is invalid (not correctly set up). */
		INVALID,
		/** You are not allowed to access this library. */
		UNAUTORIZED,
		/** The library is currently out of commission. */
		UNAVAILABLE,
	}

	/**
	 * Return a name for this library (the UI may display this).
	 * <p>
	 * Must not be NULL.
	 * 
	 * @return the name, or an empty {@link String} if none
	 */
	public String getLibraryName() {
		return "";
	}

	/**
	 * The library status.
	 * 
	 * @return the current status
	 */
	public Status getStatus() {
		return Status.READY;
	}

	/**
	 * Retrieve the main {@link File} corresponding to the given {@link Story},
	 * which can be passed to an external reader or instance.
	 * <p>
	 * Do <b>NOT</b> alter this file.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * @param pg
	 *            the optional {@link Progress}
	 * 
	 * @return the corresponding {@link Story}
	 */
	public abstract File getFile(String luid, Progress pg);

	/**
	 * Return the cover image associated to this story.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * 
	 * @return the cover image
	 */
	public abstract Image getCover(String luid);

	/**
	 * Return the cover image associated to this source.
	 * <p>
	 * By default, return the custom cover if any, and if not, return the cover
	 * of the first story with this source.
	 * 
	 * @param source
	 *            the source
	 * 
	 * @return the cover image or NULL
	 */
	public Image getSourceCover(String source) {
		Image custom = getCustomSourceCover(source);
		if (custom != null) {
			return custom;
		}

		List<MetaData> metas = getListBySource(source);
		if (metas.size() > 0) {
			return getCover(metas.get(0).getLuid());
		}

		return null;
	}

	/**
	 * Return the custom cover image associated to this source.
	 * <p>
	 * By default, return NULL.
	 * 
	 * @param source
	 *            the source to look for
	 * 
	 * @return the custom cover or NULL if none
	 */
	public Image getCustomSourceCover(@SuppressWarnings("unused") String source) {
		return null;
	}

	/**
	 * Fix the source cover to the given story cover.
	 * 
	 * @param source
	 *            the source to change
	 * @param luid
	 *            the story LUID
	 */
	public abstract void setSourceCover(String source, String luid);

	/**
	 * Return the list of stories (represented by their {@link MetaData}, which
	 * <b>MAY</b> not have the cover included).
	 * 
	 * @param pg
	 *            the optional {@link Progress}
	 * 
	 * @return the list (can be empty but not NULL)
	 */
	protected abstract List<MetaData> getMetas(Progress pg);

	/**
	 * Invalidate the {@link Story} cache (when the content should be re-read
	 * because it was changed).
	 */
	protected void invalidateInfo() {
		invalidateInfo(null);
	}

	/**
	 * Invalidate the {@link Story} cache (when the content is removed).
	 * <p>
	 * All the cache can be deleted if NULL is passed as meta.
	 * 
	 * @param luid
	 *            the LUID of the {@link Story} to clear from the cache, or NULL
	 *            for all stories
	 */
	protected abstract void invalidateInfo(String luid);

	/**
	 * Invalidate the {@link Story} cache (when the content has changed, but we
	 * already have it) with the new given meta.
	 * 
	 * @param meta
	 *            the {@link Story} to clear from the cache
	 */
	protected abstract void updateInfo(MetaData meta);

	/**
	 * Return the next LUID that can be used.
	 * 
	 * @return the next luid
	 */
	protected abstract int getNextId();

	/**
	 * Delete the target {@link Story}.
	 * 
	 * @param luid
	 *            the LUID of the {@link Story}
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} wa not found
	 */
	protected abstract void doDelete(String luid) throws IOException;

	/**
	 * Actually save the story to the back-end.
	 * 
	 * @param story
	 *            the {@link Story} to save
	 * @param pg
	 *            the optional {@link Progress}
	 * 
	 * @return the saved {@link Story} (which may have changed, especially
	 *         regarding the {@link MetaData})
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract Story doSave(Story story, Progress pg)
			throws IOException;

	/**
	 * Refresh the {@link BasicLibrary}, that is, make sure all metas are
	 * loaded.
	 * 
	 * @param pg
	 *            the optional progress reporter
	 */
	public void refresh(Progress pg) {
		getMetas(pg);
	}

	/**
	 * List all the known types (sources) of stories.
	 * 
	 * @return the sources
	 */
	public synchronized List<String> getSources() {
		List<String> list = new ArrayList<String>();
		for (MetaData meta : getMetas(null)) {
			String storySource = meta.getSource();
			if (!list.contains(storySource)) {
				list.add(storySource);
			}
		}

		Collections.sort(list);
		return list;
	}

	/**
	 * List all the known authors of stories.
	 * 
	 * @return the authors
	 */
	public synchronized List<String> getAuthors() {
		List<String> list = new ArrayList<String>();
		for (MetaData meta : getMetas(null)) {
			String storyAuthor = meta.getAuthor();
			if (!list.contains(storyAuthor)) {
				list.add(storyAuthor);
			}
		}

		Collections.sort(list);
		return list;
	}

	/**
	 * Return the list of authors, grouped by starting letter(s) if needed.
	 * <p>
	 * If the number of author is not too high, only one group with an empty
	 * name and all the authors will be returned.
	 * <p>
	 * If not, the authors will be separated into groups:
	 * <ul>
	 * <li><tt>*</tt>: any author whose name doesn't contain letters nor numbers
	 * </li>
	 * <li><tt>0-9</tt>: any authors whose name starts with a number</li>
	 * <li><tt>A-C</tt> (for instance): any author whose name starts with
	 * <tt>A</tt>, <tt>B</tt> or <tt>C</tt></li>
	 * </ul>
	 * Note that the letters used in the groups can vary (except <tt>*</tt> and
	 * <tt>0-9</tt>, which may only be present or not).
	 * 
	 * @return the authors' names, grouped by letter(s)
	 */
	public List<Entry<String, List<String>>> getAuthorsGrouped() {
		int MAX = 20;

		List<Entry<String, List<String>>> groups = new ArrayList<Entry<String, List<String>>>();
		List<String> authors = getAuthors();

		if (authors.size() <= MAX) {
			groups.add(new SimpleEntry<String, List<String>>("", authors));
			return groups;
		}

		groups.add(new SimpleEntry<String, List<String>>("*", getAuthorsGroup(
				authors, '*')));
		groups.add(new SimpleEntry<String, List<String>>("0-9",
				getAuthorsGroup(authors, '0')));

		for (char car = 'A'; car <= 'Z'; car++) {
			groups.add(new SimpleEntry<String, List<String>>(Character
					.toString(car), getAuthorsGroup(authors, car)));
		}

		// do NOT collapse * and [0-9] with the rest
		for (int i = 2; i + 1 < groups.size(); i++) {
			Entry<String, List<String>> now = groups.get(i);
			Entry<String, List<String>> next = groups.get(i + 1);
			int currentTotal = now.getValue().size() + next.getValue().size();
			if (currentTotal <= MAX) {
				String key = now.getKey().charAt(0) + "-"
						+ next.getKey().charAt(next.getKey().length() - 1);
				List<String> all = new ArrayList<String>();
				all.addAll(now.getValue());
				all.addAll(next.getValue());
				groups.set(i, new SimpleEntry<String, List<String>>(key, all));
				groups.remove(i + 1);
				i--;
			}
		}

		for (int i = 0; i < groups.size(); i++) {
			if (groups.get(i).getValue().size() == 0) {
				groups.remove(i);
				i--;
			}
		}

		return groups;
	}

	/**
	 * Get all the authors that start with the given character:
	 * <ul>
	 * <li><tt>*</tt>: any author whose name doesn't contain letters nor numbers
	 * </li>
	 * <li><tt>0</tt>: any authors whose name starts with a number</li>
	 * <li><tt>A</tt> (any capital latin letter): any author whose name starts
	 * with <tt>A</tt></li>
	 * </ul>
	 * 
	 * @param authors
	 *            the full list of authors
	 * @param car
	 *            the starting character, <tt>*</tt>, <tt>0</tt> or a capital
	 *            letter
	 * @return the authors that fulfill the starting letter
	 */
	private List<String> getAuthorsGroup(List<String> authors, char car) {
		List<String> accepted = new ArrayList<String>();
		for (String author : authors) {
			char first = '*';
			for (int i = 0; first == '*' && i < author.length(); i++) {
				String san = StringUtils.sanitize(author, true, true);
				char c = san.charAt(i);
				if (c >= '0' && c <= '9') {
					first = '0';
				} else if (c >= 'a' && c <= 'z') {
					first = (char) (c - 'a' + 'A');
				} else if (c >= 'A' && c <= 'Z') {
					first = c;
				}
			}

			if (first == car) {
				accepted.add(author);
			}
		}

		return accepted;
	}

	/**
	 * List all the stories in the {@link BasicLibrary}.
	 * <p>
	 * Cover images <b>MAYBE</b> not included.
	 * 
	 * @return the stories
	 */
	public synchronized List<MetaData> getList() {
		return getMetas(null);
	}

	/**
	 * List all the stories of the given source type in the {@link BasicLibrary}
	 * , or all the stories if NULL is passed as a type.
	 * <p>
	 * Cover images not included.
	 * 
	 * @param type
	 *            the type of story to retrieve, or NULL for all
	 * 
	 * @return the stories
	 */
	public synchronized List<MetaData> getListBySource(String type) {
		List<MetaData> list = new ArrayList<MetaData>();
		for (MetaData meta : getMetas(null)) {
			String storyType = meta.getSource();
			if (type == null || type.equalsIgnoreCase(storyType)) {
				list.add(meta);
			}
		}

		Collections.sort(list);
		return list;
	}

	/**
	 * List all the stories of the given author in the {@link BasicLibrary}, or
	 * all the stories if NULL is passed as an author.
	 * <p>
	 * Cover images not included.
	 * 
	 * @param author
	 *            the author of the stories to retrieve, or NULL for all
	 * 
	 * @return the stories
	 */
	public synchronized List<MetaData> getListByAuthor(String author) {
		List<MetaData> list = new ArrayList<MetaData>();
		for (MetaData meta : getMetas(null)) {
			String storyAuthor = meta.getAuthor();
			if (author == null || author.equalsIgnoreCase(storyAuthor)) {
				list.add(meta);
			}
		}

		Collections.sort(list);
		return list;
	}

	/**
	 * Retrieve a {@link MetaData} corresponding to the given {@link Story},
	 * cover image <b>MAY</b> not be included.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * 
	 * @return the corresponding {@link Story}
	 */
	public synchronized MetaData getInfo(String luid) {
		if (luid != null) {
			for (MetaData meta : getMetas(null)) {
				if (luid.equals(meta.getLuid())) {
					return meta;
				}
			}
		}

		return null;
	}

	/**
	 * Retrieve a specific {@link Story}.
	 * 
	 * @param luid
	 *            the Library UID of the story
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the corresponding {@link Story} or NULL if not found
	 */
	public synchronized Story getStory(String luid, Progress pg) {
		if (pg == null) {
			pg = new Progress();
		}

		Progress pgGet = new Progress();
		Progress pgProcess = new Progress();

		pg.setMinMax(0, 2);
		pg.addProgress(pgGet, 1);
		pg.addProgress(pgProcess, 1);

		Story story = null;
		for (MetaData meta : getMetas(null)) {
			if (meta.getLuid().equals(luid)) {
				File file = getFile(luid, pgGet);
				pgGet.done();
				try {
					SupportType type = SupportType.valueOfAllOkUC(meta
							.getType());
					URL url = file.toURI().toURL();
					if (type != null) {
						story = BasicSupport.getSupport(type, url) //
								.process(pgProcess);

						// Because we do not want to clear the meta cache:
						meta.setCover(story.getMeta().getCover());
						meta.setResume(story.getMeta().getResume());
						story.setMeta(meta);
						//
					} else {
						throw new IOException("Unknown type: " + meta.getType());
					}
				} catch (IOException e) {
					// We should not have not-supported files in the
					// library
					Instance.getTraceHandler().error(
							new IOException("Cannot load file from library: "
									+ file, e));
				} finally {
					pgProcess.done();
					pg.done();
				}

				break;
			}
		}

		return story;
	}

	/**
	 * Import the {@link Story} at the given {@link URL} into the
	 * {@link BasicLibrary}.
	 * 
	 * @param url
	 *            the {@link URL} to import
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the imported {@link Story}
	 * 
	 * @throws UnknownHostException
	 *             if the host is not supported
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Story imprt(URL url, Progress pg) throws IOException {
		if (pg == null)
			pg = new Progress();

		pg.setMinMax(0, 1000);
		Progress pgProcess = new Progress();
		Progress pgSave = new Progress();
		pg.addProgress(pgProcess, 800);
		pg.addProgress(pgSave, 200);

		BasicSupport support = BasicSupport.getSupport(url);
		if (support == null) {
			throw new UnknownHostException("" + url);
		}

		Story story = save(support.process(pgProcess), pgSave);
		pg.done();

		return story;
	}

	/**
	 * Import the story from one library to another, and keep the same LUID.
	 * 
	 * @param other
	 *            the other library to import from
	 * @param luid
	 *            the Library UID
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void imprt(BasicLibrary other, String luid, Progress pg)
			throws IOException {
		Progress pgGetStory = new Progress();
		Progress pgSave = new Progress();
		if (pg == null) {
			pg = new Progress();
		}

		pg.setMinMax(0, 2);
		pg.addProgress(pgGetStory, 1);
		pg.addProgress(pgSave, 1);

		Story story = other.getStory(luid, pgGetStory);
		if (story != null) {
			story = this.save(story, luid, pgSave);
			pg.done();
		} else {
			pg.done();
			throw new IOException("Cannot find story in Library: " + luid);
		}
	}

	/**
	 * Export the {@link Story} to the given target in the given format.
	 * 
	 * @param luid
	 *            the {@link Story} ID
	 * @param type
	 *            the {@link OutputType} to transform it to
	 * @param target
	 *            the target to save to
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the saved resource (the main saved {@link File})
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public File export(String luid, OutputType type, String target, Progress pg)
			throws IOException {
		Progress pgGetStory = new Progress();
		Progress pgOut = new Progress();
		if (pg != null) {
			pg.setMax(2);
			pg.addProgress(pgGetStory, 1);
			pg.addProgress(pgOut, 1);
		}

		BasicOutput out = BasicOutput.getOutput(type, false, false);
		if (out == null) {
			throw new IOException("Output type not supported: " + type);
		}

		Story story = getStory(luid, pgGetStory);
		if (story == null) {
			throw new IOException("Cannot find story to export: " + luid);
		}

		return out.process(story, target, pgOut);
	}

	/**
	 * Save a {@link Story} to the {@link BasicLibrary}.
	 * 
	 * @param story
	 *            the {@link Story} to save
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the same {@link Story}, whose LUID may have changed
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Story save(Story story, Progress pg) throws IOException {
		return save(story, null, pg);
	}

	/**
	 * Save a {@link Story} to the {@link BasicLibrary} -- the LUID <b>must</b>
	 * be correct, or NULL to get the next free one.
	 * <p>
	 * Will override any previous {@link Story} with the same LUID.
	 * 
	 * @param story
	 *            the {@link Story} to save
	 * @param luid
	 *            the <b>correct</b> LUID or NULL to get the next free one
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @return the same {@link Story}, whose LUID may have changed
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public synchronized Story save(Story story, String luid, Progress pg)
			throws IOException {

		Instance.getTraceHandler().trace(
				this.getClass().getSimpleName() + ": saving story " + luid);

		// Do not change the original metadata, but change the original story
		MetaData meta = story.getMeta().clone();
		story.setMeta(meta);

		if (luid == null || luid.isEmpty()) {
			meta.setLuid(String.format("%03d", getNextId()));
		} else {
			meta.setLuid(luid);
		}

		if (luid != null && getInfo(luid) != null) {
			delete(luid);
		}

		story = doSave(story, pg);

		updateInfo(story.getMeta());

		Instance.getTraceHandler().trace(
				this.getClass().getSimpleName() + ": story saved (" + luid
						+ ")");

		return story;
	}

	/**
	 * Delete the given {@link Story} from this {@link BasicLibrary}.
	 * 
	 * @param luid
	 *            the LUID of the target {@link Story}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public synchronized void delete(String luid) throws IOException {
		Instance.getTraceHandler().trace(
				this.getClass().getSimpleName() + ": deleting story " + luid);

		doDelete(luid);
		invalidateInfo(luid);

		Instance.getTraceHandler().trace(
				this.getClass().getSimpleName() + ": story deleted (" + luid
						+ ")");
	}

	/**
	 * Change the type (source) of the given {@link Story}.
	 * 
	 * @param luid
	 *            the {@link Story} LUID
	 * @param newSource
	 *            the new source
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not found
	 */
	public synchronized void changeSource(String luid, String newSource,
			Progress pg) throws IOException {
		MetaData meta = getInfo(luid);
		if (meta == null) {
			throw new IOException("Story not found: " + luid);
		}

		changeSTA(luid, newSource, meta.getTitle(), meta.getAuthor(), pg);
	}

	/**
	 * Change the title (name) of the given {@link Story}.
	 * 
	 * @param luid
	 *            the {@link Story} LUID
	 * @param newTitle
	 *            the new title
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not found
	 */
	public synchronized void changeTitle(String luid, String newTitle,
			Progress pg) throws IOException {
		MetaData meta = getInfo(luid);
		if (meta == null) {
			throw new IOException("Story not found: " + luid);
		}

		changeSTA(luid, meta.getSource(), newTitle, meta.getAuthor(), pg);
	}

	/**
	 * Change the author of the given {@link Story}.
	 * 
	 * @param luid
	 *            the {@link Story} LUID
	 * @param newAuthor
	 *            the new author
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not found
	 */
	public synchronized void changeAuthor(String luid, String newAuthor,
			Progress pg) throws IOException {
		MetaData meta = getInfo(luid);
		if (meta == null) {
			throw new IOException("Story not found: " + luid);
		}

		changeSTA(luid, meta.getSource(), meta.getTitle(), newAuthor, pg);
	}

	/**
	 * Change the Source, Title and Author of the {@link Story} in one single
	 * go.
	 * 
	 * @param luid
	 *            the {@link Story} LUID
	 * @param newSource
	 *            the new source
	 * @param newTitle
	 *            the new title
	 * @param newAuthor
	 *            the new author
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not found
	 */
	protected synchronized void changeSTA(String luid, String newSource,
			String newTitle, String newAuthor, Progress pg) throws IOException {
		MetaData meta = getInfo(luid);
		if (meta == null) {
			throw new IOException("Story not found: " + luid);
		}

		meta.setSource(newSource);
		meta.setTitle(newTitle);
		meta.setAuthor(newAuthor);
		saveMeta(meta, pg);
	}

	/**
	 * Save back the current state of the {@link MetaData} (LUID <b>MUST NOT</b>
	 * change) for this {@link Story}.
	 * <p>
	 * By default, delete the old {@link Story} then recreate a new
	 * {@link Story}.
	 * <p>
	 * Note that this behaviour can lead to data loss in case of problems!
	 * 
	 * @param meta
	 *            the new {@link MetaData} (LUID <b>MUST NOT</b> change)
	 * @param pg
	 *            the optional {@link Progress}
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the {@link Story} was not found
	 */
	protected synchronized void saveMeta(MetaData meta, Progress pg)
			throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		Progress pgGet = new Progress();
		Progress pgSet = new Progress();
		pg.addProgress(pgGet, 50);
		pg.addProgress(pgSet, 50);

		Story story = getStory(meta.getLuid(), pgGet);
		if (story == null) {
			throw new IOException("Story not found: " + meta.getLuid());
		}

		// TODO: this is not safe!
		delete(meta.getLuid());
		story.setMeta(meta);
		save(story, meta.getLuid(), pgSet);

		pg.done();
	}
}
