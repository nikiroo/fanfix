package be.nikiroo.fanfix_swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Window;
import java.io.File;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;

public class Actions {
	static public void openExternal(final BasicLibrary lib, MetaData meta, final Container parent,
			final Runnable onDone) {
		Container parentWindow = parent;
		while (!(parentWindow instanceof Window) && parentWindow != null) {
			parentWindow = parentWindow.getParent();
		}

		// TODO: UI
		final JDialog wait = new JDialog((Window) parentWindow);
		wait.setTitle("Opening story");
		wait.setSize(400, 300);
		wait.setLayout(new BorderLayout());
		wait.add(new JLabel("Waiting..."));

		// TODO: pg?

		final Object waitLock = new Object();
		final Boolean[] waitScreen = new Boolean[] { false };
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}

				synchronized (waitLock) {
					if (!waitScreen[0]) {
						waitScreen[0] = true;
						wait.setVisible(true);
					}
				}
			}
		}).start();

		final String luid = meta.getLuid();
		final boolean isImageDocument = meta.isImageDocument();

		final SwingWorker<File, Void> worker = new SwingWorker<File, Void>() {
			private File target;

			@Override
			protected File doInBackground() throws Exception {
				target = lib.getFile(luid, null);
				return null;
			}

			@Override
			protected void done() {
				try {
					get();
					openExternal(target, isImageDocument);
				} catch (Exception e) {
					// TODO: i18n
					UiHelper.error(parent, e.getLocalizedMessage(), "Cannot open the story", e);
				}

				synchronized (waitLock) {
					if (waitScreen[0]) {
						wait.setVisible(false);
					}
					waitScreen[0] = true;
				}

				if (onDone != null) {
					onDone.run();
				}
			}
		};

		worker.execute();
	}

	/**
	 * Open the {@link Story} with an external reader (the program will be passed
	 * the given target file).
	 * 
	 * @param target          the target {@link File}
	 * @param isImageDocument TRUE for image documents, FALSE for not-images
	 *                        documents
	 * 
	 * @throws IOException in case of I/O error
	 */
	static public void openExternal(File target, boolean isImageDocument) throws IOException {
		String program = null;
		if (isImageDocument) {
			program = Instance.getInstance().getUiConfig().getString(UiConfig.IMAGES_DOCUMENT_READER);
		} else {
			program = Instance.getInstance().getUiConfig().getString(UiConfig.NON_IMAGES_DOCUMENT_READER);
		}

		if (program != null && program.trim().isEmpty()) {
			program = null;
		}

		start(target, program, false);
	}

	/**
	 * Start a file and open it with the given program if given or the first default
	 * system starter we can find.
	 * 
	 * @param target  the target to open
	 * @param program the program to use or NULL for the default system starter
	 * @param sync    execute the process synchronously (wait until it is terminated
	 *                before returning)
	 * 
	 * @throws IOException in case of I/O error
	 */
	static protected void start(File target, String program, boolean sync) throws IOException {
		Process proc = null;
		if (program == null) {
			boolean ok = false;
			for (String starter : new String[] { "xdg-open", "open", "see", "start", "run" }) {
				try {
					Instance.getInstance().getTraceHandler().trace("starting external program");
					proc = Runtime.getRuntime().exec(new String[] { starter, target.getAbsolutePath() });
					ok = true;
					break;
				} catch (IOException e) {
				}
			}
			if (!ok) {
				throw new IOException("Cannot find a program to start the file");
			}
		} else {
			Instance.getInstance().getTraceHandler().trace("starting external program");
			proc = Runtime.getRuntime().exec(new String[] { program, target.getAbsolutePath() });
		}

		if (proc != null && sync) {
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
			}
		}
	}
}
