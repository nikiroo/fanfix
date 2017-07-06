package be.nikiroo.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class offer some utilities based around Streams.
 * 
 * @author niki
 */
public class IOUtils {
	/**
	 * Write the data to the given {@link File}.
	 * 
	 * @param in
	 *            the data source
	 * @param target
	 *            the target {@link File}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void write(InputStream in, File target) throws IOException {
		OutputStream out = new FileOutputStream(target);
		try {
			write(in, out);
		} finally {
			out.close();
		}
	}

	/**
	 * Write the data to the given {@link OutputStream}.
	 * 
	 * @param in
	 *            the data source
	 * @param out
	 *            the target {@link OutputStream}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void write(InputStream in, OutputStream out)
			throws IOException {
		byte buffer[] = new byte[4069];
		for (int len = 0; (len = in.read(buffer)) > 0;) {
			out.write(buffer, 0, len);
		}
	}

	/**
	 * Recursively Add a {@link File} (which can thus be a directory, too) to a
	 * {@link ZipOutputStream}.
	 * 
	 * @param zip
	 *            the stream
	 * @param base
	 *            the path to prepend to the ZIP info before the actual
	 *            {@link File} path
	 * @param target
	 *            the source {@link File} (which can be a directory)
	 * @param targetIsRoot
	 *            FALSE if we need to add a {@link ZipEntry} for base/target,
	 *            TRUE to add it at the root of the ZIP
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void zip(ZipOutputStream zip, String base, File target,
			boolean targetIsRoot) throws IOException {
		if (target.isDirectory()) {
			if (!targetIsRoot) {
				if (base == null || base.isEmpty()) {
					base = target.getName();
				} else {
					base += "/" + target.getName();
				}
				zip.putNextEntry(new ZipEntry(base + "/"));
			}
			for (File file : target.listFiles()) {
				zip(zip, base, file, false);
			}
		} else {
			if (base == null || base.isEmpty()) {
				base = target.getName();
			} else {
				base += "/" + target.getName();
			}
			zip.putNextEntry(new ZipEntry(base));
			FileInputStream in = new FileInputStream(target);
			try {
				IOUtils.write(in, zip);
			} finally {
				in.close();
			}
		}
	}

	/**
	 * Zip the given source into dest.
	 * 
	 * @param src
	 *            the source {@link File} (which can be a directory)
	 * @param dest
	 *            the destination <tt>.zip</tt> file
	 * @param srcIsRoot
	 *            FALSE if we need to add a {@link ZipEntry} for src, TRUE to
	 *            add it at the root of the ZIP
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void zip(File src, File dest, boolean srcIsRoot)
			throws IOException {
		OutputStream out = new FileOutputStream(dest);
		try {
			ZipOutputStream zip = new ZipOutputStream(out);
			try {
				IOUtils.zip(zip, "", src, srcIsRoot);
			} finally {
				zip.close();
			}
		} finally {
			out.close();
		}
	}

	/**
	 * Write the {@link String} content to {@link File}.
	 * 
	 * @param dir
	 *            the directory where to write the {@link File}
	 * @param filename
	 *            the {@link File} name
	 * @param content
	 *            the content
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void writeSmallFile(File dir, String filename, String content)
			throws IOException {
		if (!dir.exists()) {
			dir.mkdirs();
		}

		FileWriter writerVersion = new FileWriter(new File(dir, filename));
		try {
			writerVersion.write(content);
		} finally {
			writerVersion.close();
		}
	}

	/**
	 * Read the whole {@link File} content into a {@link String}.
	 * 
	 * @param file
	 *            the {@link File}
	 * 
	 * @return the content
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static String readSmallFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			StringBuilder builder = new StringBuilder();
			for (String line = reader.readLine(); line != null; line = reader
					.readLine()) {
				builder.append(line);
			}
			return builder.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * Recursively delete the given {@link File}, which may of course also be a
	 * directory.
	 * <p>
	 * Will silently continue in case of error.
	 * 
	 * @param target
	 *            the target to delete
	 */
	public static void deltree(File target) {
		File[] files = target.listFiles();
		if (files != null) {
			for (File file : files) {
				deltree(file);
			}
		}

		if (!target.delete()) {
			System.err.println("Cannot delete: " + target.getAbsolutePath());
		}
	}

	/**
	 * Open the given /-separated resource (from the binary root).
	 * 
	 * @param name
	 *            the resource name
	 * 
	 * @return the opened resource if found, NLL if not
	 */
	public static InputStream openResource(String name) {
		ClassLoader loader = IOUtils.class.getClassLoader();
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}

		return loader.getResourceAsStream(name);
	}
}
