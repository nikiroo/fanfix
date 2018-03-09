package be.nikiroo.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

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
	 * or justification is applied (if there is enough horizontal space for it
	 * to be aligned).
	 */
	public enum Alignment {
		/** Aligned at left. */
		LEFT,
		/** Centered. */
		CENTER,
		/** Aligned at right. */
		RIGHT,
		/** Full justified (to both left and right). */
		JUSTIFY,

		// Old Deprecated values:

		/** DEPRECATED: please use LEFT. */
		@Deprecated
		Beginning,
		/** DEPRECATED: please use CENTER. */
		@Deprecated
		Center,
		/** DEPRECATED: please use RIGHT. */
		@Deprecated
		End;

		/**
		 * Return the non-deprecated version of this enum if needed (or return
		 * self if not).
		 * 
		 * @return the non-deprecated value
		 */
		Alignment undeprecate() {
			if (this == Beginning)
				return LEFT;
			if (this == Center)
				return CENTER;
			if (this == End)
				return RIGHT;
			return this;
		}
	}

	static private Pattern marks = getMarks();

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
		return padString(text, width, true, null);
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
	 *            space (default is Alignment.Beginning)
	 * 
	 * @return the resulting {@link String} of size <i>size</i> minimum
	 */
	static public String padString(String text, int width, boolean cut,
			Alignment align) {

		if (align == null) {
			align = Alignment.LEFT;
		}

		align = align.undeprecate();

		if (width >= 0) {
			if (text == null)
				text = "";

			int diff = width - text.length();

			if (diff < 0) {
				if (cut)
					text = text.substring(0, width);
			} else if (diff > 0) {
				if (diff < 2 && align != Alignment.RIGHT)
					align = Alignment.LEFT;

				switch (align) {
				case RIGHT:
					text = new String(new char[diff]).replace('\0', ' ') + text;
					break;
				case CENTER:
					int pad1 = (diff) / 2;
					int pad2 = (diff + 1) / 2;
					text = new String(new char[pad1]).replace('\0', ' ') + text
							+ new String(new char[pad2]).replace('\0', ' ');
					break;
				case LEFT:
				default:
					text = text + new String(new char[diff]).replace('\0', ' ');
					break;
				}
			}
		}

		return text;
	}

	/**
	 * Justify a text into width-sized (at the maximum) lines.
	 * 
	 * @param text
	 *            the {@link String} to justify
	 * @param width
	 *            the maximum size of the resulting lines
	 * 
	 * @return a list of justified text lines
	 */
	static public List<String> justifyText(String text, int width) {
		return justifyText(text, width, null);
	}

	/**
	 * Justify a text into width-sized (at the maximum) lines.
	 * 
	 * @param text
	 *            the {@link String} to justify
	 * @param width
	 *            the maximum size of the resulting lines
	 * @param align
	 *            align the lines in this position (default is
	 *            Alignment.Beginning)
	 * 
	 * @return a list of justified text lines
	 */
	static public List<String> justifyText(String text, int width,
			Alignment align) {
		if (align == null) {
			align = Alignment.LEFT;
		}

		align = align.undeprecate();

		switch (align) {
		case CENTER:
			return StringJustifier.center(text, width);
		case RIGHT:
			return StringJustifier.right(text, width);
		case JUSTIFY:
			return StringJustifier.full(text, width);
		case LEFT:
		default:
			return StringJustifier.left(text, width);
		}
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
			if (marks != null) {
				input = marks.matcher(input).replaceAll("");
			}
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
	 * Convert between the time in milliseconds to a {@link String} in a "fixed"
	 * way (to exchange data over the wire, for instance).
	 * <p>
	 * Precise to the second.
	 * 
	 * @param time
	 *            the specified number of milliseconds since the standard base
	 *            time known as "the epoch", namely January 1, 1970, 00:00:00
	 *            GMT
	 * 
	 * @return the time as a {@link String}
	 */
	static public String fromTime(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date(time));
	}

	/**
	 * Convert between the time as a {@link String} to milliseconds in a "fixed"
	 * way (to exchange data over the wire, for instance).
	 * <p>
	 * Precise to the second.
	 * 
	 * @param displayTime
	 *            the time as a {@link String}
	 * 
	 * @return the number of milliseconds since the standard base time known as
	 *         "the epoch", namely January 1, 1970, 00:00:00 GMT, or -1 in case
	 *         of error
	 * 
	 * @throws ParseException
	 *             in case of parse error
	 */
	static public long toTime(String displayTime) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.parse(displayTime).getTime();
	}

	/**
	 * Return a hash of the given {@link String}.
	 * 
	 * @param input
	 *            the input data
	 * 
	 * @return the hash
	 */
	static public String getMd5Hash(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(input.getBytes("UTF-8"));
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
		} catch (UnsupportedEncodingException e) {
			return input;
		}
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

		char nbsp = ' '; // non-breakable space (a special char)
		char space = ' ';
		return HtmlEscape.unescapeHtml(builder.toString()).replace(nbsp, space);
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

	/**
	 * Zip the data and then encode it into Base64.
	 * 
	 * @param data
	 *            the data
	 * 
	 * @return the Base64 zipped version
	 */
	public static String zip64(String data) {
		try {
			return Base64.encodeBytes(data.getBytes(), Base64.GZIP);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Unconvert from Base64 then unzip the content.
	 * 
	 * @param data
	 *            the data in Base64 format
	 * 
	 * @return the raw data
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static String unzip64(String data) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(Base64.decode(data,
				Base64.GZIP));

		Scanner scan = new Scanner(in);
		scan.useDelimiter("\\A");
		try {
			return scan.next();
		} finally {
			scan.close();
		}
	}

	/**
	 * The "remove accents" pattern.
	 * 
	 * @return the pattern, or NULL if a problem happens
	 */
	private static Pattern getMarks() {
		try {
			return Pattern
					.compile("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+");
		} catch (Exception e) {
			// Can fail on Android...
			return null;
		}
	}
}
