package be.nikiroo.utils.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * A simple {@link InputStream} that is buffered with a bytes array.
 * <p>
 * It is mostly intended to be used as a base class to create new
 * {@link InputStream}s with special operation modes, and to give some default
 * methods.
 * 
 * @author niki
 */
public class BufferedInputStream extends InputStream {
	/** The current position in the buffer. */
	protected int start;
	/** The index of the last usable position of the buffer. */
	protected int stop;
	/** The buffer itself. */
	protected byte[] buffer;
	/** An End-Of-File (or buffer, here) marker. */
	protected boolean eof;

	private boolean closed;
	private InputStream in;
	private int openCounter;

	// special use, prefetched next buffer
	private byte[] buffer2;
	private int pos2;
	private int len2;
	private byte[] originalBuffer;

	private long bytesRead;

	/**
	 * Create a new {@link BufferedInputStream} that wraps the given
	 * {@link InputStream}.
	 * 
	 * @param in
	 *            the {@link InputStream} to wrap
	 */
	public BufferedInputStream(InputStream in) {
		this.in = in;

		this.buffer = new byte[4096];
		this.originalBuffer = this.buffer;
		this.start = 0;
		this.stop = 0;
	}

	/**
	 * Create a new {@link BufferedInputStream} that wraps the given bytes array
	 * as a data source.
	 * 
	 * @param in
	 *            the array to wrap, cannot be NULL
	 */
	public BufferedInputStream(byte[] in) {
		this(in, 0, in.length);
	}

	/**
	 * Create a new {@link BufferedInputStream} that wraps the given bytes array
	 * as a data source.
	 * 
	 * @param in
	 *            the array to wrap, cannot be NULL
	 * @param offset
	 *            the offset to start the reading at
	 * @param length
	 *            the number of bytes to take into account in the array,
	 *            starting from the offset
	 * 
	 * @throws NullPointerException
	 *             if the array is NULL
	 * @throws IndexOutOfBoundsException
	 *             if the offset and length do not correspond to the given array
	 */
	public BufferedInputStream(byte[] in, int offset, int length) {
		if (in == null) {
			throw new NullPointerException();
		} else if (offset < 0 || length < 0 || length > in.length - offset) {
			throw new IndexOutOfBoundsException();
		}

		this.in = null;

		this.buffer = in;
		this.originalBuffer = this.buffer;
		this.start = offset;
		this.stop = length;
	}

	/**
	 * Return this very same {@link BufferedInputStream}, but keep a counter of
	 * how many streams were open this way. When calling
	 * {@link BufferedInputStream#close()}, decrease this counter if it is not
	 * already zero instead of actually closing the stream.
	 * <p>
	 * You are now responsible for it &mdash; you <b>must</b> close it.
	 * <p>
	 * This method allows you to use a wrapping stream around this one and still
	 * close the wrapping stream.
	 * 
	 * @return the same stream, but you are now responsible for closing it
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the stream is closed
	 */
	public synchronized InputStream open() throws IOException {
		checkClose();
		openCounter++;
		return this;
	}

	/**
	 * Check if the current content (what will be read next) starts with the
	 * given search term.
	 * <p>
	 * Note: the search term size <b>must</b> be smaller or equal the internal
	 * buffer size.
	 * 
	 * @param search
	 *            the term to search for
	 * 
	 * @return TRUE if the content that will be read starts with it
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the size of the search term is
	 *             greater than the internal buffer
	 */
	public boolean startsWiths(String search) throws IOException {
		return startsWith(search.getBytes("UTF-8"));
	}

	/**
	 * Check if the current content (what will be read next) starts with the
	 * given search term.
	 * <p>
	 * Note: the search term size <b>must</b> be smaller or equal the internal
	 * buffer size.
	 * 
	 * @param search
	 *            the term to search for
	 * 
	 * @return TRUE if the content that will be read starts with it
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the size of the search term is
	 *             greater than the internal buffer
	 */
	public boolean startsWith(byte[] search) throws IOException {
		if (search.length > originalBuffer.length) {
			throw new IOException(
					"This stream does not support searching for more than "
							+ buffer.length + " bytes");
		}

		checkClose();

		if (available() < search.length) {
			preRead();
		}

		if (available() >= search.length) {
			// Easy path
			return StreamUtils.startsWith(search, buffer, start, stop);
		} else if (!eof) {
			// Harder path
			if (buffer2 == null && buffer.length == originalBuffer.length) {
				buffer2 = Arrays.copyOf(buffer, buffer.length * 2);

				pos2 = buffer.length;
				len2 = in.read(buffer2, pos2, buffer.length);
				if (len2 > 0) {
					bytesRead += len2;
				}

				// Note: here, len/len2 = INDEX of last good byte
				len2 += pos2;
			}

			return StreamUtils.startsWith(search, buffer2, pos2, len2);
		}

		return false;
	}

	/**
	 * The number of bytes read from the under-laying {@link InputStream}.
	 * 
	 * @return the number of bytes
	 */
	public long getBytesRead() {
		return bytesRead;
	}

	/**
	 * Check if this stream is totally spent (no more data to read or to
	 * process).
	 * 
	 * @return TRUE if it is
	 */
	public boolean eof() {
		return closed || (stop < 0 && !hasMoreData());
	}

	@Override
	public int read() throws IOException {
		checkClose();

		preRead();
		if (eof) {
			return -1;
		}

		return buffer[start++];
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int boff, int blen) throws IOException {
		checkClose();

		if (b == null) {
			throw new NullPointerException();
		} else if (boff < 0 || blen < 0 || blen > b.length - boff) {
			throw new IndexOutOfBoundsException();
		} else if (blen == 0) {
			return 0;
		}

		int done = 0;
		while (hasMoreData() && done < blen) {
			preRead();
			if (hasMoreData()) {
				int now = Math.min(blen, stop) - start;
				if (now > 0) {
					System.arraycopy(buffer, start, b, boff + done, now);
					start += now;
					done += now;
				}
			}
		}

		return done > 0 ? done : -1;
	}

	@Override
	public long skip(long n) throws IOException {
		if (n <= 0) {
			return 0;
		}

		long skipped = 0;
		while (hasMoreData() && n > 0) {
			preRead();

			long inBuffer = Math.min(n, available());
			start += inBuffer;
			n -= inBuffer;
			skipped += inBuffer;
		}

		return skipped;
	}

	@Override
	public int available() {
		if (closed) {
			return 0;
		}

		return Math.max(0, stop - start);
	}

	/**
	 * Closes this stream and releases any system resources associated with the
	 * stream.
	 * <p>
	 * Including the under-laying {@link InputStream}.
	 * <p>
	 * <b>Note:</b> if you called the {@link BufferedInputStream#open()} method
	 * prior to this one, it will just decrease the internal count of how many
	 * open streams it held and do nothing else. The stream will actually be
	 * closed when you have called {@link BufferedInputStream#close()} once more
	 * than {@link BufferedInputStream#open()}.
	 * 
	 * @exception IOException
	 *                in case of I/O error
	 */
	@Override
	public synchronized void close() throws IOException {
		close(true);
	}

	/**
	 * Closes this stream and releases any system resources associated with the
	 * stream.
	 * <p>
	 * Including the under-laying {@link InputStream} if
	 * <tt>incudingSubStream</tt> is true.
	 * <p>
	 * You can call this method multiple times, it will not cause an
	 * {@link IOException} for subsequent calls.
	 * <p>
	 * <b>Note:</b> if you called the {@link BufferedInputStream#open()} method
	 * prior to this one, it will just decrease the internal count of how many
	 * open streams it held and do nothing else. The stream will actually be
	 * closed when you have called {@link BufferedInputStream#close()} once more
	 * than {@link BufferedInputStream#open()}.
	 * 
	 * @param includingSubStream
	 *            also close the under-laying stream
	 * 
	 * @exception IOException
	 *                in case of I/O error
	 */
	public synchronized void close(boolean includingSubStream)
			throws IOException {
		if (!closed) {
			if (openCounter > 0) {
				openCounter--;
			} else {
				closed = true;
				if (includingSubStream && in != null) {
					in.close();
				}
			}
		}
	}

	/**
	 * Check if we still have some data in the buffer and, if not, fetch some.
	 * 
	 * @return TRUE if we fetched some data, FALSE if there are still some in
	 *         the buffer
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected boolean preRead() throws IOException {
		boolean hasRead = false;
		if (!eof && in != null && start >= stop) {
			start = 0;
			if (buffer2 != null) {
				buffer = buffer2;
				start = pos2;
				stop = len2;

				buffer2 = null;
				pos2 = 0;
				len2 = 0;
			} else {
				buffer = originalBuffer;

				stop = read(in, buffer);
				if (stop > 0) {
					bytesRead += stop;
				}
			}

			hasRead = true;
		}

		if (start >= stop) {
			eof = true;
		}

		return hasRead;
	}

	/**
	 * Read the under-laying stream into the local buffer.
	 * 
	 * @param in
	 *            the under-laying {@link InputStream}
	 * @param buffer
	 *            the buffer we use in this {@link BufferedInputStream}
	 * 
	 * @return the number of bytes read
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected int read(InputStream in, byte[] buffer) throws IOException {
		return in.read(buffer);
	}

	/**
	 * We have more data available in the buffer or we can fetch more.
	 * 
	 * @return TRUE if it is the case, FALSE if not
	 */
	protected boolean hasMoreData() {
		return !closed && !(eof && start >= stop);
	}

	/**
	 * Check that the stream was not closed, and throw an {@link IOException} if
	 * it was.
	 * 
	 * @throws IOException
	 *             if it was closed
	 */
	protected void checkClose() throws IOException {
		if (closed) {
			throw new IOException(
					"This BufferedInputStream was closed, you cannot use it anymore.");
		}
	}
}
