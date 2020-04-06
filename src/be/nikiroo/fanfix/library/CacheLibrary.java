package be.nikiroo.fanfix.library;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.bundles.UiConfigBundle;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.Progress;

/**
 * This library will cache another pre-existing {@link BasicLibrary}.
 * 
 * @author niki
 */
public class CacheLibrary extends BasicLibrary {
	private List<MetaData> metas;
	private BasicLibrary lib;
	private LocalLibrary cacheLib;

	/**
	 * Create a cache library around the given one.
	 * <p>
	 * It will return the same result, but those will be saved to disk at the same
	 * time to be fetched quicker the next time.
	 * 
	 * @param cacheDir the cache directory where to save the files to disk
	 * @param lib      the original library to wrap
	 * @param config   the configuration used to know which kind of default
	 *                 {@link OutputType} to use for images and non-images stories
	 */
	public CacheLibrary(File cacheDir, BasicLibrary lib, UiConfigBundle config) {
		this.cacheLib = new LocalLibrary(cacheDir, //
				config.getString(UiConfig.GUI_NON_IMAGES_DOCUMENT_TYPE),
				config.getString(UiConfig.GUI_IMAGES_DOCUMENT_TYPE), true);
		this.lib = lib;
	}

	@Override
	public String getLibraryName() {
		return lib.getLibraryName();
	}

	@Override
	public Status getStatus() {
		return lib.getStatus();
	}

	@Override
	protected List<MetaData> getMetas(Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		if (metas == null) {
			metas = lib.getMetas(pg);
		}

		pg.done();
		return metas;
	}

	@Override
	public synchronized MetaData getInfo(String luid) throws IOException {
		MetaData info = cacheLib.getInfo(luid);
		if (info == null) {
			info = lib.getInfo(luid);
		}

		return info;
	}

	@Override
	public synchronized Story getStory(String luid, MetaData meta, Progress pg)
			throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		Progress pgImport = new Progress();
		Progress pgGet = new Progress();

		pg.setMinMax(0, 4);
		pg.addProgress(pgImport, 3);
		pg.addProgress(pgGet, 1);

		if (!isCached(luid)) {
			try {
				cacheLib.imprt(lib, luid, pgImport);
				updateInfo(cacheLib.getInfo(luid));
				pgImport.done();
			} catch (IOException e) {
				Instance.getInstance().getTraceHandler().error(e);
			}

			pgImport.done();
			pgGet.done();
		}

		String type = cacheLib.getOutputType(meta.isImageDocument());
		MetaData cachedMeta = meta.clone();
		cachedMeta.setType(type);

		return cacheLib.getStory(luid, cachedMeta, pg);
	}

	@Override
	public synchronized File getFile(final String luid, Progress pg)
			throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		Progress pgGet = new Progress();
		Progress pgRecall = new Progress();

		pg.setMinMax(0, 5);
		pg.addProgress(pgGet, 4);
		pg.addProgress(pgRecall, 1);

		if (!isCached(luid)) {
			getStory(luid, pgGet);
			pgGet.done();
		}

		File file = cacheLib.getFile(luid, pgRecall);
		pgRecall.done();

		pg.done();
		return file;
	}

	@Override
	public Image getCover(final String luid) throws IOException {
		if (isCached(luid)) {
			return cacheLib.getCover(luid);
		}

		// We could update the cache here, but it's not easy
		return lib.getCover(luid);
	}

	@Override
	public Image getSourceCover(String source) throws IOException {
		Image custom = getCustomSourceCover(source);
		if (custom != null) {
			return custom;
		}

		Image cached = cacheLib.getSourceCover(source);
		if (cached != null) {
			return cached;
		}

		return lib.getSourceCover(source);
	}

	@Override
	public Image getAuthorCover(String author) throws IOException {
		Image custom = getCustomAuthorCover(author);
		if (custom != null) {
			return custom;
		}

		Image cached = cacheLib.getAuthorCover(author);
		if (cached != null) {
			return cached;
		}

		return lib.getAuthorCover(author);
	}

	@Override
	public Image getCustomSourceCover(String source) throws IOException {
		Image custom = cacheLib.getCustomSourceCover(source);
		if (custom == null) {
			custom = lib.getCustomSourceCover(source);
			if (custom != null) {
				cacheLib.setSourceCover(source, custom);
			}
		}

		return custom;
	}

	@Override
	public Image getCustomAuthorCover(String author) throws IOException {
		Image custom = cacheLib.getCustomAuthorCover(author);
		if (custom == null) {
			custom = lib.getCustomAuthorCover(author);
			if (custom != null) {
				cacheLib.setAuthorCover(author, custom);
			}
		}

		return custom;
	}

	@Override
	public void setSourceCover(String source, String luid) throws IOException {
		lib.setSourceCover(source, luid);
		cacheLib.setSourceCover(source, getCover(luid));
	}

	@Override
	public void setAuthorCover(String author, String luid) throws IOException {
		lib.setAuthorCover(author, luid);
		cacheLib.setAuthorCover(author, getCover(luid));
	}

	@Override
	protected void updateInfo(MetaData meta) throws IOException {
		if (meta != null && metas != null) {
			boolean changed = false;
			for (int i = 0; i < metas.size(); i++) {
				if (metas.get(i).getLuid().equals(meta.getLuid())) {
					metas.set(i, meta);
					changed = true;
				}
			}

			if (!changed) {
				metas.add(meta);
			}
		}

		cacheLib.updateInfo(meta);
		lib.updateInfo(meta);
	}

	@Override
	protected void invalidateInfo(String luid) {
		if (luid == null) {
			metas = null;
		} else if (metas != null) {
			for (int i = 0; i < metas.size(); i++) {
				if (metas.get(i).getLuid().equals(luid)) {
					metas.remove(i--);
				}
			}
		}

		cacheLib.invalidateInfo(luid);
		lib.invalidateInfo(luid);
	}

	@Override
	public synchronized Story save(Story story, String luid, Progress pg)
			throws IOException {
		Progress pgLib = new Progress();
		Progress pgCacheLib = new Progress();

		if (pg == null) {
			pg = new Progress();
		}

		pg.setMinMax(0, 2);
		pg.addProgress(pgLib, 1);
		pg.addProgress(pgCacheLib, 1);

		story = lib.save(story, luid, pgLib);
		story = cacheLib.save(story, story.getMeta().getLuid(), pgCacheLib);

		updateInfo(story.getMeta());

		return story;
	}

	@Override
	public synchronized void delete(String luid) throws IOException {
		if (isCached(luid)) {
			cacheLib.delete(luid);
		}
		lib.delete(luid);

		invalidateInfo(luid);
	}

	@Override
	protected synchronized void changeSTA(String luid, String newSource,
			String newTitle, String newAuthor, Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		Progress pgCache = new Progress();
		Progress pgOrig = new Progress();
		pg.setMinMax(0, 2);
		pg.addProgress(pgCache, 1);
		pg.addProgress(pgOrig, 1);

		MetaData meta = getInfo(luid);
		if (meta == null) {
			throw new IOException("Story not found: " + luid);
		}

		if (isCached(luid)) {
			cacheLib.changeSTA(luid, newSource, newTitle, newAuthor, pgCache);
		}
		pgCache.done();

		lib.changeSTA(luid, newSource, newTitle, newAuthor, pgOrig);
		pgOrig.done();

		meta.setSource(newSource);
		meta.setTitle(newTitle);
		meta.setAuthor(newAuthor);
		pg.done();

		invalidateInfo(luid);
	}

	/**
	 * Check if the {@link Story} denoted by this Library UID is present in the
	 * cache.
	 * 
	 * @param luid
	 *            the Library UID
	 * 
	 * @return TRUE if it is
	 */
	public boolean isCached(String luid) {
		try {
			return cacheLib.getInfo(luid) != null;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Clear the {@link Story} from the cache.
	 * <p>
	 * The next time we try to retrieve the {@link Story}, it may be required to
	 * cache it again.
	 * 
	 * @param luid
	 *            the story to clear
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void clearFromCache(String luid) throws IOException {
		if (isCached(luid)) {
			cacheLib.delete(luid);
		}
	}

	@Override
	public MetaData imprt(URL url, Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		Progress pgImprt = new Progress();
		Progress pgCache = new Progress();
		pg.setMinMax(0, 10);
		pg.addProgress(pgImprt, 7);
		pg.addProgress(pgCache, 3);

		MetaData meta = lib.imprt(url, pgImprt);
		updateInfo(meta);
		
		clearFromCache(meta.getLuid());
		
		pg.done();
		return meta;
	}

	// All the following methods are only used by Save and Delete in
	// BasicLibrary:

	@Override
	protected int getNextId() {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected void doDelete(String luid) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}

	@Override
	protected Story doSave(Story story, Progress pg) throws IOException {
		throw new java.lang.InternalError("Should not have been called");
	}
}
