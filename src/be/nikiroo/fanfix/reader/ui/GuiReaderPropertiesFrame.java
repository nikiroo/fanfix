package be.nikiroo.fanfix.reader.ui;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.data.Story;
import be.nikiroo.fanfix.library.BasicLibrary;

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
	 * @param ib
	 *            the library to use for the cover image
	 * @param meta
	 *            the meta to describe
	 */
	public GuiReaderPropertiesFrame(BasicLibrary lib, MetaData meta) {
		setTitle(meta.getLuid() + ": " + meta.getTitle());

		GuiReaderPropertiesPane desc = new GuiReaderPropertiesPane(lib, meta);
		setSize(800,
				(int) desc.getPreferredSize().getHeight() + 2
						* desc.getBorderThickness());

		setLayout(new BorderLayout());
		add(desc, BorderLayout.NORTH);
	}
}
