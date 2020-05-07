package be.nikiroo.utils.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.VersionCheck;

/**
 * Some Java Swing utilities.
 * 
 * @author niki
 */
public class UIUtils {
	/**
	 * Set a fake "native Look &amp; Feel" for the application if possible
	 * (check for the one currently in use, then try GTK).
	 * <p>
	 * <b>Must</b> be called prior to any GUI work.
	 * 
	 * @return TRUE if it succeeded
	 */
	static public boolean setLookAndFeel() {
		// native look & feel
		String noLF = "javax.swing.plaf.metal.MetalLookAndFeel";
		String lf = UIManager.getSystemLookAndFeelClassName();
		if (lf.equals(noLF))
			lf = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
		
		return setLookAndFeel(lf);
	}

	/**
	 * Switch to the given Look &amp; Feel for the application if possible
	 * (check for the one currently in use, then try GTK).
	 * <p>
	 * <b>Must</b> be called prior to any GUI work.
	 * 
	 * @param laf
	 *            the Look &amp; Feel to use
	 * 
	 * @return TRUE if it succeeded
	 */
	static public boolean setLookAndFeel(String laf) {
		try {
			UIManager.setLookAndFeel(laf);
			return true;
		} catch (InstantiationException e) {
		} catch (ClassNotFoundException e) {
		} catch (UnsupportedLookAndFeelException e) {
		} catch (IllegalAccessException e) {
		}

		return false;
	}

	/**
	 * Draw a 3D-looking ellipse at the given location, if the given
	 * {@link Graphics} object is compatible (with {@link Graphics2D}); draw a
	 * simple ellipse if not.
	 * 
	 * @param g
	 *            the {@link Graphics} to draw on
	 * @param color
	 *            the base colour
	 * @param x
	 *            the X coordinate
	 * @param y
	 *            the Y coordinate
	 * @param width
	 *            the width radius
	 * @param height
	 *            the height radius
	 */
	static public void drawEllipse3D(Graphics g, Color color, int x, int y,
			int width, int height) {
		drawEllipse3D(g, color, x, y, width, height, true);
	}

	/**
	 * Draw a 3D-looking ellipse at the given location, if the given
	 * {@link Graphics} object is compatible (with {@link Graphics2D}); draw a
	 * simple ellipse if not.
	 * 
	 * @param g
	 *            the {@link Graphics} to draw on
	 * @param color
	 *            the base colour
	 * @param x
	 *            the X coordinate of the upper left corner
	 * @param y
	 *            the Y coordinate of the upper left corner
	 * @param width
	 *            the width radius
	 * @param height
	 *            the height radius
	 * @param fill
	 *            fill the content of the ellipse
	 */
	static public void drawEllipse3D(Graphics g, Color color, int x, int y,
			int width, int height, boolean fill) {
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);

			// Retains the previous state
			Paint oldPaint = g2.getPaint();

			// Base shape
			g2.setColor(color);
			if (fill) {
				g2.fillOval(x, y, width, height);
			} else {
				g2.drawOval(x, y, width, height);
			}

			// Compute dark/bright colours
			Paint p = null;
			Color dark = color.darker().darker();
			Color bright = color.brighter().brighter();
			Color darkEnd = new Color(dark.getRed(), dark.getGreen(),
					dark.getBlue(), 0);
			Color darkPartial = new Color(dark.getRed(), dark.getGreen(),
					dark.getBlue(), 64);
			Color brightEnd = new Color(bright.getRed(), bright.getGreen(),
					bright.getBlue(), 0);

			// Adds shadows at the bottom left
			p = new GradientPaint(0, height, dark, width, 0, darkEnd);
			g2.setPaint(p);
			if (fill) {
				g2.fillOval(x, y, width, height);
			} else {
				g2.drawOval(x, y, width, height);
			}
			// Adds highlights at the top right
			p = new GradientPaint(width, 0, bright, 0, height, brightEnd);
			g2.setPaint(p);
			if (fill) {
				g2.fillOval(x, y, width, height);
			} else {
				g2.drawOval(x, y, width, height);
			}

			// Darken the edges
			p = new RadialGradientPaint(x + width / 2f, y + height / 2f,
					Math.min(width / 2f, height / 2f), new float[] { 0f, 1f },
					new Color[] { darkEnd, darkPartial },
					RadialGradientPaint.CycleMethod.NO_CYCLE);
			g2.setPaint(p);
			if (fill) {
				g2.fillOval(x, y, width, height);
			} else {
				g2.drawOval(x, y, width, height);
			}

			// Adds inner highlight at the top right
			p = new RadialGradientPaint(x + 3f * width / 4f, y + height / 4f,
					Math.min(width / 4f, height / 4f),
					new float[] { 0.0f, 0.8f },
					new Color[] { bright, brightEnd },
					RadialGradientPaint.CycleMethod.NO_CYCLE);
			g2.setPaint(p);
			if (fill) {
				g2.fillOval(x * 2, y, width, height);
			} else {
				g2.drawOval(x * 2, y, width, height);
			}

			// Reset original paint
			g2.setPaint(oldPaint);
		} else {
			g.setColor(color);
			if (fill) {
				g.fillOval(x, y, width, height);
			} else {
				g.drawOval(x, y, width, height);
			}
		}
	}

	/**
	 * Add a {@link JScrollPane} around the given panel and use a sensible (for
	 * me) increment for the mouse wheel.
	 * 
	 * @param pane
	 *            the panel to wrap in a {@link JScrollPane}
	 * @param allowHorizontal
	 *            allow horizontal scrolling (not always desired)
	 * 
	 * @return the {@link JScrollPane}
	 */
	static public JScrollPane scroll(JComponent pane, boolean allowHorizontal) {
		return scroll(pane, allowHorizontal, true);
	}

	/**
	 * Add a {@link JScrollPane} around the given panel and use a sensible (for
	 * me) increment for the mouse wheel.
	 * 
	 * @param pane
	 *            the panel to wrap in a {@link JScrollPane}
	 * @param allowHorizontal
	 *            allow horizontal scrolling (not always desired)
	 * @param allowVertical
	 *            allow vertical scrolling (usually yes, but sometimes you only
	 *            want horizontal)
	 * 
	 * @return the {@link JScrollPane}
	 */
	static public JScrollPane scroll(JComponent pane, boolean allowHorizontal,
			boolean allowVertical) {
		JScrollPane scroll = new JScrollPane(pane);

		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.getHorizontalScrollBar().setUnitIncrement(16);

		if (!allowHorizontal) {
			scroll.setHorizontalScrollBarPolicy(
					JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		}
		if (!allowVertical) {
			scroll.setVerticalScrollBarPolicy(
					JScrollPane.VERTICAL_SCROLLBAR_NEVER);
		}

		return scroll;
	}

	/**
	 * Show a confirmation message to the user to show him the changes since
	 * last version.
	 * <p>
	 * HTML 3.2 supported, links included (the user browser will be launched if
	 * possible).
	 * <p>
	 * If this is already the latest version, a message will still be displayed.
	 * 
	 * @param parentComponent
	 *            determines the {@link java.awt.Frame} in which the dialog is
	 *            displayed; if <code>null</code>, or if the
	 *            <code>parentComponent</code> has no {@link java.awt.Frame}, a
	 *            default {@link java.awt.Frame} is used
	 * @param updates
	 *            the new version
	 * @param introText
	 *            an introduction text before the list of changes
	 * @param title
	 *            the title of the dialog
	 * 
	 * @return TRUE if the user clicked on OK, false if the dialog was dismissed
	 */
	static public boolean showUpdatedDialog(Component parentComponent,
			VersionCheck updates, String introText, String title) {
		
		StringBuilder builder = new StringBuilder();
		final JEditorPane updateMessage = new JEditorPane("text/html", "");
		if (introText != null && !introText.isEmpty()) {
			builder.append(introText);
			builder.append("<br>");
			builder.append("<br>");
		}
		for (Version v : updates.getNewer()) {
			builder.append("\t<b>" //
					+ "Version " + v.toString() //
					+ "</b>");
			builder.append("<br>");
			builder.append("<ul>");
			for (String item : updates.getChanges().get(v)) {
				builder.append("<li>" + item + "</li>");
			}
			builder.append("</ul>");
		}

		// html content
		updateMessage.setText("<html><body>" //
				+ builder//
				+ "</body></html>");

		// handle link events
		updateMessage.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
					try {
						Desktop.getDesktop().browse(e.getURL().toURI());
					} catch (IOException ee) {
						Instance.getInstance().getTraceHandler().error(ee);
					} catch (URISyntaxException ee) {
						Instance.getInstance().getTraceHandler().error(ee);
					}
			}
		});
		updateMessage.setEditable(false);
		updateMessage.setBackground(new JLabel().getBackground());
		updateMessage.addHyperlinkListener(new HyperlinkListener() {
			@Override
			public void hyperlinkUpdate(HyperlinkEvent evn) {
				if (evn.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					if (Desktop.isDesktopSupported()) {
						try {
							Desktop.getDesktop().browse(evn.getURL().toURI());
						} catch (IOException e) {
						} catch (URISyntaxException e) {
						}
					}
				}
			}
		});

		return JOptionPane.showConfirmDialog(parentComponent, updateMessage,
				title, JOptionPane.DEFAULT_OPTION) == JOptionPane.OK_OPTION;
	}
}
