package be.nikiroo.fanfix;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.imageio.ImageIO;

import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.supported.BasicSupport;
import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.ImageUtils;
import be.nikiroo.utils.MarkableFileInputStream;

/**
 * This cache will manage Internet (and local) downloads, as well as put the
 * downloaded files into a cache.
 * <p>
 * As long the cached resource is not too old, it will use it instead of
 * retrieving the file again.
 * 
 * @author niki
 */
public class Cache {
	private File dir;
	private String UA;
	private long tooOldChanging;
	private long tooOldStable;
	private CookieManager cookies;

	/**
	 * Create a new {@link Cache} object.
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
	public Cache(File dir, String UA, int hoursChanging, int hoursStable)
			throws IOException {
		this.dir = dir;
		this.UA = UA;
		this.tooOldChanging = 1000 * 60 * 60 * hoursChanging;
		this.tooOldStable = 1000 * 60 * 60 * hoursStable;

		if (dir != null) {
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}

		if (dir == null || !dir.exists()) {
			throw new IOException("Cannot create the cache directory: "
					+ (dir == null ? "null" : dir.getAbsolutePath()));
		}

		cookies = new CookieManager();
		cookies.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
		CookieHandler.setDefault(cookies);
	}

	/**
	 * Clear all the cookies currently in the jar.
	 */
	public void clearCookies() {
		cookies.getCookieStore().removeAll();
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
			InputStream in = load(originalUrl, false, stable);
			if (in == null) {
				try {
					save(url, support, originalUrl);
				} catch (IOException e) {
					throw new IOException("Cannot save the url: "
							+ (url == null ? "null" : url.toString()), e);
				}

				// Was just saved, can load old, so, will not be null
				in = load(originalUrl, true, stable);
			}

			return in;
		} catch (IOException e) {
			throw new IOException("Cannot open the url: "
					+ (url == null ? "null" : url.toString()), e);
		}
	}

	/**
	 * Open the given {@link URL} without using the cache, but still using and
	 * updating the cookies.
	 * 
	 * @param url
	 *            the {@link URL} to open
	 * @param support
	 *            the {@link BasicSupport} used for the cookies
	 * 
	 * @return the {@link InputStream} of the opened page
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream openNoCache(URL url, BasicSupport support)
			throws IOException {
		return openNoCache(url, support, url, null);
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
	 * 
	 * @return the {@link InputStream} of the opened page
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public InputStream openNoCache(URL url, BasicSupport support,
			Map<String, String> postParams) throws IOException {
		return openNoCache(url, support, url, postParams);
	}

	/**
	 * Open the given {@link URL} without using the cache, but still using and
	 * updating the cookies.
	 * 
	 * @param url
	 *            the {@link URL} to open
	 * @param support
	 *            the {@link BasicSupport} used for the cookies
	 * @param originalUrl
	 *            the original {@link URL} before any redirection occurs
	 * @param postParams
	 *            the POST parameters
	 * 
	 * @return the {@link InputStream} of the opened page
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private InputStream openNoCache(URL url, BasicSupport support,
			final URL originalUrl, Map<String, String> postParams)
			throws IOException {

		URLConnection conn = openConnectionWithCookies(url, support);
		if (postParams != null && conn instanceof HttpURLConnection) {
			StringBuilder postData = new StringBuilder();
			for (Map.Entry<String, String> param : postParams.entrySet()) {
				if (postData.length() != 0)
					postData.append('&');
				postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				postData.append('=');
				postData.append(URLEncoder.encode(
						String.valueOf(param.getValue()), "UTF-8"));
			}

			conn.setDoOutput(true);
			((HttpURLConnection) conn).setRequestMethod("POST");
			conn.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");
			conn.setRequestProperty("charset", "utf-8");

			OutputStreamWriter writer = new OutputStreamWriter(
					conn.getOutputStream());

			writer.write(postData.toString());
			writer.flush();
			writer.close();
		}

		conn.connect();

		// Check if redirect
		if (conn instanceof HttpURLConnection
				&& ((HttpURLConnection) conn).getResponseCode() / 100 == 3) {
			String newUrl = conn.getHeaderField("Location");
			return openNoCache(new URL(newUrl), support, originalUrl,
					postParams);
		}

		InputStream in = conn.getInputStream();
		if ("gzip".equals(conn.getContentEncoding())) {
			in = new GZIPInputStream(in);
		}

		return in;
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
		File cached = getCached(url);
		if (cached.exists() && !isOld(cached, stable)) {
			return;
		}

		open(url, support, stable).close();
	}

	/**
	 * Check the resource to see if it is in the cache.
	 * 
	 * @param url
	 *            the resource to check
	 * 
	 * @return TRUE if it is
	 * 
	 */
	public boolean check(URL url) {
		return getCached(url).exists();
	}

	/**
	 * Save the given resource as an image on disk using the default image
	 * format for content.
	 * 
	 * @param url
	 *            the resource
	 * @param target
	 *            the target file
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void saveAsImage(URL url, File target) throws IOException {
		URL cachedUrl = new URL(url.toString());
		File cached = getCached(cachedUrl);

		if (!cached.exists() || isOld(cached, true)) {
			InputStream imageIn = open(url, null, true);
			ImageIO.write(ImageUtils.fromStream(imageIn), Instance.getConfig()
					.getString(Config.IMAGE_FORMAT_CONTENT).toLowerCase(),
					cached);
		}

		IOUtils.write(new FileInputStream(cached), target);
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
		File file = getCached(uniqueID);
		IOUtils.write(in, file);
		return file;
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
		File file = getCached(uniqueID);
		if (file.exists()) {
			try {
				return new MarkableFileInputStream(new FileInputStream(file));
			} catch (FileNotFoundException e) {
			}
		}

		return null;
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
		int num = 0;
		for (File file : dir.listFiles()) {
			if (!onlyOld || isOld(file, true)) {
				if (file.delete()) {
					num++;
				} else {
					System.err.println("Cannot delete temporary file: "
							+ file.getAbsolutePath());
				}
			}
		}
		return num;
	}

	/**
	 * Open a resource from the cache if it exists.
	 * 
	 * @param url
	 *            the resource to open
	 * @param allowTooOld
	 *            allow files even if they are considered too old
	 * @param stable
	 *            a stable file (that dones't change too often) -- parameter
	 *            used to check if the file is too old to keep or not
	 * 
	 * @return the opened resource if found, NULL i not
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private InputStream load(URL url, boolean allowTooOld, boolean stable)
			throws IOException {
		File cached = getCached(url);
		if (cached.exists() && (allowTooOld || !isOld(cached, stable))) {
			return new MarkableFileInputStream(new FileInputStream(cached));
		}

		return null;
	}

	/**
	 * Save the given resource to the cache.
	 * 
	 * @param url
	 *            the resource
	 * @param support
	 *            the {@link BasicSupport} used to download it
	 * @param originalUrl
	 *            the original {@link URL} used to locate the cached resource
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private void save(URL url, BasicSupport support, URL originalUrl)
			throws IOException {
		InputStream in = openNoCache(url, support, originalUrl, null);
		try {
			File cached = getCached(originalUrl);
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(cached));
			try {
				byte[] buf = new byte[4096];
				int len;
				while ((len = in.read(buf)) > 0) {
					out.write(buf, 0, len);
				}
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
	}

	/**
	 * Open a connection on the given {@link URL}, and manage the cookies that
	 * come with it.
	 * 
	 * @param url
	 *            the {@link URL} to open
	 * @param support
	 *            the {@link BasicSupport} to use for cookie generation
	 * 
	 * @return the connection
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private URLConnection openConnectionWithCookies(URL url,
			BasicSupport support) throws IOException {
		URLConnection conn = url.openConnection();

		conn.setRequestProperty("User-Agent", UA);
		conn.setRequestProperty("Cookie", generateCookies(support));
		conn.setRequestProperty("Accept-Encoding", "gzip");
		if (support != null && support.getCurrentReferer() != null) {
			conn.setRequestProperty("Referer", support.getCurrentReferer()
					.toString());
			conn.setRequestProperty("Host", support.getCurrentReferer()
					.getHost());
		}

		return conn;
	}

	/**
	 * Check if the {@link File} is too old according to
	 * {@link Cache#tooOldChanging}.
	 * 
	 * @param file
	 *            the file to check
	 * @param stable
	 *            TRUE to denote files that are not supposed to change too often
	 * 
	 * @return TRUE if it is
	 */
	private boolean isOld(File file, boolean stable) {
		long max = tooOldChanging;
		if (stable) {
			max = tooOldStable;
		}

		if (max < 0) {
			return false;
		}

		long time = new Date().getTime() - file.lastModified();
		if (time < 0) {
			System.err.println("Timestamp in the future for file: "
					+ file.getAbsolutePath());
		}

		return time < 0 || time > max;
	}

	/**
	 * Get the cache resource from the cache if it is present for this
	 * {@link URL}.
	 * 
	 * @param url
	 *            the url
	 * 
	 * @return the cached version if present, NULL if not
	 */
	private File getCached(URL url) {
		String name = url.getHost();
		if (name == null || name.isEmpty()) {
			name = url.getFile();
		} else {
			name = url.toString();
		}

		return getCached(name);
	}

	/**
	 * Get the cache resource from the cache if it is present for this unique
	 * ID.
	 * 
	 * @param uniqueID
	 *            the id
	 * 
	 * @return the cached version if present, NULL if not
	 */
	private File getCached(String uniqueID) {
		uniqueID = uniqueID.replace('/', '_').replace(':', '_')
				.replace("\\", "_");

		return new File(dir, uniqueID);
	}

	/**
	 * Generate the cookie {@link String} from the local {@link CookieStore} so
	 * it is ready to be passed.
	 * 
	 * @return the cookie
	 */
	private String generateCookies(BasicSupport support) {
		StringBuilder builder = new StringBuilder();
		for (HttpCookie cookie : cookies.getCookieStore().getCookies()) {
			if (builder.length() > 0) {
				builder.append(';');
			}

			// TODO: check if format is ok
			builder.append(cookie.toString());
		}

		if (support != null) {
			try {
				for (Map.Entry<String, String> set : support.getCookies()
						.entrySet()) {
					if (builder.length() > 0) {
						builder.append(';');
					}
					builder.append(set.getKey());
					builder.append('=');
					builder.append(set.getValue());
				}
			} catch (IOException e) {
				Instance.syserr(e);
			}
		}

		return builder.toString();
	}
}
