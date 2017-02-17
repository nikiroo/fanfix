package be.nikiroo.utils;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Some Java Swing utilities.
 * 
 * @author niki
 */
public class UIUtils {
	/**
	 * Set a fake "native look &amp; feel" for the application if possible
	 * (check for the one currently in use, then try GTK).
	 * <p>
	 * <b>Must</b> be called prior to any GUI work.
	 */
	static public void setLookAndFeel() {
		// native look & feel
		try {
			String noLF = "javax.swing.plaf.metal.MetalLookAndFeel";
			String lf = UIManager.getSystemLookAndFeelClassName();
			if (lf.equals(noLF))
				lf = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
			UIManager.setLookAndFeel(lf);
		} catch (InstantiationException e) {
		} catch (ClassNotFoundException e) {
		} catch (UnsupportedLookAndFeelException e) {
		} catch (IllegalAccessException e) {
		}
	}
}
