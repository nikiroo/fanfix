package be.nikiroo.utils.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import be.nikiroo.utils.StringUtils;

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
	/**
	 * The size of the internal buffer (can be different if you pass your own
	 * buffer, of course, and can also expand to search for longer "startsWith"
	 * data).
	 * <p>
	 * Note that special "push-back" buffers can also be created during the life
	 * of this stream.
	 */
	static private final int BUFFER_SIZE = 4096;

	/** The current position in the buffer. */
	protected int start;
	/** The index of the last usable position of the buffer. */
	protected int stop;
	/** The buffer itself. */
	protected byte[] buffer;
	/** An End-Of-File (or {@link InputStream}, here) marker. */
	protected boolean eof;

	private boolean closed;
	private InputStream in;
	private int openCounter;
	private byte[] singleByteReader = new byte[1];

	/** array + offset of pushed-back buffers */
	private List<Entry<byte[], Integer>> backBuffers;

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

		this.buffer = new byte[BUFFER_SIZE];
		this.start = 0;
		this.stop = 0;
		this.backBuffers = new ArrayList<Entry<byte[], Integer>>();
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
		this.start = offset;
		this.stop = length;
		this.backBuffers = new ArrayList<Entry<byte[], Integer>>();
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
	 * Check if the current content (until eof) is equal to the given search
	 * term.
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
	public boolean is(String search) throws IOException {
		return is(StringUtils.getBytes(search));
	}

	/**
	 * Check if the current content (until eof) is equal to the given search
	 * term.
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
	public boolean is(byte[] search) throws IOException {
		if (startsWith(search)) {
			return available() == search.length;
		}

		return false;
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
	public boolean startsWith(String search) throws IOException {
		return startsWith(StringUtils.getBytes(search));
	}

	/**
	 * Check if the current content (what will be read next) starts with the
	 * given search term.
	 * <p>
	 * An empty string will always return true (unless the stream is closed,
	 * which would throw an {@link IOException}).
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
		checkClose();

		while (consolidatePushBack(search.length) < search.length) {
			preRead();
			if (start >= stop) {
				// Not enough data left to start with that
				return false;
			}

			byte[] newBuffer = new byte[stop - start];
			System.arraycopy(buffer, start, newBuffer, 0, stop - start);
			pushback(newBuffer, 0);
			start = stop;
		}

		Entry<byte[], Integer> bb = backBuffers.get(backBuffers.size() - 1);
		byte[] bbBuffer = bb.getKey();
		int bbOffset = bb.getValue();

		return StreamUtils.startsWith(search, bbBuffer, bbOffset,
				bbBuffer.length);
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
	 * Check if this stream is spent (no more data to read or to process).
	 * 
	 * @return TRUE if it is
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public boolean eof() throws IOException {
		if (closed) {
			return true;
		}

		preRead();
		return !hasMoreData();
	}

	/**
	 * Read the whole {@link InputStream} until the end and return the number of
	 * bytes read.
	 * 
	 * @return the number of bytes read
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public long end() throws IOException {
		long skipped = 0;
		while (hasMoreData()) {
			skipped += skip(buffer.length);
		}

		return skipped;
	}

	@Override
	public int read() throws IOException {
		if (read(singleByteReader) < 0) {
			return -1;
		}

		return singleByteReader[0];
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

		// Read from the pushed-back buffers if any
		if (backBuffers.isEmpty()) {
			preRead(); // an implementation could pushback in preRead()
		}
		
		if (!backBuffers.isEmpty()) {
			int read = 0;

			Entry<byte[], Integer> bb = backBuffers
					.remove(backBuffers.size() - 1);
			byte[] bbBuffer = bb.getKey();
			int bbOffset = bb.getValue();
			int bbSize = bbBuffer.length - bbOffset;
			
			if (bbSize > blen) {
				read = blen;
				System.arraycopy(bbBuffer, bbOffset, b, boff, read);
				pushback(bbBuffer, bbOffset + read);
			} else {
				read = bbSize;
				System.arraycopy(bbBuffer, bbOffset, b, boff, read);
			}

			return read;
		}

		int done = 0;
		while (hasMoreData() && done < blen) {
			preRead();
			if (hasMoreData()) {
				int now = Math.min(blen - done, stop - start);
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
		while (!backBuffers.isEmpty() && n > 0) {
			Entry<byte[], Integer> bb = backBuffers
					.remove(backBuffers.size() - 1);
			byte[] bbBuffer = bb.getKey();
			int bbOffset = bb.getValue();
			int bbSize = bbBuffer.length - bbOffset;

			int localSkip = 0;
			localSkip = (int) Math.min(n, bbSize);

			n -= localSkip;
			bbSize -= localSkip;

			if (bbSize > 0) {
				pushback(bbBuffer, bbOffset + localSkip);
			}
		}
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

		int avail = 0;
		for (Entry<byte[], Integer> entry : backBuffers) {
			avail += entry.getKey().length - entry.getValue();
		}

		return avail + Math.max(0, stop - start);
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
	 * Consolidate the push-back buffers so the last one is at least the given
	 * size, if possible.
	 * <p>
	 * If there is not enough data in the push-back buffers, they will all be
	 * consolidated.
	 * 
	 * @param size
	 *            the minimum size of the consolidated buffer, or -1 to force
	 *            the consolidation of all push-back buffers
	 * 
	 * @return the size of the last, consolidated buffer; can be less than the
	 *         requested size if not enough data
	 */
	protected int consolidatePushBack(int size) {
		int bbIndex = -1;
		int bbUpToSize = 0;
		for (Entry<byte[], Integer> entry : backBuffers) {
			bbIndex++;
			bbUpToSize += entry.getKey().length - entry.getValue();

			if (size >= 0 && bbUpToSize >= size) {
				break;
			}
		}

		// Index 0 means "the last buffer is already big enough"
		if (bbIndex > 0) {
			byte[] consolidatedBuffer = new byte[bbUpToSize];
			int consolidatedPos = 0;
			for (int i = 0; i <= bbIndex; i++) {
				Entry<byte[], Integer> bb = backBuffers
						.remove(backBuffers.size() - 1);
				byte[] bbBuffer = bb.getKey();
				int bbOffset = bb.getValue();
				int bbSize = bbBuffer.length - bbOffset;
				System.arraycopy(bbBuffer, bbOffset, consolidatedBuffer,
						consolidatedPos, bbSize);
			}

			pushback(consolidatedBuffer, 0);
		}

		return bbUpToSize;
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
		if (in != null && !eof && start >= stop) {
			start = 0;
			stop = read(in, buffer);
			if (stop > 0) {
				bytesRead += stop;
			}

			hasRead = true;
		}

		if (start >= stop) {
			eof = true;
		}

		return hasRead;
	}

	/**
	 * Push back some data that will be read again at the next read call.
	 * 
	 * @param buffer
	 *            the buffer to push back
	 * @param offset
	 *            the offset at which to start reading in the buffer
	 */
	protected void pushback(byte[] buffer, int offset) {
		backBuffers.add(
				new AbstractMap.SimpleEntry<byte[], Integer>(buffer, offset));
	}

	/**
	 * Push back some data that will be read again at the next read call.
	 * 
	 * @param buffer
	 *            the buffer to push back
	 * @param offset
	 *            the offset at which to start reading in the buffer
	 * @param len
	 *            the length to copy
	 */
	protected void pushback(byte[] buffer, int offset, int len) {
		// TODO: not efficient!
		if (buffer.length != len) {
			byte[] lenNotSupportedYet = new byte[len];
			System.arraycopy(buffer, offset, lenNotSupportedYet, 0, len);
			buffer = lenNotSupportedYet;
			offset = 0;
		}

		pushback(buffer, offset);
	}

	/**
	 * Read the under-laying stream into the given local buffer.
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
		return in.read(buffer, 0, buffer.length);
	}

	/**
	 * We have more data available in the buffer <b>or</b> we can, maybe, fetch
	 * more.
	 * 
	 * @return TRUE if it is the case, FALSE if not
	 */
	protected boolean hasMoreData() {
		if (closed) {
			return false;
		}

		return !backBuffers.isEmpty() || (start < stop) || !eof;
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
