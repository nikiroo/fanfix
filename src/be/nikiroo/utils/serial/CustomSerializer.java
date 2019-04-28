package be.nikiroo.utils.serial;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.streams.NextableInputStream;
import be.nikiroo.utils.streams.NextableInputStreamStep;
import be.nikiroo.utils.streams.ReplaceInputStream;
import be.nikiroo.utils.streams.ReplaceOutputStream;

public abstract class CustomSerializer {

	protected abstract void toStream(OutputStream out, Object value)
			throws IOException;

	protected abstract Object fromStream(InputStream in) throws IOException;

	protected abstract String getType();

	/**
	 * Encode the object into the given {@link OutputStream} if supported.
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
