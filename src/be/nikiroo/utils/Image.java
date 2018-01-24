package be.nikiroo.utils;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class represents an image data.
 * 
 * @author niki
 */
public class Image implements Closeable {
	static private TempFiles tmpRepository;
	static private long count = 0;
	static private Object lock = new Object();

	private File data;

	/**
	 * Do not use -- for serialisation purposes only.
	 */
	@SuppressWarnings("unused")
	private Image() {
	}

	/**
	 * Create a new {@link Image} with the given data.
	 * 
	 * @param data
	 *            the data
	 */
	public Image(byte[] data) {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		try {
			this.data = getTemporaryFile();
			IOUtils.write(in, this.data);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Create a new {@link Image} from its Base64 representation.
	 * 
	 * @param base64
	 *            the {@link Image} in Base64 format
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Image(String base64) throws IOException {
		this(Base64.decode(base64));
	}

	/**
	 * Create a new {@link Image} from a stream.
	 * 
	 * @param in
	 *            the stream
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Image(InputStream in) throws IOException {
		data = getTemporaryFile();
		IOUtils.write(in, data);
	}

	/**
	 * <b>Read</b> the actual image data, as a byte array.
	 * 
	 * @return the image data
	 */
	public byte[] getData() {
		try {
			FileInputStream in = new FileInputStream(data);
			try {
				return IOUtils.toByteArray(in);
			} finally {
				in.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convert the given {@link Image} object into a Base64 representation of
	 * the same {@link Image} object.
	 * 
	 * @return the Base64 representation
	 */
	public String toBase64() {
		return Base64.encodeBytes(getData());
	}

	/**
	 * Closing the {@link Image} will delete the associated temporary file on
	 * disk.
	 * <p>
	 * Note that even if you don't, the program will still <b>try</b> to delete
	 * all the temporary files at JVM termination.
	 */
	@Override
	public void close() throws IOException {
		data.delete();
		synchronized (lock) {
			count--;
			if (count <= 0) {
				count = 0;
				tmpRepository.close();
				tmpRepository = null;
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

	/**
	 * Return a newly created temporary file to work on.
	 * 
	 * @return the file
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	private File getTemporaryFile() throws IOException {
		synchronized (lock) {
			if (tmpRepository == null) {
				tmpRepository = new TempFiles("images");
				count = 0;
			}

			count++;

			return tmpRepository.createTempFile("image");
		}
	}
}
