package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicArrowButton;

import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.StringUtils.Alignment;
import be.nikiroo.utils.resources.Bundle;
import be.nikiroo.utils.resources.Meta.Format;
import be.nikiroo.utils.resources.MetaInfo;

/**
 * A graphical item that reflect a configuration option from the given
 * {@link Bundle}.
 * <p>
 * This graphical item can be edited, and the result will be saved back into the
 * linked {@link MetaInfo}; you still have to save the {@link MetaInfo} should
 * you wish to, of course.
 * 
 * @author niki
 * 
 * @param <E>
 *            the type of {@link Bundle} to edit
 */
public class ConfigItem<E extends Enum<E>> extends JPanel {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@link ConfigItem} for the given {@link MetaInfo}.
	 * 
	 * @param info
	 *            the {@link MetaInfo}
	 */
	public ConfigItem(MetaInfo<E> info) {
		this.setLayout(new BorderLayout());

		// TODO: support arrays
		Format fmt = info.getFormat();
		if (info.isArray()) {
			fmt = Format.STRING;
		}

		switch (fmt) {
		case BOOLEAN:
			addBooleanField(info);
			break;
		case COLOR:
			addColorField(info);
			break;
		case FILE:
			addBrowseField(info, false);
			break;
		case DIRECTORY:
			addBrowseField(info, true);
			break;
		case COMBO_LIST:
			addComboboxField(info, true);
			break;
		case FIXED_LIST:
			addComboboxField(info, false);
			break;
		case INT:
			addIntField(info);
			break;
		case PASSWORD:
			addPasswordField(info);
			break;
		case STRING:
		case LOCALE: // TODO?
		default:
			addStringField(info);
			break;
		}
	}

	private void addStringField(final MetaInfo<E> info) {
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

		this.add(label(info), BorderLayout.WEST);
		this.add(field, BorderLayout.CENTER);
	}

	private void addBooleanField(final MetaInfo<E> info) {
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
							+ info.getName() + "\", we consider it is FALSE");
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
	}

	private void addColorField(final MetaInfo<E> info) {
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

		this.add(label(info), BorderLayout.WEST);
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
	}

	private void addBrowseField(final MetaInfo<E> info, final boolean dir) {
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

		JButton browseButton = new JButton("...");
		browseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(null);
				chooser.setFileSelectionMode(dir ? JFileChooser.DIRECTORIES_ONLY
						: JFileChooser.FILES_ONLY);
				if (chooser.showOpenDialog(ConfigItem.this) == JFileChooser.APPROVE_OPTION) {
					File file = chooser.getSelectedFile();
					if (file != null) {
						info.setString(file.getAbsolutePath());
						field.setText(info.getString());
					}
				}
			}
		});

		JPanel pane = new JPanel(new BorderLayout());
		this.add(label(info), BorderLayout.WEST);
		pane.add(browseButton, BorderLayout.WEST);
		pane.add(field, BorderLayout.CENTER);
		this.add(pane, BorderLayout.CENTER);
	}

	private void addComboboxField(final MetaInfo<E> info, boolean editable) {
		// rawtypes for Java 1.6 (and 1.7 ?) support
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final JComboBox field = new JComboBox(info.getAllowedValues());
		field.setEditable(editable);
		field.setSelectedItem(info.getString());

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				field.setSelectedItem(info.getString());
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				info.setString(field.getSelectedItem().toString());
			}
		});

		this.add(label(info), BorderLayout.WEST);
		this.add(field, BorderLayout.CENTER);
	}

	private void addPasswordField(final MetaInfo<E> info) {
		final JPasswordField field = new JPasswordField();
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
				info.setString(new String(field.getPassword()));
			}
		});

		this.add(label(info), BorderLayout.WEST);
		this.add(field, BorderLayout.CENTER);
	}

	private void addIntField(final MetaInfo<E> info) {
		final JTextField field = new JTextField();
		field.setToolTipText(info.getDescription());
		field.setText(info.getString());
		field.setInputVerifier(new InputVerifier() {
			@Override
			public boolean verify(JComponent input) {
				String text = field.getText().trim();
				if (text.startsWith("-")) {
					text = text.substring(1).trim();
				}

				return text.replaceAll("[0-9]", "").isEmpty();
			}
		});

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
				Integer value = info.getInteger();
				if (value == null) {
					info.setString("");
				} else {
					info.setInteger(value);
				}
				field.setText(info.getString());
			}
		});

		JButton up = new BasicArrowButton(BasicArrowButton.NORTH);
		JButton down = new BasicArrowButton(BasicArrowButton.SOUTH);

		up.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				int value = 0;
				try {
					value = Integer.parseInt(field.getText());
				} catch (NumberFormatException e) {
				}

				field.setText(Integer.toString(value + 1));
			}
		});

		down.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				int value = 0;
				try {
					value = Integer.parseInt(field.getText());
				} catch (NumberFormatException e) {
				}

				field.setText(Integer.toString(value - 1));
			}
		});

		JPanel upDown = new JPanel(new BorderLayout());
		upDown.add(up, BorderLayout.NORTH);
		upDown.add(down, BorderLayout.SOUTH);

		JPanel pane = new JPanel(new BorderLayout());
		pane.add(upDown, BorderLayout.WEST);
		pane.add(field, BorderLayout.CENTER);

		this.add(label(info), BorderLayout.WEST);
		this.add(pane, BorderLayout.CENTER);
	}

	/**
	 * Create a label which width is constrained in lock steps.
	 * 
	 * @param info
	 *            the {@link MetaInfo} for which we want to add a label
	 * 
	 * @return the label
	 */
	private JComponent label(final MetaInfo<E> info) {
		final JLabel label = new JLabel(info.getName());

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

		// TODO: image
		JButton help = new JButton("?");
		help.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				StringBuilder builder = new StringBuilder();
				String text = info.getDescription().replace("\\n", "\n");
				for (String line : StringUtils.justifyText(text, 80,
						Alignment.LEFT)) {
					if (builder.length() > 0) {
						builder.append("\n");
					}
					builder.append(line);
				}
				text = builder.toString();
				JOptionPane.showMessageDialog(ConfigItem.this, text);
			}
		});

		JPanel pane2 = new JPanel(new BorderLayout());
		pane2.add(help, BorderLayout.WEST);
		pane2.add(new JLabel(" "), BorderLayout.CENTER);

		JPanel pane = new JPanel(new BorderLayout());
		pane.add(label, BorderLayout.WEST);
		pane.add(pane2, BorderLayout.CENTER);

		ps.width = w + 30; // 30 for the (?) sign
		pane.setSize(ps);
		pane.setPreferredSize(ps);

		return pane;
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
