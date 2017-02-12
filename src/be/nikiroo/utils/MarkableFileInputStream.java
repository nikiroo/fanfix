package be.nikiroo.utils;

import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * This is a markable (and thus reset-able) stream that you can create from a
 * FileInputStream.
 * 
 * @author niki
 */
public class MarkableFileInputStream extends FilterInputStream {
	private FileChannel channel;
	private long mark = 0;

	/**
	 * Create a new {@link MarkableFileInputStream} from this stream.
	 * 
	 * @param in
	 *            the original {@link FileInputStream} to wrap
	 */
	public MarkableFileInputStream(FileInputStream in) {
		super(in);
		channel = in.getChannel();
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public synchronized void mark(int readlimit) {
		try {
			mark = channel.position();
		} catch (IOException ex) {
			ex.printStackTrace();
			mark = -1;
		}
	}

	@Override
	public synchronized void reset() throws IOException {
		if (mark < 0) {
			throw new IOException("mark position not valid");
		}
		channel.position(mark);
	}
}