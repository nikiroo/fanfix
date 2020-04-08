package be.nikiroo.fanfix_swing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.utils.Progress;

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

	/**
	 * Import a {@link Story} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * 
	 * @param askUrl TRUE for an {@link URL}, false for a {@link File}
	 */
	static public void imprt(final Container parent, boolean askUrl, final Runnable onSuccess) {
		JFileChooser fc = new JFileChooser();

		Object url;
		if (askUrl) {
			String clipboard = "";
			try {
				clipboard = ("" + Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor))
						.trim();
			} catch (Exception e) {
				// No data will be handled
			}

			if (clipboard == null || !(clipboard.startsWith("http://") || //
					clipboard.startsWith("https://"))) {
				clipboard = "";
			}

			url = JOptionPane.showInputDialog(parent,
					Instance.getInstance().getTransGui().getString(StringIdGui.SUBTITLE_IMPORT_URL),
					Instance.getInstance().getTransGui().getString(StringIdGui.TITLE_IMPORT_URL),
					JOptionPane.QUESTION_MESSAGE, null, null, clipboard);
		} else if (fc.showOpenDialog(parent) != JFileChooser.CANCEL_OPTION) {
			url = fc.getSelectedFile().getAbsolutePath();
		} else {
			url = null;
		}

		if (url != null && !url.toString().isEmpty()) {
			imprt(parent, url.toString(), null, null);
		}
	}

	/**
	 * Actually import the {@link Story} into the main {@link LocalLibrary}.
	 * <p>
	 * Should be called inside the UI thread.
	 * 
	 * @param url             the {@link Story} to import by {@link URL}
	 * @param onSuccess       Action to execute on success
	 * @param onSuccessPgName the name to use for the onSuccess progress bar
	 */
	static public void imprt(final Container parent, final String url, final Runnable onSuccess,
			String onSuccessPgName) {
		final Progress pg = new Progress();
		final Progress pgImprt = new Progress();
		final Progress pgOnSuccess = new Progress(onSuccessPgName);
		pg.addProgress(pgImprt, 95);
		pg.addProgress(pgOnSuccess, 5);

		new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				Exception ex = null;
				MetaData meta = null;
				try {
					meta = Instance.getInstance().getLibrary().imprt(BasicReader.getUrl(url), pgImprt);
				} catch (IOException e) {
					e.printStackTrace();
					ex = e;
				}

				final Exception e = ex;

				final boolean ok = (e == null);

				pgOnSuccess.setProgress(0);
				if (!ok) {
					if (e instanceof UnknownHostException) {
						UiHelper.error(parent,
								Instance.getInstance().getTransGui().getString(StringIdGui.ERROR_URL_NOT_SUPPORTED,
										url),
								Instance.getInstance().getTransGui().getString(StringIdGui.TITLE_ERROR), null);
					} else {
						UiHelper.error(parent,
								Instance.getInstance().getTransGui().getString(StringIdGui.ERROR_URL_IMPORT_FAILED, url,
										e.getMessage()),
								Instance.getInstance().getTransGui().getString(StringIdGui.TITLE_ERROR), e);
					}
				} else {
					if (onSuccess != null) {
						onSuccess.run();
					}
				}
				pgOnSuccess.done();
				
				return null;
			}
		}.execute();
	}
}
