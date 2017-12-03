package be.nikiroo.utils.ui;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import be.nikiroo.utils.Image;
import be.nikiroo.utils.ImageUtils;

/**
 * This class offer some utilities based around images and uses java.awt.
 * 
 * @author niki
 */
public class ImageUtilsAwt extends ImageUtils {
	@Override
	public void saveAsImage(Image img, File target, String format)
			throws IOException {
		try {
			BufferedImage image = fromImage(img);

			boolean ok = false;
			try {

				ok = ImageIO.write(image, format, target);
			} catch (IOException e) {
				ok = false;
			}

			// Some formats are not reliable
			// Second chance: PNG
			if (!ok && !format.equals("png")) {
				ok = ImageIO.write(image, "png", target);
			}

			if (!ok) {
				throw new IOException(
						"Cannot find a writer for this image and format: "
								+ format);
			}
		} catch (IOException e) {
			throw new IOException("Cannot write image to " + target, e);
		}
	}

	/**
	 * Convert the given {@link Image} into a {@link BufferedImage} object,
	 * respecting the EXIF transformations if any.
	 * 
	 * @param img
	 *            the {@link Image}
	 * 
	 * @return the {@link Image} object
	 * 
	 * @throws IOException
	 *             in case of IO error
	 */
	static public BufferedImage fromImage(Image img) throws IOException {
		InputStream in = new ByteArrayInputStream(img.getData());

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

		return image;
	}
}
