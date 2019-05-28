package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

import be.nikiroo.utils.resources.MetaInfo;

public class ConfigItemColor<E extends Enum<E>> extends ConfigItem<E> {
	private static final long serialVersionUID = 1L;

	private Map<JComponent, JTextField> fields = new HashMap<JComponent, JTextField>();

	/**
	 * Create a new {@link ConfigItemColor} for the given {@link MetaInfo}.
	 * 
	 * @param info
	 *            the {@link MetaInfo}
	 */
	public ConfigItemColor(MetaInfo<E> info) {
		super(info, true);
	}

	@Override
	protected Object getFromField(int item) {
		JTextField field = fields.get(getField(item));
		if (field != null) {
			return field.getText();
		}

		return null;
	}

	@Override
	protected Object getFromInfo(int item) {
		return info.getString(item, false);
	}

	@Override
	protected void setToField(Object value, int item) {
		JTextField field = fields.get(getField(item));
		if (field != null) {
			field.setText(value == null ? "" : value.toString());
		}
		// TODO: change color too
	}

	@Override
	protected void setToInfo(Object value, int item) {
		info.setString((String) value, item);
	}

	private int getFromInfoColor(int item) {
		Integer color = info.getColor(item, true);
		if (color == null) {
			return new Color(255, 255, 255, 255).getRGB();
		}

		return color;
	}

	@Override
	protected JComponent createField(final int item) {
		final JPanel pane = new JPanel(new BorderLayout());
		final JTextField field = new JTextField();

		final JButton colorWheel = new JButton();
		colorWheel.setIcon(getIcon(17, getFromInfoColor(item)));
		colorWheel.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int icol = getFromInfoColor(item);
				Color initialColor = new Color(icol, true);
				Color newColor = JColorChooser.showDialog(ConfigItemColor.this,
						info.getName(), initialColor);
				if (newColor != null) {
					info.setColor(newColor.getRGB(), item);
					field.setText(info.getString(item, false));
					colorWheel.setIcon(getIcon(17, info.getColor(item, true)));
				}
			}
		});

		pane.add(colorWheel, BorderLayout.WEST);
		pane.add(field, BorderLayout.CENTER);

		fields.put(pane, field);
		return pane;
	}

	/**
	 * Return an {@link Icon} to use as a colour badge for the colour field
	 * controls.
	 * 
	 * @param size
	 *            the size of the badge
	 * @param color
	 *            the colour of the badge, which can be NULL (will return
	 *            transparent white)
	 * 
	 * @return the badge
	 */
	static private Icon getIcon(int size, Integer color) {
		// Allow null values
		if (color == null) {
			color = new Color(255, 255, 255, 255).getRGB();
		}

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
