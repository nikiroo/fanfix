package be.nikiroo.utils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * A small utility class to generate auto-delete temporary files in a
 * centralised location.
 * 
 * @author niki
 */
public class TempFiles implements Closeable {
	protected File root;

	/**
	 * Create a new {@link TempFiles} -- each instance is separate and have a
	 * dedicated sub-directory in a shared temporary root.
	 * <p>
	 * The whole repository will be deleted on close (if you fail to call it,
	 * the program will <b>try</b> to all it on JVM termination).
	 * 
	 * @param name
	 *            the instance name (will be <b>part</b> of the final directory
	 *            name)
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public TempFiles(String name) throws IOException {
		root = File.createTempFile(".temp", "");
		IOUtils.deltree(root, true);

		root = new File(root.getParentFile(), ".temp");
		root.mkdir();
		if (!root.exists()) {
			throw new IOException("Cannot create root directory: " + root);
		}

		root.deleteOnExit();

		root = createTempFile(name);
		IOUtils.deltree(root, true);

		root.mkdir();
		if (!root.exists()) {
			throw new IOException("Cannot create root subdirectory: " + root);
		}
	}

	/**
	 * Create an auto-delete temporary file.
	 * 
	 * @param name
	 *            a base for the final filename (only a <b>part</b> of said
	 *            filename)
	 * 
	 * @return the newly created file
	 * 
	 * @throws IOException
	 *             in case of I/O errors
	 */
	public synchronized File createTempFile(String name) throws IOException {
		name += "_";
		while (name.length() < 3) {
			name += "_";
		}

		while (true) {
			File tmp = File.createTempFile(name, "");
			IOUtils.deltree(tmp, true);

			File test = new File(root, tmp.getName());
			if (!test.exists()) {
				test.createNewFile();
				if (!test.exists()) {
					throw new IOException("Cannot create temporary file: "
							+ test);
				}

				test.deleteOnExit();
				return test;
			}
		}
	}

	/**
	 * Create an auto-delete temporary directory.
	 * <p>
	 * Note that creating 2 temporary directories with the same name will result
	 * in two <b>different</b> directories, even if the final name is the same
	 * (the absolute path will be different).
	 * 
	 * @param name
	 *            the actual directory name (not path)
	 * 
	 * @return the newly created file
	 * 
	 * @throws IOException
	 *             in case of I/O errors, or if the name was a path instead of a
	 *             name
	 */
	public synchronized File createTempDir(String name) throws IOException {
		File localRoot = createTempFile(name);
		IOUtils.deltree(localRoot, true);

		localRoot.mkdir();
		if (!localRoot.exists()) {
			throw new IOException("Cannot create subdirectory: " + localRoot);
		}

		File dir = new File(localRoot, name);
		if (!dir.getName().equals(name)) {
			throw new IOException(
					"Cannot create temporary directory with a path, only names are allowed: "
							+ dir);
		}

		dir.mkdir();
		dir.deleteOnExit();

		if (!dir.exists()) {
			throw new IOException("Cannot create subdirectory: " + dir);
		}

		return dir;
	}

	@Override
	public synchronized void close() throws IOException {
		IOUtils.deltree(root); // NO exception here
		root.getParentFile().delete(); // only if empty
		root = null;
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}
}
