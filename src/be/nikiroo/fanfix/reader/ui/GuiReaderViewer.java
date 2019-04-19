package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import be.nikiroo.fanfix.bundles.StringIdGui;
import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;

/**
 * An internal, Swing-based {@link Story} viewer.
 * <p>
 * Works on both text and image document (see {@link MetaData#isImageDocument()}
 * ).
 * 
 * @author niki
 */
public class GuiReaderViewer extends JFrame {
	private static final long serialVersionUID = 1L;

	private Story story;
	private MetaData meta;
	private JLabel title;
	private GuiReaderPropertiesPane descPane;
	private GuiReaderViewerPanel mainPanel;
	private GuiReaderNavBar navbar;

	/**
	 * Create a new {@link Story} viewer.
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to load the cover from
	 * @param story
	 *            the {@link Story} to display
	 */
	public GuiReaderViewer(BasicLibrary lib, Story story) {
		setTitle(GuiReader.trans(StringIdGui.TITLE_STORY, story.getMeta()
				.getLuid(), story.getMeta().getTitle()));

		setSize(800, 600);

		this.story = story;
		this.meta = story.getMeta();

		initGuiBase(lib);
		initGuiNavButtons();

		setChapter(-1);
	}

	/**
	 * Initialise the base panel with everything but the navigation buttons.
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to use to retrieve the cover image in
	 *            the description panel
	 */
	private void initGuiBase(BasicLibrary lib) {
		setLayout(new BorderLayout());

		title = new JLabel();
		title.setFont(new Font(Font.SERIF, Font.BOLD,
				title.getFont().getSize() * 3));
		title.setText(meta.getTitle());
		title.setHorizontalAlignment(SwingConstants.CENTER);
		title.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(title, BorderLayout.NORTH);

		JPanel contentPane = new JPanel(new BorderLayout());
		add(contentPane, BorderLayout.CENTER);

		descPane = new GuiReaderPropertiesPane(lib, meta);
		contentPane.add(descPane, BorderLayout.NORTH);

		mainPanel = new GuiReaderViewerPanel(story);
		contentPane.add(mainPanel, BorderLayout.CENTER);
	}

	/**
	 * Create the 4 navigation buttons in {@link GuiReaderViewer#navButtons} and
	 * initialise them.
	 */
	private void initGuiNavButtons() {
		navbar = new GuiReaderNavBar(-1, story.getChapters().size() - 1) {
			private static final long serialVersionUID = 1L;

			@Override
			protected String computeLabel(int index, int min, int max) {
				int chapter = index;
				Chapter chap;
				if (chapter < 0) {
					chap = meta.getResume();
					descPane.setVisible(true);
				} else {
					chap = story.getChapters().get(chapter);
					descPane.setVisible(false);
				}

				String chapterDisplay = GuiReader.trans(
						StringIdGui.CHAPTER_HTML_UNNAMED, chap.getNumber(),
						story.getChapters().size());
				if (chap.getName() != null && !chap.getName().trim().isEmpty()) {
					chapterDisplay = GuiReader.trans(
							StringIdGui.CHAPTER_HTML_NAMED, chap.getNumber(),
							story.getChapters().size(), chap.getName());
				}

				return "<HTML>" + chapterDisplay + "</HTML>";
			}
		};

		navbar.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(navbar.getIndex());
			}
		});

		JPanel navButtonsPane = new JPanel();
		LayoutManager layout = new BoxLayout(navButtonsPane, BoxLayout.X_AXIS);
		navButtonsPane.setLayout(layout);

		add(navbar, BorderLayout.SOUTH);
	}

	/**
	 * Set the current chapter, 0-based.
	 * <p>
	 * Chapter -1 is reserved for the description page.
	 * 
	 * @param chapter
	 *            the chapter number to set
	 */
	private void setChapter(int chapter) {
		Chapter chap;
		if (chapter < 0) {
			chap = meta.getResume();
			descPane.setVisible(true);
		} else {
			chap = story.getChapters().get(chapter);
			descPane.setVisible(false);
		}

		mainPanel.setChapter(chap);
	}
}
