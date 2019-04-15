package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import be.nikiroo.fanfix.library.BasicLibrary;

public class GuiReaderSearchAction extends JFrame {
	private static final long serialVersionUID = 1L;

	private GuiReaderBookInfo info;

	public GuiReaderSearchAction(BasicLibrary lib, GuiReaderBookInfo info) {
		// TODO i18n
		super("TODO: " + info.getMainInfo());
		this.setSize(800, 600);
		this.info = info;

		setLayout(new BorderLayout());

		JPanel props = new GuiReaderPropertiesPane(lib, info.getMeta());

		add(props, BorderLayout.NORTH);
		add(new GuiReaderViewerPanel(info.getMeta(), info.getMeta()
				.isImageDocument()), BorderLayout.CENTER);
		add(new JButton("TODO: Download"), BorderLayout.SOUTH);
		// TODO --^
	}
}
