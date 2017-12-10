package be.nikiroo.utils.android;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import be.nikiroo.utils.Image;
import be.nikiroo.utils.ImageUtils;

/**
 * This class offer some utilities based around images and uses the Android
 * framework.
 * 
 * @author niki
 */
public class ImageUtilsAndroid extends ImageUtils {
	@Override
	public void saveAsImage(Image img, File target, String format)
			throws IOException {
		FileOutputStream fos = new FileOutputStream(target);
		try {
			Bitmap image = fromImage(img);

			boolean ok = false;
			try {
				ok = image.compress(
						Bitmap.CompressFormat.valueOf(format.toUpperCase()),
						90, fos);
			} catch (Exception e) {
				ok = false;
			}

			// Some formats are not reliable
			// Second change: PNG
			if (!ok && !format.equals("png")) {
				ok = image.compress(Bitmap.CompressFormat.PNG, 90, fos);
			}

			if (!ok) {
				throw new IOException(
						"Cannot find a writer for this image and format: "
								+ format);
			}
		} catch (IOException e) {
			throw new IOException("Cannot write image to " + target, e);
		} finally {
			fos.close();
		}
	}

	/**
	 * Convert the given {@link Image} into a {@link Bitmap} object.
	 * 
	 * @param img
	 *            the {@link Image}
	 * @return the {@link Image} object
	 * @throws IOException
	 *             in case of IO error
	 */
	static public Bitmap fromImage(Image img) throws IOException {
		Bitmap image = BitmapFactory.decodeByteArray(img.getData(), 0,
				img.getData().length);
		if (image == null) {
			int size = img.getData().length;
			String ssize = size + " byte";
			if (size > 1) {
				ssize = size + " bytes";
				if (size >= 1000) {
					size = size / 1000;
					ssize = size + " kb";
					if (size > 1000) {
						size = size / 1000;
						ssize = size + " MB";
					}
				}
			}

			throw new IOException(
					"Failed to convert input to image, size was: " + ssize);
		}

		return image;
	}
}
