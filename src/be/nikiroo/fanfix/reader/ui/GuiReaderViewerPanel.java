package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;

import be.nikiroo.fanfix.Instance;
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

	private JLabel label;
	private GuiReaderViewerTextOutput htmlOutput;
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
		this.label = new JLabel();
		label.setAlignmentY(TOP_ALIGNMENT);
		htmlOutput = new GuiReaderViewerTextOutput();

		if (!imageDocument) {
			// TODO: why is it broken?
			// text.setPreferredSize(new Dimension(500, 500));

			JScrollPane scroll = new JScrollPane(label);
			scroll.getVerticalScrollBar().setUnitIncrement(16);

			add(scroll, BorderLayout.CENTER);
		} else {
			imageProgress = new JProgressBar();
			imageProgress.setStringPainted(true);
			add(imageProgress, BorderLayout.SOUTH);

			JPanel main = new JPanel(new BorderLayout());
			main.add(label, BorderLayout.CENTER);

			left = new JButton(" < ");
			left.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setImage(--currentImage);
				}
			});
			main.add(left, BorderLayout.WEST);

			right = new JButton(" > ");
			right.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setImage(++currentImage);
				}
			});
			main.add(right, BorderLayout.EAST);

			add(main, BorderLayout.CENTER);
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
			label.setText(htmlOutput.convert(chap));
		} else {
			left.setVisible(chap.getNumber() > 0);
			right.setVisible(chap.getNumber() > 0);
			imageProgress.setVisible(chap.getNumber() > 0);

			imageProgress.setMinimum(0);
			imageProgress.setMaximum(chap.getParagraphs().size() - 1);

			if (chap.getNumber() == 0) {
				label.setText(htmlOutput.convert(chap));
			} else {
				setImage(0);
			}
		}
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
		imageProgress.setString(String.format("Image %d / %d", i + 1, chap
				.getParagraphs().size()));

		currentImage = i;
		label.setText("");
		label.setIcon(null);

		Image img = chap.getParagraphs().get(i).getContentImage();
		if (img == null) {
			label.setText("Error: cannot render image.");
		} else {
			try {
				BufferedImage buffImg = ImageUtilsAwt.fromImage(img);
				Icon icon = new ImageIcon(buffImg);
				label.setIcon(icon);
			} catch (Exception e) {
				Instance.getTraceHandler().error(
						new Exception("Failed to load image into label", e));
				label.setText("Error: cannot load image.");
			}
		}
	}
}
