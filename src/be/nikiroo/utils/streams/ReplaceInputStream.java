package be.nikiroo.utils.streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * This {@link InputStream} will change some of its content by replacing it with
 * something else.
 * 
 * @author niki
 */
public class ReplaceInputStream extends BufferedInputStream {
	private byte[] from;
	private byte[] to;

	private byte[] source;
	private int spos;
	private int slen;

	/**
	 * Create a {@link ReplaceInputStream} that will replace <tt>from</tt> with
	 * <tt>to</tt>.
	 * 
	 * @param in
	 *            the under-laying {@link InputStream}
	 * @param from
	 *            the {@link String} to replace
	 * @param to
	 *            the {@link String} to replace with
	 */
	public ReplaceInputStream(InputStream in, String from, String to) {
		this(in, StreamUtils.bytes(from), StreamUtils.bytes(to));
	}

	/**
	 * Create a {@link ReplaceInputStream} that will replace <tt>from</tt> with
	 * <tt>to</tt>.
	 * 
	 * @param in
	 *            the under-laying {@link InputStream}
	 * @param from
	 *            the value to replace
	 * @param to
	 *            the value to replace with
	 */
	public ReplaceInputStream(InputStream in, byte[] from, byte[] to) {
		super(in);
		this.from = from;
		this.to = to;

		source = new byte[4096];
		spos = 0;
		slen = 0;
	}

	@Override
	protected int read(InputStream in, byte[] buffer) throws IOException {
		if (buffer.length < to.length || source.length < to.length * 2) {
			throw new IOException(
					"An underlaying buffer is too small for this replace value");
		}

		if (spos >= slen) {
			spos = 0;
			slen = in.read(source);
		}

		// Note: very simple, not efficient implementation, sorry.
		int count = 0;
		while (spos < slen && count < buffer.length - to.length) {
			if (from.length > 0
					&& StreamUtils.startsWith(from, source, spos, slen)) {
				System.arraycopy(to, 0, buffer, spos, to.length);
				count += to.length;
				spos += from.length;
			} else {
				buffer[count++] = source[spos++];
			}
		}

		return count;
	}
}
