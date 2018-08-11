package be.nikiroo.fanfix.library;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
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
	 * It will return the same result, but those will be saved to disk at the
	 * same time to be fetched quicker the next time.
	 * 
	 * @param cacheDir
	 *            the cache directory where to save the files to disk
	 * @param lib
	 *            the original library to wrap
	 */
	public CacheLibrary(File cacheDir, BasicLibrary lib) {
		this.cacheLib = new LocalLibrary(cacheDir, Instance.getUiConfig()
				.getString(UiConfig.GUI_NON_IMAGES_DOCUMENT_TYPE), Instance
				.getUiConfig().getString(UiConfig.GUI_IMAGES_DOCUMENT_TYPE),
				true);
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
	protected List<MetaData> getMetas(Progress pg) {
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
	public synchronized File getFile(final String luid, Progress pg) {
		if (pg == null) {
			pg = new Progress();
		}

		Progress pgImport = new Progress();
		Progress pgGet = new Progress();
		Progress pgRecall = new Progress();

		pg.setMinMax(0, 5);
		pg.addProgress(pgImport, 3);
		pg.addProgress(pgGet, 1);
		pg.addProgress(pgRecall, 1);

		if (!isCached(luid)) {
			try {
				cacheLib.imprt(lib, luid, pgImport);
				updateInfo(cacheLib.getInfo(luid));
				pgImport.done();
			} catch (IOException e) {
				Instance.getTraceHandler().error(e);
			}

			pgImport.done();
			pgGet.done();
		}

		File file = cacheLib.getFile(luid, pgRecall);
		pgRecall.done();

		pg.done();
		return file;
	}

	@Override
	public Image getCover(final String luid) {
		if (isCached(luid)) {
			return cacheLib.getCover(luid);
		}

		// We could update the cache here, but it's not easy
		return lib.getCover(luid);
	}

	@Override
	public Image getSourceCover(String source) {
		// no cache for the source cover
		return lib.getSourceCover(source);
	}

	@Override
	public void setSourceCover(String source, String luid) {
		lib.setSourceCover(source, luid);
		cacheLib.setSourceCover(source, getSourceCover(source));
	}

	@Override
	protected void updateInfo(MetaData meta) {
		if (meta != null && metas != null) {
			for (int i = 0; i < metas.size(); i++) {
				if (metas.get(i).getLuid().equals(meta.getLuid())) {
					metas.set(i, meta);
				}
			}
		}

		cacheLib.updateInfo(meta);
		lib.updateInfo(meta);
	}

	@Override
	protected void deleteInfo(String luid) {
		if (luid == null) {
			metas = null;
		} else if (metas != null) {
			for (int i = 0; i < metas.size(); i++) {
				if (metas.get(i).getLuid().equals(luid)) {
					metas.remove(i--);
				}
			}
		}

		cacheLib.deleteInfo(luid);
		lib.deleteInfo(luid);
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

		MetaData meta = getInfo(luid);
		if (meta != null) {
			metas.remove(meta);
		}
	}

	@Override
	public synchronized void changeSource(String luid, String newSource,
			Progress pg) throws IOException {
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
			cacheLib.changeSource(luid, newSource, pgCache);
		}
		pgCache.done();

		lib.changeSource(luid, newSource, pgOrig);
		pgOrig.done();

		meta.setSource(newSource);
		pg.done();
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
		return cacheLib.getInfo(luid) != null;
	}

	/**
	 * Clear the {@link Story} from the cache.
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
	public Story imprt(URL url, Progress pg) throws IOException {
		if (pg == null) {
			pg = new Progress();
		}

		Progress pgImprt = new Progress();
		Progress pgCache = new Progress();
		pg.setMinMax(0, 10);
		pg.addProgress(pgImprt, 7);
		pg.addProgress(pgCache, 3);

		Story story = lib.imprt(url, pgImprt);
		cacheLib.save(story, story.getMeta().getLuid(), pgCache);

		updateInfo(story.getMeta());

		pg.done();
		return story;
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
