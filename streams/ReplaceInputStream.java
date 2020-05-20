package be.nikiroo.utils.streams;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import be.nikiroo.utils.StringUtils;

/**
 * This {@link InputStream} will change some of its content by replacing it with
 * something else.
 * 
 * @author niki
 */
public class ReplaceInputStream extends BufferedInputStream {
	/**
	 * The minimum size of the internal buffer (could be more if at least one of
	 * the 'FROM' bytes arrays is &gt; 2048 bytes &mdash; in that case the
	 * buffer will be twice the largest size of the 'FROM' bytes arrays).
	 * <p>
	 * This is a different buffer than the one from the inherited class.
	 */
	static private final int MIN_BUFFER_SIZE = 4096;

	private byte[][] froms;
	private byte[][] tos;
	private int bufferSize;
	private int maxFromSize;

	/**
	 * Create a {@link ReplaceInputStream} that will replace <tt>from</tt> with
	 * <tt>to</tt>.
	 * 
	 * @param in
	 *            the under-laying {@link InputStream}
	 * @param from
	 *            the {@link String} to replace
	 * @param to
	 *            the {@link String} to replace with
	 */
	public ReplaceInputStream(InputStream in, String from, String to) {
		this(in, StringUtils.getBytes(from), StringUtils.getBytes(to));
	}

	/**
	 * Create a {@link ReplaceInputStream} that will replace <tt>from</tt> with
	 * <tt>to</tt>.
	 * 
	 * @param in
	 *            the under-laying {@link InputStream}
	 * @param from
	 *            the value to replace
	 * @param to
	 *            the value to replace with
	 */
	public ReplaceInputStream(InputStream in, byte[] from, byte[] to) {
		this(in, new byte[][] { from }, new byte[][] { to });
	}

	/**
	 * Create a {@link ReplaceInputStream} that will replace all <tt>froms</tt>
	 * with <tt>tos</tt>.
	 * <p>
	 * Note that they will be replaced in order, and that for each <tt>from</tt>
	 * a <tt>to</tt> must correspond.
	 * 
	 * @param in
	 *            the under-laying {@link InputStream}
	 * @param froms
	 *            the values to replace
	 * @param tos
	 *            the values to replace with
	 */
	public ReplaceInputStream(InputStream in, String[] froms, String[] tos) {
		this(in, StreamUtils.getBytes(froms), StreamUtils.getBytes(tos));
	}

	/**
	 * Create a {@link ReplaceInputStream} that will replace all <tt>froms</tt>
	 * with <tt>tos</tt>.
	 * <p>
	 * Note that they will be replaced in order, and that for each <tt>from</tt>
	 * a <tt>to</tt> must correspond.
	 * 
	 * @param in
	 *            the under-laying {@link InputStream}
	 * @param froms
	 *            the values to replace
	 * @param tos
	 *            the values to replace with
	 */
	public ReplaceInputStream(InputStream in, byte[][] froms, byte[][] tos) {
		super(in);

		if (froms.length != tos.length) {
			throw new IllegalArgumentException(
					"For replacing, each FROM must have a corresponding TO");
		}

		this.froms = froms;
		this.tos = tos;

		maxFromSize = 0;
		for (int i = 0; i < froms.length; i++) {
			maxFromSize = Math.max(maxFromSize, froms[i].length);
		}

		int maxToSize = 0;
		for (int i = 0; i < tos.length; i++) {
			maxToSize = Math.max(maxToSize, tos[i].length);
		}

		// We need at least maxFromSize so we can iterate and replace
		bufferSize = Math.max(4 * Math.max(maxToSize, maxFromSize),
				MIN_BUFFER_SIZE);
	}

	@Override
	protected boolean preRead() throws IOException {
		boolean rep = super.preRead();
		start = stop;
		return rep;
	}

	@Override
	protected int read(InputStream in, byte[] buffer) throws IOException {
		buffer = null; // do not use the buffer.

		byte[] newBuffer = new byte[bufferSize];
		int read = 0;
		while (read < bufferSize / 2) {
			int thisTime = in.read(newBuffer, read, bufferSize / 2 - read);
			if (thisTime <= 0) {
				break;
			}
			read += thisTime;
		}

		List<byte[]> bbBuffers = new ArrayList<byte[]>();
		List<Integer> bbOffsets = new ArrayList<Integer>();
		List<Integer> bbLengths = new ArrayList<Integer>();

		int offset = 0;
		for (int i = 0; i < read; i++) {
			for (int fromIndex = 0; fromIndex < froms.length; fromIndex++) {
				byte[] from = froms[fromIndex];
				byte[] to = tos[fromIndex];

				if (from.length > 0
						&& StreamUtils.startsWith(from, newBuffer, i, read)) {
					if (i - offset > 0) {
						bbBuffers.add(newBuffer);
						bbOffsets.add(offset);
						bbLengths.add(i - offset);
					}

					if (to.length > 0) {
						bbBuffers.add(to);
						bbOffsets.add(0);
						bbLengths.add(to.length);
					}

					i += from.length;
					offset = i;
				}
			}
		}

		if (offset < read) {
			bbBuffers.add(newBuffer);
			bbOffsets.add(offset);
			bbLengths.add(read - offset);
		}

		for (int i = bbBuffers.size() - 1; i >= 0; i--) {
			// DEBUG("pushback", bbBuffers.get(i), bbOffsets.get(i),
			// bbLengths.get(i));
			pushback(bbBuffers.get(i), bbOffsets.get(i), bbLengths.get(i));
		}

		return read;
	}

	// static public void DEBUG(String title, byte[] b, int off, int len) {
	// String str = new String(b,off,len);
	// if(str.length()>20) {
	// str=str.substring(0,10)+" ...
	// "+str.substring(str.length()-10,str.length());
	// }
	// }

	@Override
	public String toString() {
		StringBuilder rep = new StringBuilder();
		rep.append(getClass().getSimpleName()).append("\n");

		for (int i = 0; i < froms.length; i++) {
			byte[] from = froms[i];
			byte[] to = tos[i];

			rep.append("\t");
			rep.append("bytes[").append(from.length).append("]");
			if (from.length <= 20) {
				rep.append(" (").append(new String(from)).append(")");
			}
			rep.append(" -> ");
			rep.append("bytes[").append(to.length).append("]");
			if (to.length <= 20) {
				rep.append(" (").append(new String(to)).append(")");
			}
			rep.append("\n");
		}

		return "[" + rep + "]";
	}
}
