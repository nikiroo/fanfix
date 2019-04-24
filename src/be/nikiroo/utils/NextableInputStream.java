package be.nikiroo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class NextableInputStream extends InputStream {
	private List<NextableInputStreamStep> steps = new ArrayList<NextableInputStreamStep>();
	private NextableInputStreamStep step = null;

	private InputStream in;
	private boolean eof;
	private int pos = 0;
	private int len = 0;
	private byte[] buffer = new byte[4096];

	public NextableInputStream(InputStream in) {
		this.in = in;
	}

	public void addStep(NextableInputStreamStep step) {
		steps.add(step);
	}

	public boolean next() {
		if (!hasMoreData() && step != null) {
			len = step.getResumeLen();
			pos += step.getSkip();
			eof = false;
			step = null;
			
			checkNexts(false);

			return true;
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

	private void preRead() throws IOException {
		if (!eof && in != null && pos >= len && step == null) {
			pos = 0;
			len = in.read(buffer);
			checkNexts(true);
		}

		if (pos >= len) {
			eof = true;
		}
	}

	private boolean hasMoreData() {
		return !(eof && pos >= len);
	}

	private void checkNexts(boolean newBuffer) {
		if (!eof) {
			for (NextableInputStreamStep step : steps) {
				if (newBuffer) {
					step.clearBuffer();
				}

				int stopAt = step.stop(buffer, pos, len);
				if (stopAt >= 0) {
					this.step = step;
					len = stopAt;
					eof = true;
					break;
				}
			}
		}
	}
}
