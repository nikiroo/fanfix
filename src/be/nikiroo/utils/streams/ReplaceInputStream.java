package be.nikiroo.utils.streams;

import java.io.IOException;
import java.io.InputStream;

/**
 * This {@link InputStream} will change some of its content by replacing it with
 * something else.
 * 
 * @author niki
 */
public class ReplaceInputStream extends BufferedInputStream {
	private byte[][] froms;
	private byte[][] tos;
	private int maxToSize;

	private byte[] source;
	private int spos;
	private int slen;

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
		this(in, StreamUtils.bytes(from), StreamUtils.bytes(to));
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
		this(in, StreamUtils.bytes(froms), StreamUtils.bytes(tos));
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

		for (int i = 0; i < tos.length; i++) {
			maxToSize = Math.max(maxToSize, tos[i].length);
		}

		source = new byte[4096];
		spos = 0;
		slen = 0;
	}

	@Override
	protected int read(InputStream in, byte[] buffer, int off, int len)
			throws IOException {
		if (len < maxToSize || source.length < maxToSize * 2) {
			throw new IOException(
					"An underlaying buffer is too small for these replace values");
		}

		if (spos >= slen) {
			spos = 0;
			slen = in.read(source);
		}

		// Note: very simple, not efficient implementation, sorry.
		int count = 0;
		while (spos < slen && count < len - maxToSize) {
			boolean replaced = false;
			for (int i = 0; i < froms.length; i++) {
				if (froms[i] != null && froms[i].length > 0
						&& StreamUtils.startsWith(froms[i], source, spos, slen)) {
					if (tos[i] != null && tos[i].length > 0) {
						System.arraycopy(tos[i], 0, buffer, off + spos,
								tos[i].length);
						count += tos[i].length;
					}

					spos += froms[i].length;
					replaced = true;
					break;
				}
			}

			if (!replaced) {
				buffer[off + count++] = source[spos++];
			}
		}

		return count;
	}
}
