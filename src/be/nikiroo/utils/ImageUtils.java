package be.nikiroo.utils;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import be.nikiroo.utils.ImageText.Mode;

/**
 * This class offer some utilities based around images.
 * 
 * @author niki
 */
public class ImageUtils {
	/**
	 * Convert the given {@link Image} object into a Base64 representation of
	 * the same {@link Image} object.
	 * 
	 * @param image
	 *            the {@link Image} object to convert
	 * 
	 * @return the Base64 representation
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public String toBase64(BufferedImage image) throws IOException {
		return toBase64(image, null);
	}

	/**
	 * Convert the given {@link Image} object into a Base64 representation of
	 * the same {@link Image}. object.
	 * 
	 * @param image
	 *            the {@link Image} object to convert
	 * @param format
	 *            the image format to use to serialise it (default is PNG)
	 * 
	 * @return the Base64 representation
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public String toBase64(BufferedImage image, String format)
			throws IOException {
		if (format == null) {
			format = "png";
		}

		String imageString = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		ImageIO.write(image, format, out);
		byte[] imageBytes = out.toByteArray();

		imageString = new String(Base64.encodeBytes(imageBytes));

		out.close();

		return imageString;
	}

	/**
	 * Convert the given image into a Base64 representation of the same
	 * {@link File}.
	 * 
	 * @param in
	 *            the image to convert
	 * 
	 * @return the Base64 representation
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public String toBase64(InputStream in) throws IOException {
		String fileString = null;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		byte[] buf = new byte[8192];

		int c = 0;
		while ((c = in.read(buf, 0, buf.length)) > 0) {
			out.write(buf, 0, c);
		}
		out.flush();
		in.close();

		fileString = new String(Base64.encodeBytes(out.toByteArray()));
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
	static public BufferedImage fromBase64(String b64data) throws IOException {
		ByteArrayInputStream in = new ByteArrayInputStream(
				Base64.decode(b64data));
		return fromStream(in);
	}

	/**
	 * Convert the given {@link InputStream} (which should allow calls to
	 * {@link InputStream#reset()} for better perfs) into an {@link Image}
	 * object, respecting the EXIF transformations if any.
	 * 
	 * @param in
	 *            the 'resetable' {@link InputStream}
	 * 
	 * @return the {@link Image} object
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public BufferedImage fromStream(InputStream in) throws IOException {
		MarkableFileInputStream tmpIn = null;
		File tmp = null;

		boolean repack = !in.markSupported();
		if (!repack) {
			try {
				in.reset();
			} catch (IOException e) {
				repack = true;
			}
		}

		if (repack) {
			tmp = File.createTempFile(".tmp-image", ".tmp");
			tmp.deleteOnExit();
			IOUtils.write(in, tmp);
			tmpIn = new MarkableFileInputStream(new FileInputStream(tmp));
		}

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
			if (tmp != null) {
				tmp.delete();
				tmpIn.close();
			}
			throw new IOException("Failed to convert input to image");
		}

		// Note: this code has been found on Internet;
		// thank you anonymous coder.
		int width = image.getWidth();
		int height = image.getHeight();
		AffineTransform affineTransform = new AffineTransform();

		switch (orientation) {
		case 1:
			affineTransform = null;
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

			BufferedImage transformedImage = new BufferedImage(width, height,
					image.getType());
			transformedImage = affineTransformOp
					.filter(image, transformedImage);

			image = transformedImage;
		}
		//

		if (tmp != null) {
			tmp.delete();
			tmpIn.close();
		}

		return image;
	}

	/**
	 * A shorthand method to create an {@link ImageText} and return its output.
	 * 
	 * @param image
	 *            the source {@link Image}
	 * @param size
	 *            the final text size to target
	 * @param mode
	 *            the mode of conversion
	 * @param invert
	 *            TRUE to invert colours rendering
	 * 
	 * @return the text image
	 */
	static public String toAscii(Image image, Dimension size, Mode mode,
			boolean invert) {
		return new ImageText(image, size, mode, invert).toString();
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
}
