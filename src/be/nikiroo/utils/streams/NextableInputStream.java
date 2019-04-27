package be.nikiroo.utils.streams;

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
public class NextableInputStream extends BufferedInputStream {
	private NextableInputStreamStep step;
	private boolean started;
	private boolean stopped;

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
		super(in);
		this.step = step;
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
		super(in, offset, length);
		this.step = step;
		checkBuffer(true);
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

	/**
	 * Check if this stream is totally spent (no more data to read or to
	 * process).
	 * <p>
	 * Note: an empty stream that is still not started will return FALSE, as we
	 * don't know yet if it is empty.
	 * 
	 * @return TRUE if it is
	 */
	@Override
	public boolean eof() {
		return super.eof();
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
	@Override
	protected boolean preRead() throws IOException {
		if (!stopped) {
			boolean bufferChanged = super.preRead();
			checkBuffer(true);
			return bufferChanged;
		}

		if (start >= stop) {
			eof = true;
		}

		return false;
	}

	/**
	 * We have more data available in the buffer or we can fetch more.
	 * 
	 * @return TRUE if it is the case, FALSE if not
	 */
	@Override
	protected boolean hasMoreData() {
		return started && super.hasMoreData();
	}

	/**
	 * Check that the buffer didn't overshot to the next item, and fix
	 * {@link NextableInputStream#stop} if needed.
	 * <p>
	 * If {@link NextableInputStream#stop} is fixed,
	 * {@link NextableInputStream#eof} and {@link NextableInputStream#stopped}
	 * are set to TRUE.
	 * 
	 * @param newBuffer
	 *            we changed the buffer, we need to clear some information in
	 *            the {@link NextableInputStreamStep}
	 */
	private void checkBuffer(boolean newBuffer) {
		if (step != null && stop > 0) {
			if (newBuffer) {
				step.clearBuffer();
			}

			int stopAt = step.stop(buffer, start, stop);
			if (stopAt >= 0) {
				stop = stopAt;
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
			stop = step.getResumeLen();
			start += step.getResumeSkip();
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
}
