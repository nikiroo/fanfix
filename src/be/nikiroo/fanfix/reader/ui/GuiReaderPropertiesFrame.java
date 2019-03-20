package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.Reader;

/**
 * A frame displaying properties and other information of a {@link Story}.
 * 
 * @author niki
 */
public class GuiReaderPropertiesFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@link GuiReaderPropertiesFrame}.
	 * 
	 * @param reader
	 *            the linked reader
	 * @param meta
	 *            the meta to describe
	 */
	public GuiReaderPropertiesFrame(Reader reader, GuiReaderBookInfo info) {
		MetaData meta = info.getMeta();

		// Borders
		int top = 20;
		int space = 10;

		// Image
		ImageIcon img = GuiReaderCoverImager.generateCoverIcon(
				reader.getLibrary(), info);

		// frame
		setTitle(meta.getLuid() + ": " + meta.getTitle());

		setSize(800, img.getIconHeight() + 2 * top);
		setLayout(new BorderLayout());

		// Main panel
		JPanel mainPanel = new JPanel(new BorderLayout());
		JPanel mainPanelKeys = new JPanel();
		mainPanelKeys.setLayout(new BoxLayout(mainPanelKeys, BoxLayout.Y_AXIS));
		JPanel mainPanelValues = new JPanel();
		mainPanelValues.setLayout(new BoxLayout(mainPanelValues,
				BoxLayout.Y_AXIS));

		mainPanel.add(mainPanelKeys, BorderLayout.WEST);
		mainPanel.add(mainPanelValues, BorderLayout.CENTER);

		List<Entry<String, String>> desc = BasicReader.getMetaDesc(meta);

		Color trans = new Color(0, 0, 0, 1);
		for (Entry<String, String> descLine : desc) {
			JTextArea key = new JTextArea(descLine.getKey());
			key.setFont(new Font(key.getFont().getFontName(), Font.BOLD, key
					.getFont().getSize()));
			key.setEditable(false);
			key.setLineWrap(false);
			key.setBackground(trans);
			mainPanelKeys.add(key);

			JTextArea value = new JTextArea(descLine.getValue());
			value.setEditable(false);
			value.setLineWrap(false);
			value.setBackground(trans);
			mainPanelValues.add(value);
		}

		// Image
		JLabel imgLabel = new JLabel(img);
		imgLabel.setVerticalAlignment(JLabel.TOP);

		// Borders
		mainPanelKeys.setBorder(BorderFactory.createEmptyBorder(top, space, 0,
				0));
		mainPanelValues.setBorder(BorderFactory.createEmptyBorder(top, space,
				0, 0));
		imgLabel.setBorder(BorderFactory.createEmptyBorder(0, space, 0, 0));

		// Add all
		add(imgLabel, BorderLayout.WEST);
		add(mainPanel, BorderLayout.CENTER);
	}
}
