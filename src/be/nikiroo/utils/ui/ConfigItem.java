package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import be.nikiroo.utils.Image;
import be.nikiroo.utils.StringUtils;
import be.nikiroo.utils.StringUtils.Alignment;
import be.nikiroo.utils.resources.Bundle;
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

	private static int minimumHeight = -1;

	/** A small 16x16 "?" blue in PNG, base64 encoded. */
	private static String infoImage64 = //
	""
			+ "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBI"
			+ "WXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4wURFRg6IrtcdgAAATdJREFUOMvtkj8sQ1EUxr9z/71G"
			+ "m1RDogYxq7WDDYMYTSajSG4n6YRYzSaSLibWbiaDIGwdiLIYDFKDNJEgKu969xi8UNHy7H7LPcN3"
			+ "v/Odcy+hG9oOIeIcBCJS9MAvlZtOMtHxsrFrJHGqe0RVGnHAHpcIbPlng8BS3HmKBJYzabGUzcrJ"
			+ "XK+ckIrqANYR2JEv2nYDEVck0WKGfHzyq82Go+btxoX3XAcAIqTj8wPqOH6mtMeM4bGCLhyfhTMA"
			+ "qlLhKHqujCfaweCAmV0p50dPzsNpEKpK01V/n55HIvTnfDC2odKlfeYadZN/T+AqDACUsnkhqaU1"
			+ "LRIVuX1x7ciuSWQxVIrunONrfq3dI6oh+T94Z8453vEem/HTqT8ZpFJ0qDXtGkPbAGAMeSRngQCA"
			+ "eUvgn195AwlZWyvjtQdhAAAAAElFTkSuQmCC";

	// A small 16x16 "+" image with colours
	private static String addImage64 = //
	""
			+ "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBI"
			+ "WXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4wUeES0QBFvvnAAAAB1pVFh0Q29tbWVudAAAAAAAQ3Jl"
			+ "YXRlZCB3aXRoIEdJTVBkLmUHAAACH0lEQVQ4y42Tz0sVURTHP+fMmC7CQMpH1EjgIimCsEVBEIg/"
			+ "qIbcBAW2Uai1m/oH2rlJXLQpeRJt2gQhTO0iTTKC1I2JBf5gKCJCRPvhPOed22LmvV70Fn7hwr3c"
			+ "+z3ne+73HCFHEClxaASRHgduA91AW369BkwDI3Foy0GkEofmACQnSxyaCyItAkMClMzYdeCAJgVP"
			+ "tJJrPA7tVoUjNZlngXMAiRmXClfoK/Tjq09x7T6LW+8RxOVJ5+LQzgSRojm5WCEDlMrQVbjIQNtN"
			+ "rh0d5FTzaTLBmWKgM4h0Ig4NzWseohYCJUuqx123Sx0MBpF2+MAdyWUnlqX4lf4bIDHjR+rwJJPR"
			+ "qNCgCjDsA10lM/oKIRcO9lByCYklnG/pqQa4euQ6J5tPoKI0yD6ef33Ku40Z80R7CSJNWyZxT+Ki"
			+ "2ytGP911hyZxQaRp1RtPPPYKD4+sGJwPrDUp7Q9Xxnj9fYrUUnaszEAwQHfrZQAerT/g7cYMiuCp"
			+ "z8LmLI0qBqz6wLQn2v5he57FrXkAtlPH2ZZOuskCzG2+4dnnx3iSuSgCKqLAlAIjmXPiVIRsgYjU"
			+ "usrfO0Gq7cA9jUNbBsZrmiQnac1e6n3FeBzakpf39OSBG9IPHAZwzlFoagVg5edHXn57wZed9dpA"
			+ "C3FoYRDpf8M0AQwKwu9yubxjeA7Y72ENqlp3mOqMcwcwDPQCx8gGchV4BYzGoS1V3gL8AVA5C5/0"
			+ "oRFoAAAAAElFTkSuQmCC";

	// A small 32x32 "-" image with colours
	private static String removeImage64 = //
	""
			+ "iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAABmJLR0QA/wD/AP+gvaeTAAAACXBI"
			+ "WXMAAAsTAAALEwEAmpwYAAAAB3RJTUUH4wUeESw5X/JGsQAAAB1pVFh0Q29tbWVudAAAAAAAQ3Jl"
			+ "YXRlZCB3aXRoIEdJTVBkLmUHAAACKUlEQVQ4y5WTO2iTYRSG3+//v/+SJrG5SSABh1JQBHFJNUNR"
			+ "YodCLoMoTkK0YKhQtBmsl01wKVZRBwcrgosg3SwFW9Cippe0VmlpB6uYqYIaNSZtbv/lOKRx0iR9"
			+ "4YOzvOc8vOd8wLbG4nYGAKP9tshKr3Pq0zFXORt0UzbopvUeZ2ml1/niUcIWAYBzwwqr+xgAjCSt"
			+ "wpXjWzx105Ha+1XsMgT8U6IJfPAacyfO50OXJi3VwbtbxMbidtZ3tiClbzi/eAuCmxgai4AfNvNn"
			+ "KJn3X5xWKgwA0lHHYud3MdDUXMcmIOMx0oGJXJCN9tuiJ98p4//DbtTk2cFKhB/OSBcMgQHVMkir"
			+ "AqwJBhGYrIIkCQc2eJK3aewI9Crko2FIh0K1Jo0mcwmV6XFUlmfRXhK7eXuRKaRVIYdiUGKnW8Kn"
			+ "0ia0t6/hKHJVqCcLzncQgLhtIvBfbWbZZahq+cl96AuvQLre2Mw59NUlkCwjZ6USL0uYgSj26B/X"
			+ "oK+vtkYgMAhMRF4x5oWlPdod0UQtfUFo7YEBBKz59BEGAAtRx1xHVgzu5AYyHmMmMJHrZolhhU3t"
			+ "05XJe7s2PJuCq9k1MgKyNjOXiBf8kWW5JDy4XKHBl2ql6+pvX8ZjzDOqrcWsFQAAE/T3H3z2GG/6"
			+ "zhT8sfdKeehWkUQAeJ7WcH23xTz1uPBwf1hclA3mBZjPojFOIOSsVPpmN1OznfpA+Gn+2kCHqg/d"
			+ "LhIA/AFU5d0V6gTjtQAAAABJRU5ErkJggg==";

	/** The original value before current changes. */
	private Object orig;
	private List<Integer> dirtyBits;

	private JComponent field;
	private List<JComponent> fields = new ArrayList<JComponent>();

	/** The {@link MetaInfo} linked to the field. */
	protected MetaInfo<E> info;

	/**
	 * Create a new {@link ConfigItem} for the given {@link MetaInfo}.
	 * 
	 * @param info
	 *            the {@link MetaInfo}
	 * @param nhgap
	 *            negative horisontal gap in pixel to use for the label, i.e.,
	 *            the step lock sized labels will start smaller by that amount
	 *            (the use case would be to align controls that start at a
	 *            different horisontal position)
	 */
	public ConfigItem(MetaInfo<E> info, int nhgap) {
		this(info, true);

		ConfigItem<E> configItem;
		switch (info.getFormat()) {
		case BOOLEAN:
			configItem = new ConfigItemBoolean<E>(info);
			break;
		case COLOR:
			configItem = new ConfigItemColor<E>(info);
			break;
		case FILE:
			configItem = new ConfigItemBrowse<E>(info, false);
			break;
		case DIRECTORY:
			configItem = new ConfigItemBrowse<E>(info, true);
			break;
		case COMBO_LIST:
			configItem = new ConfigItemCombobox<E>(info, true);
			break;
		case FIXED_LIST:
			configItem = new ConfigItemCombobox<E>(info, false);
			break;
		case INT:
			configItem = new ConfigItemInteger<E>(info);
			break;
		case PASSWORD:
			configItem = new ConfigItemPassword<E>(info);
			break;
		case STRING:
		case LOCALE: // TODO?
		default:
			configItem = new ConfigItemString<E>(info);
			break;
		}

		if (info.isArray()) {
			this.setLayout(new BorderLayout());
			add(label(nhgap), BorderLayout.WEST);

			final JPanel main = new JPanel();
			main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
			int size = info.getListSize(false);
			for (int i = 0; i < size; i++) {
				JComponent field = configItem.createComponent(i);
				main.add(field);
			}

			final JButton add = new JButton();
			setImage(add, addImage64, "+");

			final ConfigItem<E> fconfigItem = configItem;
			add.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					JComponent field = fconfigItem
							.createComponent(fconfigItem.info
									.getListSize(false));
					main.add(field);

					main.revalidate();
					main.repaint();
				}
			});

			JPanel tmp = new JPanel(new BorderLayout());
			tmp.add(add, BorderLayout.WEST);

			JPanel mainPlus = new JPanel(new BorderLayout());
			mainPlus.add(main, BorderLayout.CENTER);
			mainPlus.add(tmp, BorderLayout.SOUTH);

			add(mainPlus, BorderLayout.CENTER);
		} else {
			this.setLayout(new BorderLayout());
			add(label(nhgap), BorderLayout.WEST);

			JComponent field = configItem.createComponent(-1);
			add(field, BorderLayout.CENTER);
		}
	}

	/**
	 * Prepare a new {@link ConfigItem} instance, linked to the given
	 * {@link MetaInfo}.
	 * 
	 * @param info
	 *            the info
	 * @param autoDirtyHandling
	 *            TRUE to automatically manage the setDirty/Save operations,
	 *            FALSE if you want to do it yourself via
	 *            {@link ConfigItem#setDirtyItem(int)}
	 */
	protected ConfigItem(MetaInfo<E> info, boolean autoDirtyHandling) {
		this.info = info;
		if (!autoDirtyHandling) {
			dirtyBits = new ArrayList<Integer>();
		}
	}

	/**
	 * Create an empty graphical component to be used later by
	 * {@link ConfigItem#getField(int)}.
	 * <p>
	 * Note that {@link ConfigItem#reload(int)} will be called after it was
	 * created.
	 * 
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 * 
	 * @return the graphical component
	 */
	protected JComponent createField(@SuppressWarnings("unused") int item) {
		// Not used by the main class, only the sublasses
		return null;
	}

	/**
	 * Get the information from the {@link MetaInfo} in the subclass preferred
	 * format.
	 * 
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 * 
	 * @return the information in the subclass preferred format
	 */
	protected Object getFromInfo(@SuppressWarnings("unused") int item) {
		// Not used by the main class, only the subclasses
		return null;
	}

	/**
	 * Set the value to the {@link MetaInfo}.
	 * 
	 * @param value
	 *            the value in the subclass preferred format
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 */
	protected void setToInfo(@SuppressWarnings("unused") Object value,
			@SuppressWarnings("unused") int item) {
		// Not used by the main class, only the subclasses
	}

	/**
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 * 
	 * @return
	 */
	protected Object getFromField(@SuppressWarnings("unused") int item) {
		// Not used by the main class, only the subclasses
		return null;
	}

	/**
	 * Set the value (in the subclass preferred format) into the field.
	 * 
	 * @param value
	 *            the value in the subclass preferred format
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 */
	protected void setToField(@SuppressWarnings("unused") Object value,
			@SuppressWarnings("unused") int item) {
		// Not used by the main class, only the subclasses
	}

	/**
	 * Create a new field for the given graphical component at the given index
	 * (note that the component is usually created by
	 * {@link ConfigItem#createField(int)}).
	 * 
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 * @param field
	 *            the graphical component
	 */
	private void setField(int item, JComponent field) {
		if (item < 0) {
			this.field = field;
			return;
		}

		for (int i = fields.size(); i <= item; i++) {
			fields.add(null);
		}

		fields.set(item, field);
	}

	/**
	 * Retrieve the associated graphical component that was created with
	 * {@link ConfigItem#createField(int)}.
	 * 
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 * 
	 * @return the graphical component
	 */
	protected JComponent getField(int item) {
		if (item < 0) {
			return field;
		}

		if (item < fields.size()) {
			return fields.get(item);
		}

		return null;
	}

	/**
	 * Manually specify that the given item is "dirty" and thus should be saved
	 * when asked.
	 * <p>
	 * Has no effect if the class is using automatic dirty handling (see
	 * {@link ConfigItem#ConfigItem(MetaInfo, boolean)}).
	 * 
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 */
	protected void setDirtyItem(int item) {
		if (dirtyBits != null) {
			dirtyBits.add(item);
		}
	}

	/**
	 * Check if the value changed since the last load/save into the linked
	 * {@link MetaInfo}.
	 * <p>
	 * Note that we consider NULL and an Empty {@link String} to be equals.
	 * 
	 * @param value
	 *            the value to test
	 * 
	 * @return TRUE if it has
	 */
	protected boolean hasValueChanged(Object value) {
		// We consider "" and NULL to be equals
		return !orig.equals(value == null ? "" : value);
	}

	/**
	 * Reload the values to what they currently are in the {@link MetaInfo}.
	 * 
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 */
	protected void reload(int item) {
		Object value = getFromInfo(item);
		setToField(value, item);

		// We consider "" and NULL to be equals
		orig = (value == null ? "" : value);
	}

	/**
	 * If the item has been modified, set the {@link MetaInfo} to dirty then
	 * modify it to, reflect the changes so it can be saved later.
	 * <p>
	 * This method does <b>not</b> call {@link MetaInfo#save(boolean)}.
	 * 
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 */
	protected void save(int item) {
		Object value = getFromField(item);

		boolean dirty = false;
		if (dirtyBits != null) {
			dirty = dirtyBits.remove((Integer) item);
		} else {
			// We consider "" and NULL to be equals
			dirty = hasValueChanged(value);
		}

		if (dirty) {
			info.setDirty();
			setToInfo(value, item);
			orig = (value == null ? "" : value);
		}
	}

	/**
	 * 
	 * @param item
	 *            the item number to get for an array of values, or -1 to get
	 *            the whole value (has no effect if {@link MetaInfo#isArray()}
	 *            is FALSE)
	 * @param addTo
	 * @param nhgap
	 * 
	 * @return
	 */
	protected JComponent createComponent(final int item) {
		setField(item, createField(item));
		reload(item);

		info.addReloadedListener(new Runnable() {
			@Override
			public void run() {
				reload(item);
			}
		});
		info.addSaveListener(new Runnable() {
			@Override
			public void run() {
				save(item);
			}
		});

		JComponent field = getField(item);
		setPreferredSize(field);

		return field;
	}

	/**
	 * Create a label which width is constrained in lock steps.
	 * 
	 * @param nhgap
	 *            negative horisontal gap in pixel to use for the label, i.e.,
	 *            the step lock sized labels will start smaller by that amount
	 *            (the use case would be to align controls that start at a
	 *            different horisontal position)
	 * 
	 * @return the label
	 */
	protected JComponent label(int nhgap) {
		final JLabel label = new JLabel(info.getName());

		Dimension ps = label.getPreferredSize();
		if (ps == null) {
			ps = label.getSize();
		}

		ps.height = Math.max(ps.height, getMinimumHeight());

		int w = ps.width;
		int step = 150;
		for (int i = 2 * step - nhgap; i < 10 * step; i += step) {
			if (w < i) {
				w = i;
				break;
			}
		}

		final Runnable showInfo = new Runnable() {
			@Override
			public void run() {
				StringBuilder builder = new StringBuilder();
				String text = (info.getDescription().replace("\\n", "\n"))
						.trim();
				for (String line : StringUtils.justifyText(text, 80,
						Alignment.LEFT)) {
					if (builder.length() > 0) {
						builder.append("\n");
					}
					builder.append(line);
				}
				text = builder.toString();
				JOptionPane.showMessageDialog(ConfigItem.this, text,
						info.getName(), JOptionPane.INFORMATION_MESSAGE);
			}
		};

		JLabel help = new JLabel("");
		help.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		setImage(help, infoImage64, "?");

		help.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				showInfo.run();
			}
		});

		JPanel pane2 = new JPanel(new BorderLayout());
		pane2.add(help, BorderLayout.WEST);
		pane2.add(new JLabel(" "), BorderLayout.CENTER);

		JPanel contentPane = new JPanel(new BorderLayout());
		contentPane.add(label, BorderLayout.WEST);
		contentPane.add(pane2, BorderLayout.CENTER);

		ps.width = w + 30; // 30 for the (?) sign
		contentPane.setSize(ps);
		contentPane.setPreferredSize(ps);

		JPanel pane = new JPanel(new BorderLayout());
		pane.add(contentPane, BorderLayout.NORTH);

		return pane;
	}

	protected void setPreferredSize(JComponent field) {
		int height = Math
				.max(getMinimumHeight(), field.getMinimumSize().height);
		setPreferredSize(new Dimension(200, height));
	}

	static private int getMinimumHeight() {
		if (minimumHeight < 0) {
			minimumHeight = new JTextField("Test").getMinimumSize().height;
		}

		return minimumHeight;
	}

	/**
	 * Set an image to the given {@link JButton}, with a fallback text if it
	 * fails.
	 * 
	 * @param button
	 *            the button to set
	 * @param image64
	 *            the image in BASE64 (should be PNG or similar)
	 * @param fallbackText
	 *            text to use in case the image cannot be created
	 */
	static private void setImage(JLabel button, String image64,
			String fallbackText) {
		try {
			Image img = new Image(image64);
			try {
				BufferedImage bImg = ImageUtilsAwt.fromImage(img);
				button.setIcon(new ImageIcon(bImg));
			} finally {
				img.close();
			}
		} catch (IOException e) {
			// This is an hard-coded image, should not happen
			button.setText(fallbackText);
		}
	}

	/**
	 * Set an image to the given {@link JButton}, with a fallback text if it
	 * fails.
	 * 
	 * @param button
	 *            the button to set
	 * @param image64
	 *            the image in BASE64 (should be PNG or similar)
	 * @param fallbackText
	 *            text to use in case the image cannot be created
	 */
	static private void setImage(JButton button, String image64,
			String fallbackText) {
		try {
			Image img = new Image(image64);
			try {
				BufferedImage bImg = ImageUtilsAwt.fromImage(img);
				button.setIcon(new ImageIcon(bImg));
			} finally {
				img.close();
			}
		} catch (IOException e) {
			// This is an hard-coded image, should not happen
			button.setText(fallbackText);
		}
	}
}
