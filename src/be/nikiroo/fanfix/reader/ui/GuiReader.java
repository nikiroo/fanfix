package be.nikiroo.fanfix.reader.ui;

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
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.Reader;
import be.nikiroo.utils.Progress;
import be.nikiroo.utils.Version;
import be.nikiroo.utils.ui.UIUtils;

/**
 * This class implements a graphical {@link Reader} using the Swing library from
 * Java.
 * <p>
 * It can thus be themed to look native-like, or metal-like, or use any other
 * theme you may want to try.
 * <p>
 * We actually try to enable native look-alike mode on start.
 * 
 * @author niki
 */
class GuiReader extends BasicReader {
	static private boolean nativeLookLoaded;

	private CacheLibrary cacheLib;

	private File cacheDir;

	/**
	 * Create a new graphical {@link Reader}.
	 * 
	 * @throws IOException
	 *             in case of I/O errors
	 */
	public GuiReader() throws IOException {
		// TODO: allow different themes?
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
							Instance.getTraceHandler().error(ee);
						} catch (URISyntaxException ee) {
							Instance.getTraceHandler().error(ee);
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

	@Override
	public void start(File target, String program) throws IOException {
		if (program == null) {
			try {
				Desktop.getDesktop().browse(target.toURI());
			} catch (UnsupportedOperationException e) {
				super.start(target, program);
			}
		} else {
			super.start(target, program);
		}
	}

	/**
	 * Delete the {@link Story} from the cache if it is present, but <b>NOT</b>
	 * from the main library.
	 * <p>
	 * The next time we try to retrieve the {@link Story}, it may be required to
	 * cache it again.
	 * 
	 * @param luid
	 *            the luid of the {@link Story}
	 */
	void clearLocalReaderCache(String luid) {
		try {
			cacheLib.clearFromCache(luid);
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	/**
	 * Forward the delete operation to the main library.
	 * <p>
	 * The {@link Story} will be deleted from the main library as well as the
	 * cache if present.
	 * 
	 * @param luid
	 *            the {@link Story} to delete
	 */
	void delete(String luid) {
		try {
			cacheLib.delete(luid);
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	/**
	 * "Open" the given {@link Story}. It usually involves starting an external
	 * program adapted to the given file type.
	 * 
	 * @param luid
	 *            the luid of the {@link Story} to open
	 * @param pg
	 *            the optional progress (we may need to prepare the
	 *            {@link Story} for reading
	 * 
	 * @throws IOException
	 *             in case of I/O errors
	 */
	void read(String luid, Progress pg) throws IOException {
		File file = cacheLib.getFile(luid, pg);

		// TODO: show a special page for the chapter?
		// We could also implement an internal viewer, both for image and
		// non-image documents
		openExternal(getLibrary().getInfo(luid), file);
	}

	/**
	 * Change the source of the given {@link Story} (the source is the main
	 * information used to group the stories together).
	 * <p>
	 * In other words, <b>move</b> the {@link Story} into other source.
	 * <p>
	 * The source can be a new one, it needs not exist before hand.
	 * 
	 * @param luid
	 *            the luid of the {@link Story} to move
	 * @param newSource
	 *            the new source
	 */
	void changeSource(String luid, String newSource) {
		try {
			cacheLib.changeSource(luid, newSource, null);
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}
}
