package be.nikiroo.fanfix.reader;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.LocalLibrary;
import be.nikiroo.fanfix.VersionCheck;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.output.BasicOutput.OutputType;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.ui.UIUtils;

class GuiReader extends BasicReader {
	static private boolean nativeLookLoaded;

	private LocalLibrary localLibrary;

	public GuiReader() throws IOException {
		if (!nativeLookLoaded) {
			UIUtils.setLookAndFeel();
			nativeLookLoaded = true;
		}

		File dir = Instance.getReaderDir();
		dir.mkdirs();
		if (!dir.exists()) {
			throw new IOException(
					"Cannote create cache directory for local reader: " + dir);
		}

		OutputType text = null;
		OutputType images = null;

		try {
			text = OutputType.valueOfNullOkUC(Instance.getUiConfig().getString(
					UiConfig.NON_IMAGES_DOCUMENT_TYPE));
			if (text == null) {
				text = OutputType.HTML;
			}

			images = OutputType.valueOfNullOkUC(Instance.getUiConfig()
					.getString(UiConfig.IMAGES_DOCUMENT_TYPE));
			if (images == null) {
				images = OutputType.CBZ;
			}
		} catch (Exception e) {
			UiConfig key = (text == null) ? UiConfig.NON_IMAGES_DOCUMENT_TYPE
					: UiConfig.IMAGES_DOCUMENT_TYPE;
			String value = Instance.getUiConfig().getString(key);

			throw new IOException(
					String.format(
							"The configuration option %s is not valid: %s",
							key, value), e);
		}

		localLibrary = new LocalLibrary(dir, text, images);
	}

	@Override
	public void read() throws IOException {
		if (getStory() == null) {
			throw new IOException("No story to read");
		}

		open(getStory().getMeta().getLuid(), null);
	}

	@Override
	public void read(int chapter) throws IOException {
		// TODO: show a special page?
		read();
	}

	/**
	 * Import the story into the local reader library, and keep the same LUID.
	 * 
	 * @param luid
	 *            the Library UID
	 * @param pg
	 *            the optional progress reporter
	 * 
	 * @throws IOException
	 *             in case of I/O error
	 */
	public void imprt(String luid, Progress pg) throws IOException {
		Progress pgGetStory = new Progress();
		Progress pgSave = new Progress();
		if (pg != null) {
			pg.setMax(2);
			pg.addProgress(pgGetStory, 1);
			pg.addProgress(pgSave, 1);
		}

		try {
			Story story = Instance.getLibrary().getStory(luid, pgGetStory);
			if (story != null) {
				story = localLibrary.save(story, luid, pgSave);
			} else {
				throw new IOException("Cannot find story in Library: " + luid);
			}
		} catch (IOException e) {
			throw new IOException(
					"Cannot import story from library to LocalReader library: "
							+ luid, e);
		}
	}

	/**
	 * Check if the {@link Story} denoted by this Library UID is present in the
	 * {@link GuiReader} cache.
	 * 
	 * @param luid
	 *            the Library UID
	 * 
	 * @return TRUE if it is
	 */
	public boolean isCached(String luid) {
		return localLibrary.getInfo(luid) != null;
	}

	@Override
	public void browse(String type) {
		// TODO: improve presentation of update message
		final VersionCheck updates = VersionCheck.check();
		StringBuilder builder = new StringBuilder();

		final JEditorPane updateMessage = new JEditorPane("text/html", "");
		if (updates.isNewVersionAvailable()) {
			builder.append("A new version of the program is available at <span style='color: blue;'>https://github.com/nikiroo/fanfix/releases</span>");
			builder.append("<br>");
			builder.append("<br>");
			for (Version v : updates.getNewer()) {
				builder.append("\t<b>Version " + v + "</b>");
				builder.append("<br>");
				builder.append("<ul>");
				for (String item : updates.getChanges().get(v)) {
					builder.append("<li>" + item + "</li>");
				}
				builder.append("</ul>");
			}

			// html content
			updateMessage.setText("<html><body>" //
					+ builder//
					+ "</body></html>");

			// handle link events
			updateMessage.addHyperlinkListener(new HyperlinkListener() {
				public void hyperlinkUpdate(HyperlinkEvent e) {
					if (e.getEventType().equals(
							HyperlinkEvent.EventType.ACTIVATED))
						try {
							Desktop.getDesktop().browse(e.getURL().toURI());
						} catch (IOException ee) {
							Instance.syserr(ee);
						} catch (URISyntaxException ee) {
							Instance.syserr(ee);
						}
				}
			});
			updateMessage.setEditable(false);
			updateMessage.setBackground(new JLabel().getBackground());
		}

		final String typeFinal = type;
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (updates.isNewVersionAvailable()) {
					int rep = JOptionPane.showConfirmDialog(null,
							updateMessage, "Updates available",
							JOptionPane.OK_CANCEL_OPTION);
					if (rep == JOptionPane.OK_OPTION) {
						updates.ok();
					} else {
						updates.ignore();
					}
				}

				new GuiReaderFrame(GuiReader.this, typeFinal)
						.setVisible(true);
			}
		});
	}

	// delete from local reader library
	void clearLocalReaderCache(String luid) {
		try {
			localLibrary.delete(luid);
		} catch (IOException e) {
			Instance.syserr(e);
		}
	}

	// delete from main library
	void delete(String luid) {
		try {
			localLibrary.delete(luid);
			Instance.getLibrary().delete(luid);
		} catch (IOException e) {
			Instance.syserr(e);
		}
	}

	// open the given book
	void open(String luid, Progress pg) throws IOException {
		File file = localLibrary.getFile(luid);
		if (file == null) {
			imprt(luid, pg);
			file = localLibrary.getFile(luid);
		}

		open(getLibrary().getInfo(luid), file);
	}

	void changeType(String luid, String newType) {
		try {
			localLibrary.changeSource(luid, newType, null);
			Instance.getLibrary().changeSource(luid, newType, null);
		} catch (IOException e) {
			Instance.syserr(e);
		}
	}
}
