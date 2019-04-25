package be.nikiroo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * This {@link InputStream} can be separated into sub-streams (you can process
 * it as a normal {@link InputStream} but, when it is spent, you can call
 * {@link NextableInputStream#next()} on it to unlock new data).
 * <p>
 * The separation in sub-streams is done via {@link NextableInputStreamStep}.
 * 
 * @author niki
 */
public class NextableInputStream extends InputStream {
	private NextableInputStreamStep step;
	private boolean started;
	private boolean stopped;
	private boolean closed;

	private InputStream in;
	private int openCounter;
	private boolean eof;
	private int pos;
	private int len;
	private byte[] buffer;

	// special use, prefetched next buffer
	private byte[] buffer2;
	private int pos2;
	private int len2;
	private byte[] originalBuffer;

	private long bytesRead;

	/**
	 * Create a new {@link NextableInputStream} that wraps the given
	 * {@link InputStream}.
	 * 
	 * @param in
	 *            the {@link InputStream} to wrap
	 * @param step
	 *            how to separate it into sub-streams (can be NULL, but in that
	 *            case it will behave as a normal {@link InputStream})
	 */
	public NextableInputStream(InputStream in, NextableInputStreamStep step) {
		this.in = in;
		this.step = step;

		this.buffer = new byte[4096];
		this.originalBuffer = this.buffer;
		this.pos = 0;
		this.len = 0;
	}

	/**
	 * Create a new {@link NextableInputStream} that wraps the given bytes array
	 * as a data source.
	 * 
	 * @param in
	 *            the array to wrap, cannot be NULL
	 * @param step
	 *            how to separate it into sub-streams (can be NULL, but in that
	 *            case it will behave as a normal {@link InputStream})
	 */
	public NextableInputStream(byte[] in, NextableInputStreamStep step) {
		this(in, step, 0, in.length);
	}

	/**
	 * Create a new {@link NextableInputStream} that wraps the given bytes array
	 * as a data source.
	 * 
	 * @param in
	 *            the array to wrap, cannot be NULL
	 * @param step
	 *            how to separate it into sub-streams (can be NULL, but in that
	 *            case it will behave as a normal {@link InputStream})
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
	public NextableInputStream(byte[] in, NextableInputStreamStep step,
			int offset, int length) {
		if (in == null) {
			throw new NullPointerException();
		} else if (offset < 0 || length < 0 || length > in.length - offset) {
			throw new IndexOutOfBoundsException();
		}

		this.in = null;
		this.step = step;

		this.buffer = in;
		this.originalBuffer = this.buffer;
		this.pos = offset;
		this.len = length;

		checkBuffer(true);
	}

	/**
	 * Return this very same {@link NextableInputStream}, but keep a counter of
	 * how many streams were open this way. When calling
	 * {@link NextableInputStream#close()}, decrease this counter if it is not
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
	 * Unblock the processing of the next sub-stream.
	 * <p>
	 * It can only be called when the "current" stream is spent (i.e., you must
	 * first process the stream until it is spent).
	 * <p>
	 * We consider that when the under-laying {@link InputStream} is also spent,
	 * we cannot have a next sub-stream (it will thus return FALSE).
	 * <p>
	 * {@link IOException}s can happen when we have no data available in the
	 * buffer; in that case, we fetch more data to know if we can have a next
	 * sub-stream or not.
	 * 
	 * @return TRUE if we unblocked the next sub-stream, FALSE if not
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the stream is closed
	 */
	public boolean next() throws IOException {
		return next(false);
	}

	/**
	 * Unblock the next sub-stream as would have done
	 * {@link NextableInputStream#next()}, but disable the sub-stream systems.
	 * <p>
	 * That is, the next stream, if any, will be the last one and will not be
	 * subject to the {@link NextableInputStreamStep}.
	 * 
	 * @return TRUE if we unblocked the next sub-stream, FALSE if not
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the stream is closed
	 */
	public boolean nextAll() throws IOException {
		return next(true);
	}

	// max is buffer.size !
	public boolean startsWiths(String search) throws IOException {
		return startsWith(search.getBytes("UTF-8"));
	}

	// max is buffer.size !
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
			return startsWith(search, buffer, pos);
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

			if (available() + (len2 - pos2) >= search.length) {
				return startsWith(search, buffer2, pos2);
			}
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
		return closed || (len < 0 && !hasMoreData());
	}

	@Override
	public int read() throws IOException {
		checkClose();

		preRead();
		if (eof) {
			return -1;
		}

		return buffer[pos++];
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
				for (int i = pos; i < blen && i < len; i++) {
					b[boff + done] = buffer[i];
					pos++;
					done++;
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
			pos += inBuffer;
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

		return Math.max(0, len - pos);
	}

	/**
	 * Closes this stream and releases any system resources associated with the
	 * stream.
	 * <p>
	 * Including the under-laying {@link InputStream}.
	 * <p>
	 * <b>Note:</b> if you called the {@link NextableInputStream#open()} method
	 * prior to this one, it will just decrease the internal count of how many
	 * open streams it held and do nothing else. The stream will actually be
	 * closed when you have called {@link NextableInputStream#close()} once more
	 * than {@link NextableInputStream#open()}.
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
	 * <b>Note:</b> if you called the {@link NextableInputStream#open()} method
	 * prior to this one, it will just decrease the internal count of how many
	 * open streams it held and do nothing else. The stream will actually be
	 * closed when you have called {@link NextableInputStream#close()} once more
	 * than {@link NextableInputStream#open()}.
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
	private boolean preRead() throws IOException {
		boolean hasRead = false;
		if (!eof && in != null && pos >= len && !stopped) {
			pos = 0;
			if (buffer2 != null) {
				buffer = buffer2;
				pos = pos2;
				len = len2;

				buffer2 = null;
				pos2 = 0;
				len2 = 0;
			} else {
				buffer = originalBuffer;
				len = in.read(buffer);
				if (len > 0) {
					bytesRead += len;
				}
			}

			checkBuffer(true);
			hasRead = true;
		}

		if (pos >= len) {
			eof = true;
		}

		return hasRead;
	}

	/**
	 * We have more data available in the buffer or we can fetch more.
	 * 
	 * @return TRUE if it is the case, FALSE if not
	 */
	private boolean hasMoreData() {
		return !closed && started && !(eof && pos >= len);
	}

	/**
	 * Check that the buffer didn't overshot to the next item, and fix
	 * {@link NextableInputStream#len} if needed.
	 * <p>
	 * If {@link NextableInputStream#len} is fixed,
	 * {@link NextableInputStream#eof} and {@link NextableInputStream#stopped}
	 * are set to TRUE.
	 * 
	 * @param newBuffer
	 *            we changed the buffer, we need to clear some information in
	 *            the {@link NextableInputStreamStep}
	 */
	private void checkBuffer(boolean newBuffer) {
		if (step != null && len > 0) {
			if (newBuffer) {
				step.clearBuffer();
			}

			int stopAt = step.stop(buffer, pos, len);
			if (stopAt >= 0) {
				len = stopAt;
				eof = true;
				stopped = true;
			}
		}
	}

	/**
	 * The implementation of {@link NextableInputStream#next()} and
	 * {@link NextableInputStream#nextAll()}.
	 * 
	 * @param all
	 *            TRUE for {@link NextableInputStream#nextAll()}, FALSE for
	 *            {@link NextableInputStream#next()}
	 * 
	 * @return TRUE if we unblocked the next sub-stream, FALSE if not
	 * 
	 * @throws IOException
	 *             in case of I/O error or if the stream is closed
	 */
	private boolean next(boolean all) throws IOException {
		checkClose();

		if (!started) {
			// First call before being allowed to read
			started = true;

			if (all) {
				step = null;
			}

			return true;
		}

		if (step != null && !hasMoreData() && stopped) {
			len = step.getResumeLen();
			pos += step.getResumeSkip();
			eof = false;

			if (all) {
				step = null;
			}

			if (!preRead()) {
				checkBuffer(false);
			}

			// consider that if EOF, there is no next
			return hasMoreData();
		}

		return false;
	}

	/**
	 * Check that the stream was not closed, and throw an {@link IOException} if
	 * it was.
	 * 
	 * @throws IOException
	 *             if it was closed
	 */
	private void checkClose() throws IOException {
		if (closed) {
			throw new IOException(
					"This NextableInputStream was closed, you cannot use it anymore.");
		}
	}

	// buffer must be > search
	static private boolean startsWith(byte[] search, byte[] buffer,
			int offset) {
		boolean same = true;
		for (int i = 0; i < search.length; i++) {
			if (search[i] != buffer[offset + i]) {
				same = false;
				break;
			}
		}

		return same;
	}
}
