package be.nikiroo.utils;

import java.io.IOException;
import java.io.InputStream;

/**
 * This class represents an image data.
 * 
 * @author niki
 */
public class Image {
	private byte[] data;

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
		this.data = data;
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
		this.data = IOUtils.toByteArray(in);
	}

	/**
	 * The actual image data.
	 * <p>
	 * This is the actual data, not a copy, so any change made here will be
	 * reflected into the {@link Image} and vice-versa.
	 * 
	 * @return the image data
	 */
	public byte[] getData() {
		return data;
	}

	/**
	 * Convert the given {@link Image} object into a Base64 representation of
	 * the same {@link Image} object.
	 * 
	 * @return the Base64 representation
	 */
	public String toBase64() {
		return new String(Base64.encodeBytes(getData()));
	}
}
