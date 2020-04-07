package be.nikiroo.fanfix_swing.images;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;

import be.nikiroo.utils.IOUtils;

/**
 * Icons generator for this project.
 * 
 * @author niki
 */
public class IconGenerator {
	/**
	 * The available icons.
	 * 
	 * @author niki
	 */
	public enum Icon {
		/** Icon used to clear text fields */
		clear,
		/** Search icon (magnifying glass) */
		search,
		/** An interrogation point */
		unknown,
		/** A small, left-pointed arrow */
		arrow_left,
		/** A small, right-pointed arrow */
		arrow_right,
		/** A small, up-pointed arrow */
		arrow_up,
		/** A small, down-pointed arrow */
		arrow_down,
		/** An empty (transparent) icon */
		empty,
	}

	/**
	 * The available sizes.
	 * 
	 * @author niki
	 */
	public enum Size {
		/** 4x4 pixels, only for {@link Icon#empty} */
		x4(4),
		/** 8x8 pixels, only for {@link Icon#empty} */
		x8(8),
		/** 16x16 pixels */
		x16(16),
		/** 24x24 pixels */
		x24(24),
		/** 32x32 pixels */
		x32(32),
		/** 64x64 pixels */
		x64(64);

		private int size;

		private Size(int size) {
			this.size = size;
		}

		/**
		 * Return the size in pixels.
		 * 
		 * @return the size
		 */
		public int getSize() {
			return size;
		}
	}

	static private Map<String, ImageIcon> map = new HashMap<String, ImageIcon>();

	/**
	 * Generate a new image.
	 * 
	 * @param name the name of the resource
	 * @param size the requested size
	 * 
	 * @return the image, or NULL if it does not exist or does not exist at that
	 *         size
	 */
	static public ImageIcon get(Icon name, Size size) {
		String key = String.format("%s-%dx%d.png", name.name(), size.getSize(), size.getSize());
		if (!map.containsKey(key)) {
			map.put(key, generate(key));
		}

		return map.get(key);
	}

	/**
	 * Generate a new image.
	 * 
	 * @param filename the file name of the resource (no directory)
	 * 
	 * @return the image, or NULL if it does not exist or does not exist at that
	 *         size
	 */
	static private ImageIcon generate(String filename) {
		try {
			InputStream in = IOUtils.openResource(IconGenerator.class, filename);
			if (in != null) {
				try {
					return new ImageIcon(IOUtils.toByteArray(in));
				} finally {
					in.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
