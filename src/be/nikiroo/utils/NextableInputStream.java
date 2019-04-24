package be.nikiroo.utils;

import java.io.IOException;
import java.io.InputStream;

public class NextableInputStream extends InputStream {
	private InputStream in;
	private boolean eof;
	private int pos = 0;
	private int len = 0;
	private byte[] buffer = new byte[4096];

	public NextableInputStream(InputStream in) {
		this.in = in;
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
		while (!eof && done < blen) {
			preRead();
			for (int i = pos; i < blen && i < len; i++) {
				b[boff + done] = buffer[i];
				pos++;
				done++;
			}
		}

		return done > 0 ? done : -1;
	}

	private void preRead() throws IOException {
		if (in != null && !eof && pos >= len) {
			pos = 0;
			len = in.read(buffer);
			// checkNexts();
		}

		if (pos >= len) {
			eof = true;
		}
	}
}
