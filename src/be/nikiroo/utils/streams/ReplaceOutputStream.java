package be.nikiroo.utils.streams;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This {@link OutputStream} will change some of its content by replacing it
 * with something else.
 * 
 * @author niki
 */
public class ReplaceOutputStream extends BufferedOutputStream {
	private byte[] from;
	private byte[] to;

	/**
	 * Create a {@link ReplaceOutputStream} that will replace <tt>from</tt> with
	 * <tt>to</tt>.
	 * 
	 * @param out
	 *            the under-laying {@link OutputStream}
	 * @param from
	 *            the {@link String} to replace
	 * @param to
	 *            the {@link String} to replace with
	 */
	public ReplaceOutputStream(OutputStream out, String from, String to) {
		this(out, StreamUtils.bytes(from), StreamUtils.bytes(to));
	}

	/**
	 * Create a {@link ReplaceOutputStream} that will replace <tt>from</tt> with
	 * <tt>to</tt>.
	 * 
	 * @param out
	 *            the under-laying {@link OutputStream}
	 * @param from
	 *            the value to replace
	 * @param to
	 *            the value to replace with
	 */
	public ReplaceOutputStream(OutputStream out, byte[] from, byte[] to) {
		super(out);
		bypassFlush = false;

		this.from = from;
		this.to = to;
	}

	@Override
	protected void flush(boolean includingSubStream) throws IOException {
		// Note: very simple, not efficient implementation, sorry.
		while (start < stop) {
			if (from.length > 0
					&& StreamUtils.startsWith(from, buffer, start, stop)) {
				out.write(to);
				bytesWritten += to.length;
				start += from.length;
			} else {
				out.write(buffer[start++]);
				bytesWritten++;
			}
		}

		start = 0;
		stop = 0;

		if (includingSubStream) {
			out.flush();
		}
	}
}
