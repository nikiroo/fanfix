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
import be.nikiroo.fanfix.VersionCheck;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.CacheLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.ui.UIUtils;

class GuiReader extends BasicReader {
	static private boolean nativeLookLoaded;

	private CacheLibrary cacheLib;

	private File cacheDir;

	public GuiReader() throws IOException {
		if (!nativeLookLoaded) {
			UIUtils.setLookAndFeel();
			nativeLookLoaded = true;
		}

		cacheDir = Instance.getReaderDir();
		cacheDir.mkdirs();
		if (!cacheDir.exists()) {
			throw new IOException(
					"Cannote create cache directory for local reader: "
							+ cacheDir);
		}
	}

	@Override
	public synchronized BasicLibrary getLibrary() {
		if (cacheLib == null) {
			BasicLibrary lib = super.getLibrary();
			if (lib instanceof CacheLibrary) {
				cacheLib = (CacheLibrary) lib;
			} else {
				cacheLib = new CacheLibrary(cacheDir, lib);
			}
		}

		return cacheLib;
	}

	@Override
	public void read() throws IOException {
		MetaData meta = getMeta();

		if (meta == null) {
			throw new IOException("No story to read");
		}

		read(meta.getLuid(), null);
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
		return cacheLib.isCached(luid);
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
				@Override
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
			@Override
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

				new GuiReaderFrame(GuiReader.this, typeFinal).setVisible(true);
			}
		});
	}

	// delete from local reader library
	void clearLocalReaderCache(String luid) {
		try {
			cacheLib.clearFromCache(luid);
		} catch (IOException e) {
			Instance.syserr(e);
		}
	}

	// delete from main library
	void delete(String luid) {
		try {
			cacheLib.delete(luid);
		} catch (IOException e) {
			Instance.syserr(e);
		}
	}

	// open the given book
	void read(String luid, Progress pg) throws IOException {
		File file = cacheLib.getFile(luid, pg);

		// TODO: show a special page for the chapter?
		openExternal(getLibrary().getInfo(luid), file);
	}

	void changeType(String luid, String newSource) {
		try {
			cacheLib.changeSource(luid, newSource, null);
		} catch (IOException e) {
			Instance.syserr(e);
		}
	}
}
