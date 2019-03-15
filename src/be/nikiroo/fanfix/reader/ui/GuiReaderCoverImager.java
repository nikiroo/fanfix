package be.nikiroo.fanfix.reader.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.ui.ImageUtilsAwt;
import be.nikiroo.utils.ui.UIUtils;

/**
 * This class can create a cover icon ready to use for the graphical
 * application.
 * 
 * @author niki
 */
class GuiReaderCoverImager {

	// TODO: export some of the configuration options?
	private static final int COVER_WIDTH = 100;
	private static final int COVER_HEIGHT = 150;
	private static final int SPINE_WIDTH = 5;
	private static final int SPINE_HEIGHT = 5;
	private static final int HOFFSET = 20;
	private static final Color SPINE_COLOR_BOTTOM = new Color(180, 180, 180);
	private static final Color SPINE_COLOR_RIGHT = new Color(100, 100, 100);
	private static final Color BORDER = Color.black;

	public static final int TEXT_HEIGHT = 50;
	public static final int TEXT_WIDTH = COVER_WIDTH + 40;

	//

	/**
	 * Draw a partially transparent overlay if needed depending upon the
	 * selection and mouse-hover states on top of the normal component, as well
	 * as a possible "cached" icon if the item is cached.
	 * 
	 * @param g
	 *            the {@link Graphics} to paint onto
	 * @param enabled
	 *            draw an enabled overlay
	 * @param selected
	 *            draw a selected overlay
	 * @param hovered
	 *            draw a hovered overlay
	 * @param cached
	 *            draw a cached overlay
	 */
	static public void paintOverlay(Graphics g, boolean enabled,
			boolean selected, boolean hovered, boolean cached) {
		Rectangle clip = g.getClipBounds();
		if (clip.getWidth() <= 0 || clip.getHeight() <= 0) {
			return;
		}

		int h = COVER_HEIGHT;
		int w = COVER_WIDTH;
		int xOffset = (TEXT_WIDTH - COVER_WIDTH) - 1;
		int yOffset = HOFFSET;

		if (BORDER != null) {
			if (BORDER != null) {
				g.setColor(BORDER);
				g.drawRect(xOffset, yOffset, COVER_WIDTH, COVER_HEIGHT);
			}

			xOffset++;
			yOffset++;
		}

		int[] xs = new int[] { xOffset, xOffset + SPINE_WIDTH,
				xOffset + w + SPINE_WIDTH, xOffset + w };
		int[] ys = new int[] { yOffset + h, yOffset + h + SPINE_HEIGHT,
				yOffset + h + SPINE_HEIGHT, yOffset + h };
		g.setColor(SPINE_COLOR_BOTTOM);
		g.fillPolygon(new Polygon(xs, ys, xs.length));
		xs = new int[] { xOffset + w, xOffset + w + SPINE_WIDTH,
				xOffset + w + SPINE_WIDTH, xOffset + w };
		ys = new int[] { yOffset, yOffset + SPINE_HEIGHT,
				yOffset + h + SPINE_HEIGHT, yOffset + h };
		g.setColor(SPINE_COLOR_RIGHT);
		g.fillPolygon(new Polygon(xs, ys, xs.length));

		Color color = new Color(255, 255, 255, 0);
		if (!enabled) {
		} else if (selected && !hovered) {
			color = new Color(80, 80, 100, 40);
		} else if (!selected && hovered) {
			color = new Color(230, 230, 255, 100);
		} else if (selected && hovered) {
			color = new Color(200, 200, 255, 100);
		}

		g.setColor(color);
		g.fillRect(clip.x, clip.y, clip.width, clip.height);

		if (cached) {
			UIUtils.drawEllipse3D(g, Color.green.darker(), COVER_WIDTH
					+ HOFFSET + 30, 10, 20, 20);
		}
	}

	/**
	 * Generate a cover icon based upon the given {@link MetaData}.
	 * 
	 * @param lib
	 *            the library the meta comes from
	 * @param meta
	 *            the {@link MetaData}
	 * 
	 * @return the icon
	 */
	static public ImageIcon generateCoverIcon(BasicLibrary lib, MetaData meta) {
		BufferedImage resizedImage = null;
		String id = getIconId(meta);

		InputStream in = Instance.getCache().getFromCache(id);
		if (in != null) {
			try {
				resizedImage = ImageUtilsAwt.fromImage(new Image(in));
				in.close();
				in = null;
			} catch (IOException e) {
				Instance.getTraceHandler().error(e);
			}
		}

		if (resizedImage == null) {
			try {
				Image cover = null;
				if (meta.getLuid() != null) {
					cover = lib.getCover(meta.getLuid());
				}
				if (cover == null) {
					cover = lib.getSourceCover(meta.getSource());
				}

				resizedImage = new BufferedImage(SPINE_WIDTH + COVER_WIDTH,
						SPINE_HEIGHT + COVER_HEIGHT + HOFFSET,
						BufferedImage.TYPE_4BYTE_ABGR);
				Graphics2D g = resizedImage.createGraphics();
				g.setColor(Color.white);
				g.fillRect(0, HOFFSET, COVER_WIDTH, COVER_HEIGHT);
				if (cover != null) {
					BufferedImage coverb = ImageUtilsAwt.fromImage(cover);
					g.drawImage(coverb, 0, HOFFSET, COVER_WIDTH, COVER_HEIGHT,
							null);
				} else {
					g.setColor(Color.black);
					g.drawLine(0, HOFFSET, COVER_WIDTH, HOFFSET + COVER_HEIGHT);
					g.drawLine(COVER_WIDTH, HOFFSET, 0, HOFFSET + COVER_HEIGHT);
				}
				g.dispose();

				if (id != null) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					ImageIO.write(resizedImage, "png", out);
					byte[] imageBytes = out.toByteArray();
					in = new ByteArrayInputStream(imageBytes);
					Instance.getCache().addToCache(in, id);
					in.close();
					in = null;
				}
			} catch (MalformedURLException e) {
				Instance.getTraceHandler().error(e);
			} catch (IOException e) {
				Instance.getTraceHandler().error(e);
			}
		}

		return new ImageIcon(resizedImage);
	}

	/**
	 * Manually clear the icon set for this item.
	 * 
	 * @param meta
	 *            the meta of the story or source (if luid is null)
	 */
	static public void clearIcon(MetaData meta) {
		String id = getIconId(meta);
		Instance.getCache().removeFromCache(id);
	}

	/**
	 * Get a unique ID from this meta (note that if the luid is null, it is
	 * considered a source and not a {@link Story}).
	 * 
	 * @param meta
	 *            the meta
	 * @return the unique ID
	 */
	static private String getIconId(MetaData meta) {
		String id = null;

		String key = meta.getUuid();
		if (meta.getLuid() == null) {
			// a fake meta (== a source)
			key = "source_" + meta.getSource();
		}

		id = key + ".thumb_" + SPINE_WIDTH + "x" + COVER_WIDTH + "+"
				+ SPINE_HEIGHT + "+" + COVER_HEIGHT + "@" + HOFFSET;

		return id;
	}
}
