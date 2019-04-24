package be.nikiroo.utils;

import java.io.IOException;
import java.io.InputStream;

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
	private boolean stopped;

	private InputStream in;
	private boolean eof;
	private int pos = 0;
	private int len = 0;
	private byte[] buffer = new byte[4096];

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
	 *             in case of I/O error
	 */
	public boolean next() throws IOException {
		if (!hasMoreData() && stopped) {
			len = step.getResumeLen();
			pos += step.getResumeSkip();
			eof = false;

			if (!preRead()) {
				checkBuffer(false);
			}

			// consider that if EOF, there is no next
			return hasMoreData();
		}

		return false;
	}

	@Override
	public int read() throws IOException {
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
	public int available() throws IOException {
		return Math.max(0, len - pos);
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
			len = in.read(buffer);
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
		return !(eof && pos >= len);
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
		if (step != null) {
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
}
