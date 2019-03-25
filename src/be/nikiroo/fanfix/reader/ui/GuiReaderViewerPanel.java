package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.ui.ImageUtilsAwt;

/**
 * A {@link JPanel} that will show a {@link Story} chapter on screen.
 * 
 * @author niki
 */
public class GuiReaderViewerPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private boolean imageDocument;
	private Chapter chap;
	private JScrollPane scroll;
	private GuiReaderViewerTextOutput htmlOutput;

	// text only:
	private JEditorPane text;

	// image only:
	private JLabel image;
	private JProgressBar imageProgress;
	private int currentImage;
	private JButton left;
	private JButton right;

	/**
	 * Create a new viewer.
	 * 
	 * @param story
	 *            the {@link Story} to work on.
	 */
	public GuiReaderViewerPanel(Story story) {
		super(new BorderLayout());

		this.imageDocument = story.getMeta().isImageDocument();

		this.text = new JEditorPane("text/html", "");
		text.setAlignmentY(TOP_ALIGNMENT);
		htmlOutput = new GuiReaderViewerTextOutput();

		image = new JLabel();
		image.setHorizontalAlignment(SwingConstants.CENTER);

		scroll = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.getVerticalScrollBar().setUnitIncrement(16);

		if (!imageDocument) {
			add(scroll, BorderLayout.CENTER);
		} else {
			imageProgress = new JProgressBar();
			imageProgress.setStringPainted(true);
			add(imageProgress, BorderLayout.SOUTH);

			JPanel main = new JPanel(new BorderLayout());
			main.add(scroll, BorderLayout.CENTER);

			left = new JButton("<HTML>&nbsp; &nbsp; &lt; &nbsp; &nbsp;</HTML>");
			left.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setImage(--currentImage);
				}
			});
			main.add(left, BorderLayout.WEST);

			right = new JButton("<HTML>&nbsp; &nbsp; &gt; &nbsp; &nbsp;</HTML>");
			right.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setImage(++currentImage);
				}
			});
			main.add(right, BorderLayout.EAST);

			add(main, BorderLayout.CENTER);
			main.invalidate();
		}

		setChapter(story.getMeta().getResume());
	}

	/**
	 * Load the given chapter.
	 * <p>
	 * Will always be text for a non-image document.
	 * <p>
	 * Will be an image and left/right controls for an image-document, except
	 * for chapter 0 which will be text (chapter 0 = resume).
	 * 
	 * @param chap
	 *            the chapter to load
	 */
	public void setChapter(Chapter chap) {
		this.chap = chap;

		if (!imageDocument) {
			setText(chap);
		} else {
			left.setVisible(chap.getNumber() > 0);
			right.setVisible(chap.getNumber() > 0);
			imageProgress.setVisible(chap.getNumber() > 0);

			imageProgress.setMinimum(0);
			imageProgress.setMaximum(chap.getParagraphs().size() - 1);

			if (chap.getNumber() == 0) {
				setText(chap);
			} else {
				setImage(0);
			}
		}
	}

	/**
	 * Will set and display the current chapter text.
	 * 
	 * @param chap
	 *            the chapter to display
	 */
	private void setText(final Chapter chap) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				final String content = htmlOutput.convert(chap);
				// Wait until size computations are correct
				while (!scroll.isValid()) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
					}
				}

				setText(content);
			}
		}).start();
	}

	/**
	 * Actually set the text in the UI.
	 * <p>
	 * Do <b>NOT</b> use this method from the UI thread.
	 * 
	 * @param content
	 *            the text
	 */
	private void setText(final String content) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				text.setText(content);
				text.setCaretPosition(0);
				scroll.setViewportView(text);
			}
		});
	}

	/**
	 * Will set and display the current image, take care about the progression
	 * and update the left and right cursors' <tt>enabled</tt> property.
	 * 
	 * @param i
	 *            the image index to load
	 */
	private void setImage(int i) {
		left.setEnabled(i > 0);
		right.setEnabled(i + 1 < chap.getParagraphs().size());

		if (i < 0 || i >= chap.getParagraphs().size()) {
			return;
		}

		imageProgress.setValue(i);
		imageProgress.setString(GuiReader.trans(StringIdGui.IMAGE_PROGRESSION,
				i + 1, chap.getParagraphs().size()));

		currentImage = i;

		final Image img = chap.getParagraphs().get(i).getContentImage();

		// prepare the viewport to get the right sizes later on
		image.setIcon(null);
		scroll.setViewportView(image);

		new Thread(new Runnable() {
			@Override
			public void run() {
				// Wait until size computations are correct
				while (!scroll.isValid()) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
					}
				}

				if (img == null) {
					setText("Error: cannot render image.");
				} else {
					setImage(img);
				}
			}
		}).start();
	}

	/**
	 * Actually set the image in the UI.
	 * <p>
	 * Do <b>NOT</b> use this method from the UI thread.
	 * 
	 * @param img
	 *            the image to set
	 */
	private void setImage(Image img) {
		try {
			int scrollWidth = scroll.getWidth()
					- scroll.getVerticalScrollBar().getWidth();

			BufferedImage buffImg = ImageUtilsAwt.fromImage(img);

			int iw = buffImg.getWidth();
			int ih = buffImg.getHeight();
			double ratio = ((double) ih) / iw;

			int w = scrollWidth;
			int h = (int) (ratio * scrollWidth);

			BufferedImage resizedImage = new BufferedImage(w, h,
					BufferedImage.TYPE_4BYTE_ABGR);

			Graphics2D g = resizedImage.createGraphics();
			try {
				g.drawImage(buffImg, 0, 0, w, h, null);
			} finally {
				g.dispose();
			}

			final Icon icon = new ImageIcon(resizedImage);
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					image.setIcon(icon);
					scroll.setViewportView(image);
				}
			});
		} catch (Exception e) {
			Instance.getTraceHandler().error(
					new Exception("Failed to load image into label", e));
			EventQueue.invokeLater(new Runnable() {
				@Override
				public void run() {
					text.setText("Error: cannot load image.");
				}
			});
		}
	}
}
