package be.nikiroo.utils;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.unbescape.html.HtmlEscape;
import org.unbescape.html.HtmlEscapeLevel;
import org.unbescape.html.HtmlEscapeType;

/**
 * This class offer some utilities based around {@link String}s.
 * 
 * @author niki
 */
public class StringUtils {
	/**
	 * This enum type will decide the alignment of a {@link String} when padding
	 * is applied or if there is enough horizontal space for it to be aligned.
	 */
	public enum Alignment {
		/** Aligned at left. */
		Beginning,
		/** Centered. */
		Center,
		/** Aligned at right. */
		End
	}

	static private Pattern marks = Pattern
			.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");

	/**
	 * Fix the size of the given {@link String} either with space-padding or by
	 * shortening it.
	 * 
	 * @param text
	 *            the {@link String} to fix
	 * @param width
	 *            the size of the resulting {@link String} or -1 for a noop
	 * 
	 * @return the resulting {@link String} of size <i>size</i>
	 */
	static public String padString(String text, int width) {
		return padString(text, width, true, Alignment.Beginning);
	}

	/**
	 * Fix the size of the given {@link String} either with space-padding or by
	 * optionally shortening it.
	 * 
	 * @param text
	 *            the {@link String} to fix
	 * @param width
	 *            the size of the resulting {@link String} if the text fits or
	 *            if cut is TRUE or -1 for a noop
	 * @param cut
	 *            cut the {@link String} shorter if needed
	 * @param align
	 *            align the {@link String} in this position if we have enough
	 *            space
	 * 
	 * @return the resulting {@link String} of size <i>size</i> minimum
	 */
	static public String padString(String text, int width, boolean cut,
			Alignment align) {

		if (width >= 0) {
			if (text == null)
				text = "";

			int diff = width - text.length();

			if (diff < 0) {
				if (cut)
					text = text.substring(0, width);
			} else if (diff > 0) {
				if (diff < 2 && align != Alignment.End)
					align = Alignment.Beginning;

				switch (align) {
				case Beginning:
					text = text + new String(new char[diff]).replace('\0', ' ');
					break;
				case End:
					text = new String(new char[diff]).replace('\0', ' ') + text;
					break;
				case Center:
				default:
					int pad1 = (diff) / 2;
					int pad2 = (diff + 1) / 2;
					text = new String(new char[pad1]).replace('\0', ' ') + text
							+ new String(new char[pad2]).replace('\0', ' ');
					break;
				}
			}
		}

		return text;
	}

	/**
	 * Sanitise the given input to make it more Terminal-friendly by removing
	 * combining characters.
	 * 
	 * @param input
	 *            the input to sanitise
	 * @param allowUnicode
	 *            allow Unicode or only allow ASCII Latin characters
	 * 
	 * @return the sanitised {@link String}
	 */
	static public String sanitize(String input, boolean allowUnicode) {
		return sanitize(input, allowUnicode, !allowUnicode);
	}

	/**
	 * Sanitise the given input to make it more Terminal-friendly by removing
	 * combining characters.
	 * 
	 * @param input
	 *            the input to sanitise
	 * @param allowUnicode
	 *            allow Unicode or only allow ASCII Latin characters
	 * @param removeAllAccents
	 *            TRUE to replace all accentuated characters by their non
	 *            accentuated counter-parts
	 * 
	 * @return the sanitised {@link String}
	 */
	static public String sanitize(String input, boolean allowUnicode,
			boolean removeAllAccents) {

		if (removeAllAccents) {
			input = Normalizer.normalize(input, Form.NFKD);
			input = marks.matcher(input).replaceAll("");
		}

		input = Normalizer.normalize(input, Form.NFKC);

		if (!allowUnicode) {
			StringBuilder builder = new StringBuilder();
			for (int index = 0; index < input.length(); index++) {
				char car = input.charAt(index);
				// displayable chars in ASCII are in the range 32<->255,
				// except DEL (127)
				if (car >= 32 && car <= 255 && car != 127) {
					builder.append(car);
				}
			}
			input = builder.toString();
		}

		return input;
	}

	/**
	 * Convert between time in milliseconds to {@link String} in a "static" way
	 * (to exchange data over the wire, for instance).
	 * 
	 * @param time
	 *            the time in milliseconds
	 * 
	 * @return the time as a {@link String}
	 */
	static public String fromTime(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date(time));
	}

	/**
	 * Convert between time as a {@link String} to milliseconds in a "static"
	 * way (to exchange data over the wire, for instance).
	 * 
	 * @param time
	 *            the time as a {@link String}
	 * 
	 * @return the time in milliseconds
	 */
	static public long toTime(String display) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return sdf.parse(display).getTime();
		} catch (ParseException e) {
			return -1;
		}
	}

	/**
	 * Convert the given {@link Image} object into a Base64 representation of
	 * the same {@link Image}. object.
	 * 
	 * @param image
	 *            the {@link Image} object to convert
	 * 
	 * @return the Base64 representation
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public String fromImage(BufferedImage image) throws IOException {
		String imageString = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		ImageIO.write(image, "jpeg", out);
		byte[] imageBytes = out.toByteArray();

		imageString = new String(Base64.getEncoder().encode(imageBytes));

		out.close();

		return imageString;
	}

	/**
	 * Convert the given {@link File} image into a Base64 representation of the
	 * same {@link File}.
	 * 
	 * @param file
	 *            the {@link File} image to convert
	 * 
	 * @return the Base64 representation
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public String fromStream(InputStream in) throws IOException {
		String fileString = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		byte[] buf = new byte[8192];

		int c = 0;
		while ((c = in.read(buf, 0, buf.length)) > 0) {
			out.write(buf, 0, c);
		}
		out.flush();
		in.close();

		fileString = new String(Base64.getEncoder().encode(out.toByteArray()));
		out.close();

		return fileString;
	}

	/**
	 * Convert the given Base64 representation of an image into an {@link Image}
	 * object.
	 * 
	 * @param b64data
	 *            the {@link Image} in Base64 format
	 * 
	 * @return the {@link Image} object
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public BufferedImage toImage(String b64data) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(Base64.getDecoder()
				.decode(b64data));
		return toImage(in);
	}

	/**
	 * Convert the given {@link InputStream} (which must allow calls to
	 * {@link InputStream#reset()}) into an {@link Image} object.
	 * 
	 * @param in
	 *            the 'resetable' {@link InputStream}
	 * 
	 * @return the {@link Image} object
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public BufferedImage toImage(InputStream in) throws IOException {
		int orientation;
		try {
			orientation = getExifTransorm(in);
		} catch (Exception e) {
			// no EXIF transform, ok
			orientation = -1;
		}

		in.reset();
		BufferedImage image = ImageIO.read(in);

		if (image == null) {
			throw new IOException("Failed to convert input to image");
		}

		// Note: this code has been found on internet;
		// thank you anonymous coder.
		int width = image.getWidth();
		int height = image.getHeight();
		AffineTransform affineTransform = new AffineTransform();

		switch (orientation) {
		case 1:
			break;
		case 2: // Flip X
			affineTransform.scale(-1.0, 1.0);
			affineTransform.translate(-width, 0);
			break;
		case 3: // PI rotation
			affineTransform.translate(width, height);
			affineTransform.rotate(Math.PI);
			break;
		case 4: // Flip Y
			affineTransform.scale(1.0, -1.0);
			affineTransform.translate(0, -height);
			break;
		case 5: // - PI/2 and Flip X
			affineTransform.rotate(-Math.PI / 2);
			affineTransform.scale(-1.0, 1.0);
			break;
		case 6: // -PI/2 and -width
			affineTransform.translate(height, 0);
			affineTransform.rotate(Math.PI / 2);
			break;
		case 7: // PI/2 and Flip
			affineTransform.scale(-1.0, 1.0);
			affineTransform.translate(-height, 0);
			affineTransform.translate(0, width);
			affineTransform.rotate(3 * Math.PI / 2);
			break;
		case 8: // PI / 2
			affineTransform.translate(0, width);
			affineTransform.rotate(3 * Math.PI / 2);
			break;
		default:
			affineTransform = null;
			break;
		}

		if (affineTransform != null) {
			AffineTransformOp affineTransformOp = new AffineTransformOp(
					affineTransform, AffineTransformOp.TYPE_BILINEAR);

			BufferedImage transformedImage = new BufferedImage(height, width,
					image.getType());
			transformedImage = affineTransformOp
					.filter(image, transformedImage);

			image = transformedImage;
		}
		//

		return image;
	}

	/**
	 * Return a hash of the given {@link String}.
	 * 
	 * @param input
	 *            the input data
	 * 
	 * @return the hash
	 */
	static public String getHash(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(input.getBytes());
			byte byteData[] = md.digest();

			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < byteData.length; i++) {
				String hex = Integer.toHexString(0xff & byteData[i]);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}

			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			return input;
		}
	}

	/**
	 * Return the EXIF transformation flag of this image if any.
	 * 
	 * <p>
	 * Note: this code has been found on internet; thank you anonymous coder.
	 * </p>
	 * 
	 * @param in
	 *            the data {@link InputStream}
	 * 
	 * @return the transformation flag if any
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static private int getExifTransorm(InputStream in) throws IOException {
		int[] exif_data = new int[100];
		int set_flag = 0;
		int is_motorola = 0;

		/* Read File head, check for JPEG SOI + Exif APP1 */
		for (int i = 0; i < 4; i++)
			exif_data[i] = in.read();

		if (exif_data[0] != 0xFF || exif_data[1] != 0xD8
				|| exif_data[2] != 0xFF || exif_data[3] != 0xE1)
			return -2;

		/* Get the marker parameter length count */
		int length = (in.read() << 8 | in.read());

		/* Length includes itself, so must be at least 2 */
		/* Following Exif data length must be at least 6 */
		if (length < 8)
			return -1;
		length -= 8;
		/* Read Exif head, check for "Exif" */
		for (int i = 0; i < 6; i++)
			exif_data[i] = in.read();

		if (exif_data[0] != 0x45 || exif_data[1] != 0x78
				|| exif_data[2] != 0x69 || exif_data[3] != 0x66
				|| exif_data[4] != 0 || exif_data[5] != 0)
			return -1;

		/* Read Exif body */
		length = length > exif_data.length ? exif_data.length : length;
		for (int i = 0; i < length; i++)
			exif_data[i] = in.read();

		if (length < 12)
			return -1; /* Length of an IFD entry */

		/* Discover byte order */
		if (exif_data[0] == 0x49 && exif_data[1] == 0x49)
			is_motorola = 0;
		else if (exif_data[0] == 0x4D && exif_data[1] == 0x4D)
			is_motorola = 1;
		else
			return -1;

		/* Check Tag Mark */
		if (is_motorola == 1) {
			if (exif_data[2] != 0)
				return -1;
			if (exif_data[3] != 0x2A)
				return -1;
		} else {
			if (exif_data[3] != 0)
				return -1;
			if (exif_data[2] != 0x2A)
				return -1;
		}

		/* Get first IFD offset (offset to IFD0) */
		int offset;
		if (is_motorola == 1) {
			if (exif_data[4] != 0)
				return -1;
			if (exif_data[5] != 0)
				return -1;
			offset = exif_data[6];
			offset <<= 8;
			offset += exif_data[7];
		} else {
			if (exif_data[7] != 0)
				return -1;
			if (exif_data[6] != 0)
				return -1;
			offset = exif_data[5];
			offset <<= 8;
			offset += exif_data[4];
		}
		if (offset > length - 2)
			return -1; /* check end of data segment */

		/* Get the number of directory entries contained in this IFD */
		int number_of_tags;
		if (is_motorola == 1) {
			number_of_tags = exif_data[offset];
			number_of_tags <<= 8;
			number_of_tags += exif_data[offset + 1];
		} else {
			number_of_tags = exif_data[offset + 1];
			number_of_tags <<= 8;
			number_of_tags += exif_data[offset];
		}
		if (number_of_tags == 0)
			return -1;
		offset += 2;

		/* Search for Orientation Tag in IFD0 */
		for (;;) {
			if (offset > length - 12)
				return -1; /* check end of data segment */
			/* Get Tag number */
			int tagnum;
			if (is_motorola == 1) {
				tagnum = exif_data[offset];
				tagnum <<= 8;
				tagnum += exif_data[offset + 1];
			} else {
				tagnum = exif_data[offset + 1];
				tagnum <<= 8;
				tagnum += exif_data[offset];
			}
			if (tagnum == 0x0112)
				break; /* found Orientation Tag */
			if (--number_of_tags == 0)
				return -1;
			offset += 12;
		}

		/* Get the Orientation value */
		if (is_motorola == 1) {
			if (exif_data[offset + 8] != 0)
				return -1;
			set_flag = exif_data[offset + 9];
		} else {
			if (exif_data[offset + 9] != 0)
				return -1;
			set_flag = exif_data[offset + 8];
		}
		if (set_flag > 8)
			return -1;

		return set_flag;
	}

	/**
	 * Remove the HTML content from the given input, and un-html-ize the rest.
	 * 
	 * @param html
	 *            the HTML-encoded content
	 * 
	 * @return the HTML-free equivalent content
	 */
	public static String unhtml(String html) {
		StringBuilder builder = new StringBuilder();

		int inTag = 0;
		for (char car : html.toCharArray()) {
			if (car == '<') {
				inTag++;
			} else if (car == '>') {
				inTag--;
			} else if (inTag <= 0) {
				builder.append(car);
			}
		}

		return HtmlEscape.unescapeHtml(builder.toString());
	}

	/**
	 * Escape the given {@link String} so it can be used in XML, as content.
	 * 
	 * @param input
	 *            the input {@link String}
	 * 
	 * @return the escaped {@link String}
	 */
	public static String xmlEscape(String input) {
		if (input == null) {
			return "";
		}

		return HtmlEscape.escapeHtml(input,
				HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_HEXA,
				HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
	}

	/**
	 * Escape the given {@link String} so it can be used in XML, as text content
	 * inside double-quotes.
	 * 
	 * @param input
	 *            the input {@link String}
	 * 
	 * @return the escaped {@link String}
	 */
	public static String xmlEscapeQuote(String input) {
		if (input == null) {
			return "";
		}

		return HtmlEscape.escapeHtml(input,
				HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_HEXA,
				HtmlEscapeLevel.LEVEL_1_ONLY_MARKUP_SIGNIFICANT);
	}
}
