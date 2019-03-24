package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

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
	private JLabel chapterLabel;
	private GuiReaderPropertiesPane descPane;
	private int currentChapter = -42; // cover = -1
	private GuiReaderViewerPanel mainPanel;
	private JButton[] navButtons;

	/**
	 * Create a new {@link Story} viewer.
	 * 
	 * @param lib
	 *            the {@link BasicLibrary} to load the cover from
	 * @param story
	 *            the {@link Story} to display
	 */
	public GuiReaderViewer(BasicLibrary lib, Story story) {
		super(story.getMeta().getLuid() + ": " + story.getMeta().getTitle());

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
		JPanel navButtonsPane = new JPanel();
		LayoutManager layout = new BoxLayout(navButtonsPane, BoxLayout.X_AXIS);
		navButtonsPane.setLayout(layout);

		navButtons = new JButton[4];

		navButtons[0] = createNavButton("<<", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(-1);
			}
		});
		navButtons[1] = createNavButton(" < ", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(currentChapter - 1);
			}
		});
		navButtons[2] = createNavButton(" > ", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(currentChapter + 1);
			}
		});
		navButtons[3] = createNavButton(">>", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(story.getChapters().size() - 1);
			}
		});

		for (JButton navButton : navButtons) {
			navButtonsPane.add(navButton);
		}

		add(navButtonsPane, BorderLayout.SOUTH);

		chapterLabel = new JLabel("");
		navButtonsPane.add(chapterLabel);
	}

	/**
	 * Create a single navigation button.
	 * 
	 * @param text
	 *            the text to display
	 * @param action
	 *            the action to take on click
	 * @return the button
	 */
	private JButton createNavButton(String text, ActionListener action) {
		JButton navButton = new JButton(text);
		navButton.addActionListener(action);
		navButton.setForeground(Color.BLUE);
		return navButton;
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
		navButtons[0].setEnabled(chapter >= 0);
		navButtons[1].setEnabled(chapter >= 0);
		navButtons[2].setEnabled(chapter + 1 < story.getChapters().size());
		navButtons[3].setEnabled(chapter + 1 < story.getChapters().size());

		if (chapter >= -1 && chapter < story.getChapters().size()
				&& chapter != currentChapter) {
			currentChapter = chapter;

			Chapter chap;
			if (chapter == -1) {
				chap = meta.getResume();
				descPane.setVisible(true);
			} else {
				chap = story.getChapters().get(chapter);
				descPane.setVisible(false);
			}

			chapterLabel.setText("<HTML>&nbsp;&nbsp;<B>Chapter "
					+ chap.getNumber() + "</B>: " + chap.getName() + "</HTML>");

			mainPanel.setChapter(chap);
		}
	}
}
