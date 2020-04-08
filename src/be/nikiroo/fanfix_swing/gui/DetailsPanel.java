package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.util.concurrent.ExecutionException;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix_swing.gui.book.BookBlock;
import be.nikiroo.fanfix_swing.gui.book.BookInfo;

/**
 * Display detailed informations about a {@link BookInfo}.
 * <p>
 * Actually, just its name, the number of stories it contains and a small image
 * if possible.
 * 
 * @author niki
 */
public class DetailsPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private JLabel icon;
	private JLabel name;
	private JLabel opt;

	private BookInfo info;

	/**
	 * Create a new {@link DetailsPanel}.
	 */
	public DetailsPanel() {
		this.setLayout(new BorderLayout());

		this.setPreferredSize(new Dimension(300, 300));
		this.setMinimumSize(new Dimension(200, 200));

		icon = config(new JLabel(), Color.black);
		name = config(new JLabel(), Color.black);
		opt = config(new JLabel(), Color.gray);

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(name, BorderLayout.NORTH);
		panel.add(opt, BorderLayout.SOUTH);
		panel.setBorder(new EmptyBorder(0, 0, 10, 0));

		this.add(icon, BorderLayout.CENTER);
		this.add(panel, BorderLayout.SOUTH);

		setBook(null);
	}

	/**
	 * Configure a {@link JLabel} with the given colour.
	 * 
	 * @param label the label to configure
	 * @param color the colour to use
	 * 
	 * @return the (same) configured label
	 */
	private JLabel config(JLabel label, Color color) {
		label.setAlignmentX(CENTER_ALIGNMENT);
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setHorizontalTextPosition(JLabel.CENTER);
		label.setForeground(color);
		return label;
	}

	/**
	 * Set the {@link BookInfo} you want to see displayed here.
	 * 
	 * @param info the {@link BookInfo} to display
	 */
	public void setBook(final BookInfo info) {
		this.info = info;

		icon.setIcon(null);
		if (info == null) {
			name.setText(null);
			opt.setText(null);
		} else {
			final String myId = info.getId();

			name.setText(info.getMainInfo());
			opt.setText(info.getSecondaryInfo(true));
			new SwingWorker<Image, Void>() {
				@Override
				protected Image doInBackground() throws Exception {
					Thread.sleep(20);

					BookInfo current = DetailsPanel.this.info;
					if (current != null && current.getId().equals(myId)) {
						return BookBlock.generateCoverImage(Instance.getInstance().getLibrary(), info);
					}

					return null;
				}

				@Override
				protected void done() {
					BookInfo current = DetailsPanel.this.info;
					if (current != null && current.getId().equals(myId)) {
						try {
							Image img = get();
							if (img != null)
								icon.setIcon(new ImageIcon(img));
						} catch (InterruptedException e) {
						} catch (ExecutionException e) {
						}
					}
				}
			}.execute();
		}
	}
}
