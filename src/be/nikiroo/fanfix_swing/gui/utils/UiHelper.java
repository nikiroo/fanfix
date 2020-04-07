package be.nikiroo.fanfix_swing.gui.utils;

import java.awt.Color;
import java.awt.Container;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import be.nikiroo.utils.Progress;

public class UiHelper {
	static private Color buttonNormal;
	static private Color buttonPressed;

	static public void setButtonPressed(JButton button, boolean pressed) {
		if (buttonNormal == null) {
			JButton defButton = new JButton(" ");
			buttonNormal = defButton.getBackground();
			if (buttonNormal.getBlue() >= 128) {
				buttonPressed = new Color( //
						Math.max(buttonNormal.getRed() - 100, 0), //
						Math.max(buttonNormal.getGreen() - 100, 0), //
						Math.max(buttonNormal.getBlue() - 100, 0));
			} else {
				buttonPressed = new Color( //
						Math.min(buttonNormal.getRed() + 100, 255), //
						Math.min(buttonNormal.getGreen() + 100, 255), //
						Math.min(buttonNormal.getBlue() + 100, 255));
			}
		}

		button.setSelected(pressed);
		button.setBackground(pressed ? buttonPressed : buttonNormal);
	}

	static public JComponent scroll(JComponent pane) {
		JScrollPane scroll = new JScrollPane(pane);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		return scroll;
	}
}
