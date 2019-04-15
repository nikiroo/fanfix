package be.nikiroo.fanfix.reader.ui;

import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.VersionCheck;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.bundles.UiConfig;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.CacheLibrary;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.Reader;
import be.nikiroo.fanfix.supported.SupportType;
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
	public void read(boolean sync) throws IOException {
		MetaData meta = getMeta();

		if (meta == null) {
			throw new IOException("No story to read");
		}

		read(meta.getLuid(), sync, null);
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
		final Boolean[] done = new Boolean[] { false };

		// TODO: improve presentation of update message
		final VersionCheck updates = VersionCheck.check();
		StringBuilder builder = new StringBuilder();

		final JEditorPane updateMessage = new JEditorPane("text/html", "");
		if (updates.isNewVersionAvailable()) {
			builder.append(trans(StringIdGui.NEW_VERSION_AVAILABLE,
					"<span style='color: blue;'>https://github.com/nikiroo/fanfix/releases</span>"));
			builder.append("<br>");
			builder.append("<br>");
			for (Version v : updates.getNewer()) {
				builder.append("\t<b>"
						+ trans(StringIdGui.NEW_VERSION_VERSION, v.toString())
						+ "</b>");
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
							updateMessage,
							trans(StringIdGui.NEW_VERSION_TITLE),
							JOptionPane.OK_CANCEL_OPTION);
					if (rep == JOptionPane.OK_OPTION) {
						updates.ok();
					} else {
						updates.ignore();
					}
				}

				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							GuiReaderFrame gui = new GuiReaderFrame(
									GuiReader.this, typeFinal);
							sync(gui);
						} catch (Exception e) {
							Instance.getTraceHandler().error(e);
						} finally {
							done[0] = true;
						}

					}
				}).start();
			}
		});

		// This action must be synchronous, so wait until the frame is closed
		while (!done[0]) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public void start(File target, String program, boolean sync)
			throws IOException {

		boolean handled = false;
		if (program == null && !sync) {
			try {
				Desktop.getDesktop().browse(target.toURI());
				handled = true;
			} catch (UnsupportedOperationException e) {
			}
		}

		if (!handled) {
			super.start(target, program, sync);
		}
	}

	@Override
	public void search(boolean sync) throws IOException {
		// TODO
		if (sync) {
			throw new java.lang.IllegalStateException("Not implemented yet.");
		}
	}

	@Override
	public void search(SupportType searchOn, String keywords, int page,
			int item, boolean sync) {
		// TODO: add parameters!
		GuiReaderSearch search = new GuiReaderSearch(this);
		if (sync) {
			sync(search);
		} else {
			search.setVisible(true);
		}
	}

	@Override
	public void searchTag(SupportType searchOn, int page, int item,
			boolean sync, Integer... tags) {
		// TODO
		if (sync) {
			throw new java.lang.IllegalStateException("Not implemented yet.");
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
	 * <p>
	 * Asynchronous method.
	 * 
	 * @param luid
	 *            the luid of the {@link Story} to open
	 * @param sync
	 *            execute the process synchronously (wait until it is terminated
	 *            before returning)
	 * @param pg
	 *            the optional progress (we may need to prepare the
	 *            {@link Story} for reading
	 * 
	 * @throws IOException
	 *             in case of I/O errors
	 */
	void read(String luid, boolean sync, Progress pg) throws IOException {
		MetaData meta = cacheLib.getInfo(luid);

		boolean textInternal = Instance.getUiConfig().getBoolean(
				UiConfig.NON_IMAGES_DOCUMENT_USE_INTERNAL_READER, true);
		boolean imageInternal = Instance.getUiConfig().getBoolean(
				UiConfig.IMAGES_DOCUMENT_USE_INTERNAL_READER, true);

		boolean useInternalViewer = true;
		if (meta.isImageDocument() && !imageInternal) {
			useInternalViewer = false;
		}
		if (!meta.isImageDocument() && !textInternal) {
			useInternalViewer = false;
		}

		if (useInternalViewer) {
			GuiReaderViewer viewer = new GuiReaderViewer(cacheLib,
					cacheLib.getStory(luid, null));
			if (sync) {
				sync(viewer);
			} else {
				viewer.setVisible(true);
			}
		} else {
			File file = cacheLib.getFile(luid, pg);
			openExternal(meta, file, sync);
		}
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

	/**
	 * Change the title of the given {@link Story}.
	 * 
	 * @param luid
	 *            the luid of the {@link Story} to change
	 * @param newTitle
	 *            the new title
	 */
	void changeTitle(String luid, String newTitle) {
		try {
			cacheLib.changeTitle(luid, newTitle, null);
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	/**
	 * Change the author of the given {@link Story}.
	 * <p>
	 * The author can be a new one, it needs not exist before hand.
	 * 
	 * @param luid
	 *            the luid of the {@link Story} to change
	 * @param newAuthor
	 *            the new author
	 */
	void changeAuthor(String luid, String newAuthor) {
		try {
			cacheLib.changeAuthor(luid, newAuthor, null);
		} catch (IOException e) {
			Instance.getTraceHandler().error(e);
		}
	}

	/**
	 * Simple shortcut method to call {link Instance#getTransGui()#getString()}.
	 * 
	 * @param id
	 *            the ID to translate
	 * 
	 * @return the translated result
	 */
	static String trans(StringIdGui id, Object... params) {
		return Instance.getTransGui().getString(id, params);
	}

	/**
	 * Start a frame and wait until it is closed before returning.
	 * 
	 * @param frame
	 *            the frame to start
	 */
	static private void sync(final JFrame frame) {
		if (EventQueue.isDispatchThread()) {
			throw new IllegalStateException(
					"Cannot call a sync method in the dispatch thread");
		}

		final Boolean[] done = new Boolean[] { false };
		try {
			Runnable run = new Runnable() {
				@Override
				public void run() {
					try {
						frame.addWindowListener(new WindowAdapter() {
							@Override
							public void windowClosing(WindowEvent e) {
								super.windowClosing(e);
								done[0] = true;
							}
						});

						frame.setVisible(true);
					} catch (Exception e) {
						done[0] = true;
					}
				}
			};

			if (EventQueue.isDispatchThread()) {
				run.run();
			} else {
				EventQueue.invokeLater(run);
			}
		} catch (Exception e) {
			Instance.getTraceHandler().error(e);
			done[0] = true;
		}

		// This action must be synchronous, so wait until the frame is closed
		while (!done[0]) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}
}
