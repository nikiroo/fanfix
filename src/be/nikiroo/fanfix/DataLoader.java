package be.nikiroo.fanfix;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;

import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.utils.Cache;
import be.nikiroo.utils.Downloader;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.ImageUtils;

/**
 * This cache will manage Internet (and local) downloads, as well as put the
 * downloaded files into a cache.
 * <p>
 * As long the cached resource is not too old, it will use it instead of
 * retrieving the file again.
 * 
 * @author niki
 */
public class DataLoader {
	private Cache cache;
	private Downloader downloader;

	/**
	 * Create a new {@link DataLoader} object.
	 * 
	 * @param dir
	 *            the directory to use as cache
	 * @param UA
	 *            the User-Agent to use to download the resources
	 * @param hoursChanging
	 *            the number of hours after which a cached file that is thought
	 *            to change ~often is considered too old (or -1 for
	 *            "never too old")
	 * @param hoursStable
	 *            the number of hours after which a LARGE cached file that is
	 *            thought to change rarely is considered too old (or -1 for
	 *            "never too old")
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public DataLoader(File dir, String UA, int hoursChanging, int hoursStable)
			throws IOException {
		cache = new Cache(dir, hoursChanging, hoursStable);
		cache.setTraceHandler(Instance.getTraceHandler());
		downloader = new Downloader(UA);
		downloader.setTraceHandler(Instance.getTraceHandler());
	}

	/**
	 * Open a resource (will load it from the cache if possible, or save it into
	 * the cache after downloading if not).
	 * 
	 * @param url
	 *            the resource to open
	 * @param support
	 *            the support to use to download the resource
	 * @param stable
	 *            TRUE for more stable resources, FALSE when they often change
	 * 
	 * @return the opened resource, NOT NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream open(URL url, BasicSupport support, boolean stable)
			throws IOException {
		// MUST NOT return null
		return open(url, support, stable, url);
	}

	/**
	 * Open a resource (will load it from the cache if possible, or save it into
	 * the cache after downloading if not).
	 * <p>
	 * The cached resource will be assimilated to the given original {@link URL}
	 * 
	 * @param url
	 *            the resource to open
	 * @param support
	 *            the support to use to download the resource
	 * @param stable
	 *            TRUE for more stable resources, FALSE when they often change
	 * @param originalUrl
	 *            the original {@link URL} used to locate the cached resource
	 * 
	 * @return the opened resource, NOT NULL
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream open(URL url, BasicSupport support, boolean stable,
			URL originalUrl) throws IOException {
		// MUST NOT return null
		try {
			InputStream in = cache.load(originalUrl, false, stable);
			Instance.getTraceHandler().trace(
					"Cache " + (in != null ? "hit" : "miss") + ": " + url);

			if (in == null) {
				try {
					in = openNoCache(url, support, null, null, null);
					cache.save(in, originalUrl);
					// ..But we want a resetable stream
					in.close();
					in = cache.load(originalUrl, false, stable);
				} catch (IOException e) {
					throw new IOException("Cannot save the url: "
							+ (url == null ? "null" : url.toString()), e);
				}
			}

			return in;
		} catch (IOException e) {
			throw new IOException("Cannot open the url: "
					+ (url == null ? "null" : url.toString()), e);
		}
	}

	/**
	 * Open the given {@link URL} without using the cache, but still update the
	 * cookies.
	 * 
	 * @param url
	 *            the {@link URL} to open
	 * 
	 * @return the {@link InputStream} of the opened page
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream openNoCache(URL url) throws IOException {
		return downloader.open(url);
	}

	/**
	 * Open the given {@link URL} without using the cache, but still using and
	 * updating the cookies.
	 * 
	 * @param url
	 *            the {@link URL} to open
	 * @param support
	 *            the {@link BasicSupport} used for the cookies
	 * @param postParams
	 *            the POST parameters
	 * @param getParams
	 *            the GET parameters (priority over POST)
	 * @param oauth
	 *            OAuth authorization (aka, "bearer XXXXXXX")
	 * 
	 * @return the {@link InputStream} of the opened page
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream openNoCache(URL url, BasicSupport support,
			Map<String, String> postParams, Map<String, String> getParams,
			String oauth) throws IOException {

		Map<String, String> cookiesValues = null;
		URL currentReferer = url;
		if (support != null) {
			cookiesValues = support.getCookies();
			currentReferer = support.getCurrentReferer();
			// priority: arguments
			if (oauth == null) {
				oauth = support.getOAuth();
			}
		}

		return downloader.open(url, currentReferer, cookiesValues, postParams,
				getParams, oauth);
	}

	/**
	 * Refresh the resource into cache if needed.
	 * 
	 * @param url
	 *            the resource to open
	 * @param support
	 *            the support to use to download the resource
	 * @param stable
	 *            TRUE for more stable resources, FALSE when they often change
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void refresh(URL url, BasicSupport support, boolean stable)
			throws IOException {
		if (!cache.check(url, false, stable)) {
			open(url, support, stable).close();
		}
	}

	/**
	 * Check the resource to see if it is in the cache.
	 * 
	 * @param url
	 *            the resource to check
	 * @param stable
	 *            a stable file (that dones't change too often) -- parameter
	 *            used to check if the file is too old to keep or not
	 * 
	 * @return TRUE if it is
	 * 
	 */
	public boolean check(URL url, boolean stable) {
		return cache.check(url, false, stable);
	}

	/**
	 * Save the given resource as an image on disk using the default image
	 * format for content or cover -- will automatically add the extension, too.
	 * 
	 * @param img
	 *            the resource
	 * @param target
	 *            the target file without extension
	 * @param cover
	 *            use the cover image format instead of the content image format
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void saveAsImage(Image img, File target, boolean cover)
			throws IOException {
		String format;
		if (cover) {
			format = Instance.getConfig().getString(Config.IMAGE_FORMAT_COVER)
					.toLowerCase();
		} else {
			format = Instance.getConfig()
					.getString(Config.IMAGE_FORMAT_CONTENT).toLowerCase();
		}
		saveAsImage(img, new File(target.toString() + "." + format), format);
	}

	/**
	 * Save the given resource as an image on disk using the given image format
	 * for content, or with "png" format if it fails.
	 * 
	 * @param img
	 *            the resource
	 * @param target
	 *            the target file
	 * @param format
	 *            the file format ("png", "jpeg", "bmp"...)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void saveAsImage(Image img, File target, String format)
			throws IOException {
		ImageUtils.getInstance().saveAsImage(img, target, format);

	}

	/**
	 * Manually add this item to the cache.
	 * 
	 * @param in
	 *            the input data
	 * @param uniqueID
	 *            a unique ID for this resource
	 * 
	 * @return the resulting {@link File}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public File addToCache(InputStream in, String uniqueID) throws IOException {
		return cache.save(in, uniqueID);
	}

	/**
	 * Return the {@link InputStream} corresponding to the given unique ID, or
	 * NULL if none found.
	 * 
	 * @param uniqueID
	 *            the unique ID
	 * 
	 * @return the content or NULL
	 */
	public InputStream getFromCache(String uniqueID) {
		return cache.load(uniqueID, true, true);
	}

	/**
	 * Remove the given resource from the cache.
	 * 
	 * @param uniqueID
	 *            a unique ID used to locate the cached resource
	 * 
	 * @return TRUE if it was removed
	 */
	public boolean removeFromCache(String uniqueID) {
		return cache.remove(uniqueID);
	}

	/**
	 * Clean the cache (delete the cached items).
	 * 
	 * @param onlyOld
	 *            only clean the files that are considered too old
	 * 
	 * @return the number of cleaned items
	 */
	public int cleanCache(boolean onlyOld) {
		return cache.clean(onlyOld);
	}
}
