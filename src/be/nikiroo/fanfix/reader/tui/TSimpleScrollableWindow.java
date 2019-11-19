package be.nikiroo.fanfix.reader.tui;

import jexer.TApplication;
import jexer.THScroller;
import jexer.TPanel;
import jexer.TScrollableWindow;
import jexer.TVScroller;
import jexer.TWidget;

public class TSimpleScrollableWindow extends TScrollableWindow {
	protected TPanel mainPane;
	private int prevHorizontal = -1;
	private int prevVertical = -1;

	public TSimpleScrollableWindow(TApplication application, String title,
			int width, int height) {
		this(application, title, width, height, 0, 0, 0);
	}

	public TSimpleScrollableWindow(TApplication application, String title,
			int width, int height, int flags) {
		this(application, title, width, height, flags, 0, 0);
	}

	// 0 = none (so, no scrollbar)
	public TSimpleScrollableWindow(TApplication application, String title,
			int width, int height, int flags, int realWidth, int realHeight) {
		super(application, title, width, height, flags);

		mainPane = new TPanel(this, 0, 0, width, 80) {
			@Override
			public void draw() {
				for (TWidget children : mainPane.getChildren()) {
					int y = children.getY() + children.getHeight();
					int x = children.getX() + children.getWidth();
					boolean visible = (y > getVerticalValue())
							&& (x > getHorizontalValue());
					children.setVisible(visible);
				}
				super.draw();
			}
		};

		// // TODO: test
		// for (int i = 0; i < 80; i++) {
		// mainPane.addLabel("ligne " + i, i, i);
		// }

		setRealWidth(realWidth);
		setRealHeight(realHeight);
		placeScrollbars();
	}

	/**
	 * The main pane on which you can add other widgets for this scrollable
	 * window.
	 * 
	 * @return the main pane
	 */
	public TPanel getMainPane() {
		return mainPane;
	}

	public void setRealWidth(int realWidth) {
		if (realWidth <= 0) {
			if (hScroller != null) {
				hScroller.remove();
			}
		} else {
			if (hScroller == null) {
				// size/position will be fixed by placeScrollbars()
				hScroller = new THScroller(this, 0, 0, 10);
			}
			hScroller.setRightValue(realWidth);
		}

		reflowData();
	}

	public void setRealHeight(int realHeight) {
		if (realHeight <= 0) {
			if (vScroller != null) {
				vScroller.remove();
			}
		} else {
			if (vScroller == null) {
				// size/position will be fixed by placeScrollbars()
				vScroller = new TVScroller(this, 0, 0, 10);
			}
			vScroller.setBottomValue(realHeight);
		}

		reflowData();
	}

	@Override
	public void reflowData() {
		super.reflowData();
		reflowData(getHorizontalValue(), getVerticalValue());
	}

	protected void reflowData(int totalX, int totalY) {
		super.reflowData();
		mainPane.setX(-totalX);
		mainPane.setY(-totalY);
	}

	@Override
	public void draw() {
		if (prevHorizontal != getHorizontalValue()
				|| prevVertical != getVerticalValue()) {
			prevHorizontal = getHorizontalValue();
			prevVertical = getVerticalValue();
			reflowData();
		}

		super.draw();
	}
}
