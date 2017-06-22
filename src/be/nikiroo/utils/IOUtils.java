package be.nikiroo.utils;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

/**
 * This class offer some utilities based around Streams.
 * 
 * @author niki
 */
public class IOUtils {
	/**
	 * Write the data to the given {@link File}.
	 * 
	 * @param in
	 *            the data source
	 * @param target
	 *            the target {@link File}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void write(InputStream in, File target) throws IOException {
		OutputStream out = new FileOutputStream(target);
		try {
			write(in, out);
		} finally {
			out.close();
		}
	}

	/**
	 * Write the data to the given {@link OutputStream}.
	 * 
	 * @param in
	 *            the data source
	 * @param out
	 *            the target {@link OutputStream}
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void write(InputStream in, OutputStream out)
			throws IOException {
		byte buffer[] = new byte[4069];
		for (int len = 0; (len = in.read(buffer)) > 0;) {
			out.write(buffer, 0, len);
		}
	}

	/**
	 * Recursively Add a {@link File} (which can thus be a directory, too) to a
	 * {@link ZipOutputStream}.
	 * 
	 * @param zip
	 *            the stream
	 * @param base
	 *            the path to prepend to the ZIP info before the actual
	 *            {@link File} path
	 * @param target
	 *            the source {@link File} (which can be a directory)
	 * @param targetIsRoot
	 *            FALSE if we need to add a {@link ZipEntry} for base/target,
	 *            TRUE to add it at the root of the ZIP
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void zip(ZipOutputStream zip, String base, File target,
			boolean targetIsRoot) throws IOException {
		if (target.isDirectory()) {
			if (!targetIsRoot) {
				if (base == null || base.isEmpty()) {
					base = target.getName();
				} else {
					base += "/" + target.getName();
				}
				zip.putNextEntry(new ZipEntry(base + "/"));
			}
			for (File file : target.listFiles()) {
				zip(zip, base, file, false);
			}
		} else {
			if (base == null || base.isEmpty()) {
				base = target.getName();
			} else {
				base += "/" + target.getName();
			}
			zip.putNextEntry(new ZipEntry(base));
			FileInputStream in = new FileInputStream(target);
			try {
				IOUtils.write(in, zip);
			} finally {
				in.close();
			}
		}
	}

	/**
	 * Zip the given source into dest.
	 * 
	 * @param src
	 *            the source {@link File} (which can be a directory)
	 * @param dest
	 *            the destination <tt>.zip</tt> file
	 * @param srcIsRoot
	 *            FALSE if we need to add a {@link ZipEntry} for src, TRUE to
	 *            add it at the root of the ZIP
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void zip(File src, File dest, boolean srcIsRoot)
			throws IOException {
		OutputStream out = new FileOutputStream(dest);
		try {
			ZipOutputStream zip = new ZipOutputStream(out);
			try {
				IOUtils.zip(zip, "", src, srcIsRoot);
			} finally {
				zip.close();
			}
		} finally {
			out.close();
		}
	}

	/**
	 * Write the {@link String} content to {@link File}.
	 * 
	 * @param dir
	 *            the directory where to write the {@link File}
	 * @param filename
	 *            the {@link File} name
	 * @param content
	 *            the content
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static void writeSmallFile(File dir, String filename, String content)
			throws IOException {
		if (!dir.exists()) {
			dir.mkdirs();
		}

		FileWriter writerVersion = new FileWriter(new File(dir, filename));
		try {
			writerVersion.write(content);
		} finally {
			writerVersion.close();
		}
	}

	/**
	 * Read the whole {@link File} content into a {@link String}.
	 * 
	 * @param file
	 *            the {@link File}
	 * 
	 * @return the content
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public static String readSmallFile(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		try {
			StringBuilder builder = new StringBuilder();
			for (String line = reader.readLine(); line != null; line = reader
					.readLine()) {
				builder.append(line);
			}
			return builder.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * Recursively delete the given {@link File}, which may of course also be a
	 * directory.
	 * <p>
	 * Will silently continue in case of error.
	 * 
	 * @param target
	 *            the target to delete
	 */
	public static void deltree(File target) {
		File[] files = target.listFiles();
		if (files != null) {
			for (File file : files) {
				deltree(file);
			}
		}

		if (!target.delete()) {
			System.err.println("Cannot delete: " + target.getAbsolutePath());
		}
	}

	/**
	 * Convert the given {@link InputStream} (which should allow calls to
	 * {@link InputStream#reset() for better perfs}) into an {@link Image}
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
	public static BufferedImage toImage(InputStream in) throws IOException {
		MarkableFileInputStream tmpIn = null;
		File tmp = null;
		try {
			in.reset();
		} catch (IOException e) {
			tmp = File.createTempFile("fanfic-tmp-image", ".tmp");
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

		// Note: this code has been found on internet;
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
	 * Open the given /-separated resource (from the binary root).
	 * 
	 * @param name
	 *            the resource name
	 * 
	 * @return the opened resource if found, NLL if not
	 */
	public static InputStream openResource(String name) {
		ClassLoader loader = IOUtils.class.getClassLoader();
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}

		return loader.getResourceAsStream(name);
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
	private static int getExifTransorm(InputStream in) throws IOException {
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
