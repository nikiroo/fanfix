package be.nikiroo.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

public class ReplaceInputStream extends BufferedInputStream {
	private byte[] from;
	private byte[] to;

	private byte[] source;
	private int spos;
	private int slen;

	public ReplaceInputStream(InputStream in, String from, String to) {
		this(in, bytes(from), bytes(to));
	}

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
		if (buffer.length < to.length) {
			throw new IOException(
					"Underlaying buffer is too small to contain the replace String");
		}

		if (spos >= slen) {
			spos = 0;
			slen = in.read(source);
		}

		// Note: very simple, not efficient implementation, sorry.
		int count = 0;
		int i = spos;
		while (i < slen && count < buffer.length - to.length) {
			if (from.length > 0 && startsWith(from, source, spos)) {
				System.arraycopy(to, 0, buffer, spos, to.length);
				count += to.length;
				i += to.length;
				spos += to.length;
			} else {
				buffer[count++] = source[i++];
				spos++;
			}
		}

		return count;
	}

	static private byte[] bytes(String str) {
		try {
			return str.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			// All conforming JVM must support UTF-8
			e.printStackTrace();
			return null;
		}
	}
}
