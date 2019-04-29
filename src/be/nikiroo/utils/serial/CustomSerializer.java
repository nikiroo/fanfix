package be.nikiroo.utils.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.streams.NextableInputStream;
import be.nikiroo.utils.streams.NextableInputStreamStep;
import be.nikiroo.utils.streams.ReplaceInputStream;
import be.nikiroo.utils.streams.ReplaceOutputStream;

/**
 * A {@link CustomSerializer} supports and generates values in the form:
 * <ul>
 * <li><tt>custom^<i>TYPE</i>^<i>ENCODED_VALUE</i></tt></li>
 * </ul>
 * <p>
 * In this scheme, the values are:
 * <ul>
 * <li><tt>custom</tt>: a fixed keyword</li>
 * <li><tt>^</tt>: a fixed separator character (the
 * <tt><i>ENCODED_VALUE</i></tt> can still use it inside its content, though</li>
 * <li><tt><i>TYPE</i></tt>: the object type of this value</li>
 * <li><tt><i>ENCODED_VALUE</i></tt>: the custom encoded value</li>
 * </ul>
 * <p>
 * To create a new {@link CustomSerializer}, you are expected to implement the
 * abstract methods of this class. The rest should be taken care of bythe
 * system.
 * 
 * @author niki
 */
public abstract class CustomSerializer {
	/**
	 * Generate the custom <tt><i>ENCODED_VALUE</i></tt> from this
	 * <tt>value</tt>.
	 * <p>
	 * The <tt>value</tt> will always be of the supported type.
	 * 
	 * @param out
	 *            the {@link OutputStream} to write the value to
	 * @param value
	 *            the value to serialize
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract void toStream(OutputStream out, Object value)
			throws IOException;

	/**
	 * Regenerate the value from the custom <tt><i>ENCODED_VALUE</i></tt>.
	 * <p>
	 * The value in the {@link InputStream} <tt>in</tt> will always be of the
	 * supported type.
	 * 
	 * @param in
	 *            the {@link InputStream} containing the
	 *            <tt><i>ENCODED_VALUE</i></tt>
	 * 
	 * @return the regenerated object
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	protected abstract Object fromStream(InputStream in) throws IOException;

	/**
	 * Return the supported type name.
	 * <p>
	 * It <b>must</b> be the name returned by {@link Object#getClass()
	 * #getCanonicalName()}.
	 * 
	 * @return the supported class name
	 */
	protected abstract String getType();

	/**
	 * Encode the object into the given {@link OutputStream}.
	 * 
	 * @param out
	 *            the builder to append to
	 * @param value
	 *            the object to encode
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void encode(OutputStream out, Object value) throws IOException {
		ReplaceOutputStream replace = new ReplaceOutputStream(out, //
				new String[] { "\\", "\n" }, //
				new String[] { "\\\\", "\\n" });

		try {
			SerialUtils.write(replace, "custom^");
			SerialUtils.write(replace, getType());
			SerialUtils.write(replace, "^");
			toStream(replace, value);
		} finally {
			replace.close(false);
		}
	}

	/**
	 * Decode the value back into the supported object type.
	 * 
	 * @param in
	 *            the encoded value
	 * 
	 * @return the object
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public Object decode(InputStream in) throws IOException {
		ReplaceInputStream replace = new ReplaceInputStream(in, //
				new String[] { "\\\\", "\\n" }, //
				new String[] { "\\", "\n" });

		try {
			NextableInputStream stream = new NextableInputStream(
					replace.open(), new NextableInputStreamStep('^'));
			try {
				if (!stream.next()) {
					throw new IOException(
							"Cannot find the first custom^ element");
				}

				String custom = IOUtils.readSmallStream(stream);
				if (!"custom".equals(custom)) {
					throw new IOException(
							"Cannot find the first custom^ element, it is: "
									+ custom + "^");
				}

				if (!stream.next()) {
					throw new IOException("Cannot find the second custom^"
							+ getType() + " element");
				}

				String type = IOUtils.readSmallStream(stream);
				if (!getType().equals(type)) {
					throw new IOException("Cannot find the second custom^"
							+ getType() + " element, it is: custom^" + type
							+ "^");
				}

				if (!stream.nextAll()) {
					throw new IOException("Cannot find the third custom^"
							+ getType() + "^value element");
				}

				return fromStream(stream);
			} finally {
				stream.close();
			}
		} finally {
			replace.close(false);
		}
	}

	public static boolean isCustom(String encodedValue) {
		int pos1 = encodedValue.indexOf('^');
		int pos2 = encodedValue.indexOf('^', pos1 + 1);

		return pos1 >= 0 && pos2 >= 0 && encodedValue.startsWith("custom^");
	}

	public static String typeOf(String encodedValue) {
		int pos1 = encodedValue.indexOf('^');
		int pos2 = encodedValue.indexOf('^', pos1 + 1);
		String type = encodedValue.substring(pos1 + 1, pos2);

		return type;
	}

	public static String contentOf(String encodedValue) {
		int pos1 = encodedValue.indexOf('^');
		int pos2 = encodedValue.indexOf('^', pos1 + 1);
		String encodedContent = encodedValue.substring(pos2 + 1);

		return encodedContent;
	}
}
