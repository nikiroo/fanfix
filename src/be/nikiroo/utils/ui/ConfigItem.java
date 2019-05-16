package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import be.nikiroo.utils.resources.Bundle;
import be.nikiroo.utils.resources.Meta.Format;
import be.nikiroo.utils.resources.MetaInfo;

/**
 * A graphical item that reflect a configuration option from the given
 * {@link Bundle}.
 * 
 * @author niki
 * 
 * @param <E>
 *            the type of {@link Bundle} to edit
 */
public class ConfigItem<E extends Enum<E>> extends JPanel {
	private static final long serialVersionUID = 1L;

	public ConfigItem(final MetaInfo<E> info) {
		this.setLayout(new BorderLayout());

		if (info.getFormat() == Format.BOOLEAN) {
			final JCheckBox field = new JCheckBox();
			field.setToolTipText(info.getDescription());
			Boolean state = info.getBoolean();
			if (state == null) {
				info.getDefaultBoolean();
			}

			// Should not happen!
			if (state == null) {
				System.err
						.println("No default value given for BOOLEAN parameter \""
								+ info.getName()
								+ "\", we consider it is FALSE");
				state = false;
			}

			field.setSelected(state);

			info.addReloadedListener(new Runnable() {
				@Override
				public void run() {
					Boolean state = info.getBoolean();
					if (state == null) {
						info.getDefaultBoolean();
					}
					if (state == null) {
						state = false;
					}

					field.setSelected(state);
				}
			});
			info.addSaveListener(new Runnable() {
				@Override
				public void run() {
					info.setBoolean(field.isSelected());
				}
			});

			field.setText(info.getName());
			this.add(field, BorderLayout.CENTER);
		} else if (info.getFormat() == Format.COLOR) {
			final JTextField field = new JTextField();
			field.setToolTipText(info.getDescription());
			field.setText(info.getString());

			info.addReloadedListener(new Runnable() {
				@Override
				public void run() {
					field.setText(info.getString());
				}
			});
			info.addSaveListener(new Runnable() {
				@Override
				public void run() {
					info.setString(field.getText());
				}
			});

			this.add(label(info.getName()), BorderLayout.WEST);
			JPanel pane = new JPanel(new BorderLayout());

			final JButton colorWheel = new JButton();
			colorWheel.setIcon(getIcon(17, info.getColor()));
			colorWheel.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					Color initialColor = new Color(info.getColor(), true);
					Color newColor = JColorChooser.showDialog(ConfigItem.this,
							info.getName(), initialColor);
					if (newColor != null) {
						info.setColor(newColor.getRGB());
						field.setText(info.getString());
						colorWheel.setIcon(getIcon(17, info.getColor()));
					}
				}
			});
			pane.add(colorWheel, BorderLayout.WEST);
			pane.add(field, BorderLayout.CENTER);
			this.add(pane, BorderLayout.CENTER);
		} else {
			final JTextField field = new JTextField();
			field.setToolTipText(info.getDescription());
			field.setText(info.getString());

			info.addReloadedListener(new Runnable() {
				@Override
				public void run() {
					field.setText(info.getString());
				}
			});
			info.addSaveListener(new Runnable() {
				@Override
				public void run() {
					info.setString(field.getText());
				}
			});

			this.add(label(info.getName()), BorderLayout.WEST);
			this.add(field, BorderLayout.CENTER);
		}
	}

	/**
	 * Create a label which width is constrained in lock steps.
	 * 
	 * @param text
	 *            the text of the label
	 * 
	 * @return the label
	 */
	private JLabel label(String text) {
		final JLabel label = new JLabel(text);

		Dimension ps = label.getPreferredSize();
		if (ps == null) {
			ps = label.getSize();
		}

		int w = ps.width;
		int step = 80;
		for (int i = 2 * step; i < 10 * step; i += step) {
			if (w < i) {
				w = i;
				break;
			}
		}

		ps.width = w;
		label.setSize(ps);
		label.setPreferredSize(ps);

		return label;
	}

	/**
	 * Return an {@link Icon} to use as a colour badge for the colour field
	 * controls.
	 * 
	 * @param size
	 *            the size of the badge
	 * @param color
	 *            the colour of the badge
	 * 
	 * @return the badge
	 */
	private Icon getIcon(int size, int color) {
		Color c = new Color(color, true);
		int avg = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
		Color border = (avg >= 128 ? Color.BLACK : Color.WHITE);

		BufferedImage img = new BufferedImage(size, size,
				BufferedImage.TYPE_4BYTE_ABGR);

		Graphics2D g = img.createGraphics();
		try {
			g.setColor(c);
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
			g.setColor(border);
			g.drawRect(0, 0, img.getWidth() - 1, img.getHeight() - 1);
		} finally {
			g.dispose();
		}

		return new ImageIcon(img);
	}
}
