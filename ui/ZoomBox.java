package be.nikiroo.utils.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

/**
 * A small panel that let you choose a zoom level or an actual zoom value (when
 * there is enough space to allow that).
 * 
 * @author niki
 */
public class ZoomBox extends ListenerPanel {
	private static final long serialVersionUID = 1L;

	/** The event that is fired on zoom change. */
	public static final String ZOOM_CHANGED = "zoom_changed";

	private enum ZoomLevel {
		FIT_TO_WIDTH(0, true), //
		FIT_TO_HEIGHT(0, false), //
		ACTUAL_SIZE(1, null), //
		HALF_SIZE(0.5, null), //
		DOUBLE_SIZE(2, null),//
		;

		private final double zoom;
		private final Boolean snapMode;

		private ZoomLevel(double zoom, Boolean snapMode) {
			this.zoom = zoom;
			this.snapMode = snapMode;
		}

		public double getZoom() {
			return zoom;
		}

		public Boolean getSnapToWidth() {
			return snapMode;
		}

		/**
		 * Use default values that can be understood by a human.
		 */
		@Override
		public String toString() {
			switch (this) {
			case FIT_TO_WIDTH:
				return "Fit to width";
			case FIT_TO_HEIGHT:
				return "Fit to height";
			case ACTUAL_SIZE:
				return "Actual size";
			case HALF_SIZE:
				return "Half size";
			case DOUBLE_SIZE:
				return "Double size";
			}
			return super.toString();
		}

		static ZoomLevel[] values(boolean orderedSelection) {
			if (orderedSelection) {
				return new ZoomLevel[] { //
						FIT_TO_WIDTH, //
						FIT_TO_HEIGHT, //
						ACTUAL_SIZE, //
						HALF_SIZE, //
						DOUBLE_SIZE,//
				};
			}

			return values();
		}
	}

	private boolean vertical;
	private boolean small;

	private JButton zoomIn;
	private JButton zoomOut;
	private JButton snapWidth;
	private JButton snapHeight;
	private JLabel zoomLabel;

	@SuppressWarnings("rawtypes") // JComboBox<?> is not java 1.6 compatible
	private JComboBox zoombox;

	private double zoom = 1;
	private Boolean snapMode = true;

	@SuppressWarnings("rawtypes") // JComboBox<?> not compatible java 1.6
	private DefaultComboBoxModel zoomBoxModel;

	/**
	 * Create a new {@link ZoomBox}.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" }) // JComboBox<?> not
													// compatible java 1.6
	public ZoomBox() {
		zoomIn = new JButton();
		zoomIn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				zoomIn(1);
			}
		});

		zoomBoxModel = new DefaultComboBoxModel(ZoomLevel.values(true));
		zoombox = new JComboBox(zoomBoxModel);
		zoombox.setEditable(true);
		zoombox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object selected = zoomBoxModel.getSelectedItem();

				if (selected == null) {
					return;
				}

				if (selected instanceof ZoomLevel) {
					ZoomLevel selectedZoomLevel = (ZoomLevel) selected;
					setZoomSnapMode(selectedZoomLevel.getZoom(),
							selectedZoomLevel.getSnapToWidth());
				} else {
					String selectedString = selected.toString();
					selectedString = selectedString.trim();
					if (selectedString.endsWith("%")) {
						selectedString = selectedString
								.substring(0, selectedString.length() - 1)
								.trim();
					}

					try {
						int pc = Integer.parseInt(selectedString);
						if (pc <= 0) {
							throw new NumberFormatException("invalid");
						}

						setZoomSnapMode(pc / 100.0, null);
					} catch (NumberFormatException nfe) {
					}
				}

				fireActionPerformed(ZOOM_CHANGED);
			}
		});

		zoomOut = new JButton();
		zoomOut.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				zoomOut(1);
			}
		});

		snapWidth = new JButton();
		snapWidth.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setSnapMode(true);
			}
		});

		snapHeight = new JButton();
		snapHeight.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setSnapMode(false);
			}
		});

		zoomLabel = new JLabel();
		zoomLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		setIcons(null, null, null, null);
		setOrientation(vertical);
	}

	/**
	 * The zoom level.
	 * <p>
	 * It usually returns 1 (default value), the value you passed yourself or 1
	 * (a snap to width or snap to height was asked by the user).
	 * <p>
	 * Will cause a fire event if needed.
	 * 
	 * @param zoom
	 *            the zoom level
	 */
	public void setZoom(double zoom) {
		if (this.zoom != zoom) {
			doSetZoom(zoom);
			fireActionPerformed(ZOOM_CHANGED);
		}
	}

	/**
	 * The snap mode (NULL means no snap mode, TRUE for snap to width, FALSE for
	 * snap to height).
	 * <p>
	 * Will cause a fire event if needed.
	 * 
	 * @param snapToWidth
	 *            the snap mode
	 */
	public void setSnapMode(Boolean snapToWidth) {
		if (this.snapMode != snapToWidth) {
			doSetSnapMode(snapToWidth);
			fireActionPerformed(ZOOM_CHANGED);
		}
	}

	/**
	 * Set both {@link ZoomBox#setZoom(double)} and
	 * {@link ZoomBox#setSnapMode(Boolean)} but fire only one change event.
	 * <p>
	 * Will cause a fire event if needed.
	 * 
	 * @param zoom
	 *            the zoom level
	 * @param snapMode
	 *            the snap mode
	 */
	public void setZoomSnapMode(double zoom, Boolean snapMode) {
		if (this.zoom != zoom || this.snapMode != snapMode) {
			doSetZoom(zoom);
			doSetSnapMode(snapMode);
			fireActionPerformed(ZOOM_CHANGED);
		}
	}

	/**
	 * The zoom level.
	 * <p>
	 * It usually returns 1 (default value), the value you passed yourself or 0
	 * (a snap to width or snap to height was asked by the user).
	 * 
	 * @return the zoom level
	 */
	public double getZoom() {
		return zoom;
	}

	/**
	 * The snap mode (NULL means no snap mode, TRUE for snap to width, FALSE for
	 * snap to height).
	 * 
	 * @return the snap mode
	 */
	public Boolean getSnapMode() {
		return snapMode;
	}

	/**
	 * Zoom in, by a certain amount in "steps".
	 * <p>
	 * Note that zoomIn(-1) is the same as zoomOut(1).
	 * 
	 * @param steps
	 *            the number of zoom steps to make, can be negative
	 */
	public void zoomIn(int steps) {
		// TODO: redo zoomIn/zoomOut correctly
		if (steps < 0) {
			zoomOut(-steps);
			return;
		}

		double newZoom = zoom;
		for (int i = 0; i < steps; i++) {
			newZoom = newZoom + (newZoom < 0.1 ? 0.01 : 0.1);
			if (newZoom > 0.1) {
				newZoom = Math.round(newZoom * 10.0) / 10.0; // snap to 10%
			} else {
				newZoom = Math.round(newZoom * 100.0) / 100.0; // snap to 1%
			}
		}

		setZoomSnapMode(newZoom, null);
		fireActionPerformed(ZOOM_CHANGED);
	}

	/**
	 * Zoom out, by a certain amount in "steps".
	 * <p>
	 * Note that zoomOut(-1) is the same as zoomIn(1).
	 * 
	 * @param steps
	 *            the number of zoom steps to make, can be negative
	 */
	public void zoomOut(int steps) {
		if (steps < 0) {
			zoomIn(-steps);
			return;
		}

		double newZoom = zoom;
		for (int i = 0; i < steps; i++) {
			newZoom = newZoom - (newZoom > 0.19 ? 0.1 : 0.01);
			if (newZoom < 0.01) {
				newZoom = 0.01;
				break;
			}

			if (newZoom > 0.1) {
				newZoom = Math.round(newZoom * 10.0) / 10.0; // snap to 10%
			} else {
				newZoom = Math.round(newZoom * 100.0) / 100.0; // snap to 1%
			}
		}

		setZoomSnapMode(newZoom, null);
		fireActionPerformed(ZOOM_CHANGED);
	}

	/**
	 * Set icons for the buttons instead of square brackets.
	 * <p>
	 * Any NULL value will make the button use square brackets again.
	 * 
	 * @param zoomIn
	 *            the icon of the button "go to first page"
	 * @param zoomOut
	 *            the icon of the button "go to previous page"
	 * @param snapWidth
	 *            the icon of the button "go to next page"
	 * @param snapHeight
	 *            the icon of the button "go to last page"
	 */
	public void setIcons(Icon zoomIn, Icon zoomOut, Icon snapWidth,
			Icon snapHeight) {
		this.zoomIn.setIcon(zoomIn);
		this.zoomIn.setText(zoomIn == null ? "+" : "");
		this.zoomOut.setIcon(zoomOut);
		this.zoomOut.setText(zoomOut == null ? "-" : "");
		this.snapWidth.setIcon(snapWidth);
		this.snapWidth.setText(snapWidth == null ? "W" : "");
		this.snapHeight.setIcon(snapHeight);
		this.snapHeight.setText(snapHeight == null ? "H" : "");
	}

	/**
	 * A smaller {@link ZoomBox} that uses buttons instead of a big combo box
	 * for the zoom modes.
	 * <p>
	 * Always small in vertical orientation.
	 * 
	 * @return TRUE if it is small
	 */
	public boolean getSmall() {
		return small;
	}

	/**
	 * A smaller {@link ZoomBox} that uses buttons instead of a big combo box
	 * for the zoom modes.
	 * <p>
	 * Always small in vertical orientation.
	 * 
	 * @param small
	 *            TRUE to set it small
	 * 
	 * @return TRUE if it changed something
	 */
	public boolean setSmall(boolean small) {
		return setUi(small, vertical);
	}

	/**
	 * The general orientation of the component.
	 * 
	 * @return TRUE for vertical orientation, FALSE for horisontal orientation
	 */
	public boolean getOrientation() {
		return vertical;
	}

	/**
	 * The general orientation of the component.
	 * 
	 * @param vertical
	 *            TRUE for vertical orientation, FALSE for horisontal
	 *            orientation
	 * 
	 * @return TRUE if it changed something
	 */
	public boolean setOrientation(boolean vertical) {
		return setUi(small, vertical);
	}

	/**
	 * Set the zoom level, no fire event.
	 * <p>
	 * It usually returns 1 (default value), the value you passed yourself or 0
	 * (a snap to width or snap to height was asked by the user).
	 * 
	 * @param zoom
	 *            the zoom level
	 */
	private void doSetZoom(double zoom) {
		if (zoom > 0) {
			String zoomStr = Integer.toString((int) Math.round(zoom * 100))
					+ " %";
			zoomLabel.setText(zoomStr);
			if (snapMode == null) {
				zoomBoxModel.setSelectedItem(zoomStr);
			}
		}

		this.zoom = zoom;
	}

	/**
	 * Set the snap mode, no fire event.
	 * 
	 * @param snapToWidth
	 *            the snap mode
	 */
	private void doSetSnapMode(Boolean snapToWidth) {
		if (snapToWidth == null) {
			String zoomStr = Integer.toString((int) Math.round(zoom * 100))
					+ " %";
			if (zoom > 0) {
				zoomBoxModel.setSelectedItem(zoomStr);
			}
		} else {
			for (ZoomLevel level : ZoomLevel.values()) {
				if (level.getSnapToWidth() == snapToWidth) {
					zoomBoxModel.setSelectedItem(level);
				}
			}
		}

		this.snapMode = snapToWidth;
	}

	private boolean setUi(boolean small, boolean vertical) {
		if (getWidth() == 0 || this.small != small
				|| this.vertical != vertical) {
			this.small = small;
			this.vertical = vertical;

			BoxLayout layout = new BoxLayout(this,
					vertical ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS);
			this.removeAll();
			setLayout(layout);

			if (vertical || small) {
				this.add(zoomIn);
				this.add(snapWidth);
				this.add(snapHeight);
				this.add(zoomOut);
				this.add(zoomLabel);
			} else {
				this.add(zoomIn);
				this.add(zoombox);
				this.add(zoomOut);
			}

			this.revalidate();
			this.repaint();

			return true;
		}

		return false;
	}
}
