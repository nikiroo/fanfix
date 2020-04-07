package be.nikiroo.fanfix_swing.gui.utils;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix.Instance;

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

	/**
	 * Display an error message and log the linked {@link Exception}.
	 * 
	 * @param owner   the owner of the error (to link the messagebox to it)
	 * @param message the message
	 * @param title   the title of the error message
	 * @param e       the exception to log if any
	 */
	static public void error(final Component owner, final String message, final String title, Exception e) {
		Instance.getInstance().getTraceHandler().error(title + ": " + message);
		if (e != null) {
			Instance.getInstance().getTraceHandler().error(e);
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JOptionPane.showMessageDialog(owner, message, title, JOptionPane.ERROR_MESSAGE);
			}
		});
	}
}
