package be.nikiroo.fanfix_swing.gui;

import java.awt.Container;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.net.URL;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix_swing.Actions;
import be.nikiroo.utils.Progress;

public class ImporterFrame extends JFrame {
	public ImporterFrame() {

	}

	/**
	 * Ask for and import an {@link URL} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * 
	 * @param parent
	 *            a container we can use to display the {@link URL} chooser and
	 *            to show error messages if any
	 * @param onSuccess
	 *            Action to execute on success
	 */
	public void imprtUrl(final Container parent, final Runnable onSuccess) {
		String clipboard = "";
		try {
			clipboard = ("" + Toolkit.getDefaultToolkit().getSystemClipboard()
					.getData(DataFlavor.stringFlavor)).trim();
		} catch (Exception e) {
			// No data will be handled
		}

		if (clipboard == null || !(clipboard.startsWith("http://") || //
				clipboard.startsWith("https://"))) {
			clipboard = "";
		}

		Object url = JOptionPane.showInputDialog(parent,
				Instance.getInstance().getTransGui()
						.getString(StringIdGui.SUBTITLE_IMPORT_URL),
				Instance.getInstance().getTransGui()
						.getString(StringIdGui.TITLE_IMPORT_URL),
				JOptionPane.QUESTION_MESSAGE, null, null, clipboard);

		Progress pg = null;
		if (url != null && !url.toString().isEmpty()) {
			Actions.imprt(parent, url.toString(), pg, onSuccess);
		}
	}

	/**
	 * Ask for and import a {@link File} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * 
	 * @param parent
	 *            a container we can use to display the {@link File} chooser and
	 *            to show error messages if any
	 * @param onSuccess
	 *            Action to execute on success
	 */

	public void imprtFile(final Container parent, final Runnable onSuccess) {
		JFileChooser fc = new JFileChooser();

		Progress pg = null;
		if (fc.showOpenDialog(parent) != JFileChooser.CANCEL_OPTION) {
			Object url = fc.getSelectedFile().getAbsolutePath();
			if (url != null && !url.toString().isEmpty()) {
				Actions.imprt(parent, url.toString(), pg, onSuccess);
			}
		}
	}
}
