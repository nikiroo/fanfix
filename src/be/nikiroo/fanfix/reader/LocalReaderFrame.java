package be.nikiroo.fanfix.reader;

import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.supported.BasicSupport.SupportType;

class LocalReaderFrame extends JFrame {
	private static final long serialVersionUID = 1L;
	private LocalReader reader;

	public LocalReaderFrame(LocalReader reader, SupportType type) {
		super("HTML reader");

		this.reader = reader;

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(800, 600);
		setLayout(new FlowLayout());

		// TODO: list all stories, list all TMP stories (and format?)

		List<MetaData> stories = Instance.getLibrary().getList(type);
		for (MetaData story : stories) {
			JButton button = new JButton(story.getTitle());
			final String luid = story.getLuid();
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						// TODO: config option (image, non image): TXT,
						// custom-HTML, CBZ, EPUB
						Desktop.getDesktop().browse(
								LocalReaderFrame.this.reader.getTarget(luid)
										.toURI());
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});

			add(button);
		}

		setVisible(true);
	}
}
