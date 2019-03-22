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
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import be.nikiroo.fanfix.data.Chapter;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Paragraph;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;

public class GuiReaderTextViewer extends JFrame {
	private static final long serialVersionUID = 1L;

	private Story story;
	private MetaData meta;
	private JLabel title;
	private JLabel text;
	private JLabel chapterLabel;
	private GuiReaderPropertiesPane descPane;
	private int currentChapter = -42; // cover = -1

	public GuiReaderTextViewer(BasicLibrary lib, Story story) {
		super(story.getMeta().getLuid() + ": " + story.getMeta().getTitle());

		setSize(800, 600);

		this.story = story;
		this.meta = story.getMeta();

		initGuiBase(lib);
		initGuiNavButtons();

		setChapter(-1);
	}

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

		text = new JLabel();
		text.setAlignmentY(TOP_ALIGNMENT);

		// TODO: why is it broken?
		// text.setPreferredSize(new Dimension(500, 500));

		JScrollPane scroll = new JScrollPane(text);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		contentPane.add(scroll, BorderLayout.CENTER);
	}

	private void initGuiNavButtons() {
		JPanel navButtons = new JPanel();
		LayoutManager layout = new BoxLayout(navButtons, BoxLayout.X_AXIS);
		navButtons.setLayout(layout);

		navButtons.add(createNavButton("<<", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(-1);
			}
		}));
		navButtons.add(createNavButton(" < ", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(currentChapter - 1);
			}
		}));
		navButtons.add(createNavButton(" > ", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(currentChapter + 1);
			}
		}));
		navButtons.add(createNavButton(">>", new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setChapter(story.getChapters().size() - 1);
			}
		}));

		add(navButtons, BorderLayout.SOUTH);

		chapterLabel = new JLabel("");
		navButtons.add(chapterLabel);
	}

	private JButton createNavButton(String text, ActionListener action) {
		JButton navButton = new JButton(text);
		navButton.addActionListener(action);
		navButton.setForeground(Color.BLUE);
		return navButton;
	}

	private void setChapter(int chapter) {
		if (chapter >= -1 && chapter < story.getChapters().size()
				&& chapter != currentChapter) {
			currentChapter = chapter;

			Chapter chap;
			if (chapter == -1) {
				chap = meta.getResume();
				setCoverPage();
			} else {
				chap = story.getChapters().get(chapter);
				descPane.setVisible(false);
			}

			chapterLabel.setText("<HTML>&nbsp;&nbsp;<B>Chapter "
					+ chap.getNumber() + "</B>: " + chap.getName() + "</HTML>");

			StringBuilder builder = new StringBuilder();
			addChapter(builder, chap);
			text.setText(builder.toString());
		}
	}

	private void setCoverPage() {
		descPane.setVisible(true);
	}

	// htmlInsert = no need to add HTML tags
	private void addChapter(StringBuilder builder, Chapter chap) {
		builder.append("<HTML>");

		builder.append("<H1>");
		builder.append("Chapter ");
		builder.append(chap.getNumber());
		builder.append(": ");
		builder.append(chap.getName());
		builder.append("</H1>");

		builder.append("<JUSTIFY>");
		for (Paragraph para : chap) {
			addPara(builder, para);
		}

		if (paraInQuote) {
			builder.append("</DIV>");
		}
		paraInQuote = false;

		builder.append("</JUSTIFY>");

		builder.append("</HTML>");
	}

	private boolean paraInQuote;

	private void addPara(StringBuilder builder, Paragraph para) {
		switch (para.getType()) {
		case NORMAL:
			builder.append(para.getContent());
			builder.append("<BR>");
			break;
		case BLANK:
			builder.append("<BR>");
			break;
		case BREAK:
			builder.append("<BR>* * *<BR><BR>");
			break;
		case QUOTE:
			if (!paraInQuote) {
				builder.append("<DIV>");
			} else {
				builder.append("</DIV>");
			}
			paraInQuote = !paraInQuote;

			builder.append("<DIV>");
			builder.append("&ndash;&nbsp;&nbsp;");
			builder.append(para.getContent());
			builder.append("</DIV>");

			break;
		case IMAGE:
		}
	}
}
