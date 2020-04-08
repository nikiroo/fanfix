
package be.nikiroo.fanfix_swing.gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import be.nikiroo.fanfix_swing.gui.utils.ListenerPanel;
import be.nikiroo.fanfix_swing.gui.utils.UiHelper;
import be.nikiroo.fanfix_swing.images.IconGenerator;
import be.nikiroo.fanfix_swing.images.IconGenerator.Icon;
import be.nikiroo.fanfix_swing.images.IconGenerator.Size;

/**
 * A generic search/filter bar.
 * 
 * @author niki
 */
public class SearchBar extends ListenerPanel {
	static private final long serialVersionUID = 1L;

	private JButton search;
	private JTextField text;
	private JButton clear;

	private boolean realTime;

	/**
	 * Create a new {@link SearchBar}.
	 */
	public SearchBar() {
		setLayout(new BorderLayout());

		search = new JButton(IconGenerator.get(Icon.search, Size.x16));
		UiHelper.setButtonPressed(search, realTime);
		search.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				realTime = !realTime;
				UiHelper.setButtonPressed(search, realTime);
				text.requestFocus();

				if (realTime) {
					fireActionPerformed(getText());
				}
			}
		});

		text = new JTextField();
		text.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(final KeyEvent e) {
				super.keyTyped(e);
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						boolean empty = (text.getText().isEmpty());
						clear.setVisible(!empty);

						if (realTime) {
							fireActionPerformed(getText());
						}
					}
				});
			}
		});
		text.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!realTime) {
					fireActionPerformed(getText());
				}
			}
		});

		clear = new JButton(IconGenerator.get(Icon.clear, Size.x16));
		clear.setBackground(text.getBackground());
		clear.setVisible(false);
		clear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				text.setText("");
				clear.setVisible(false);
				text.requestFocus();

				fireActionPerformed(getText());
			}
		});

		add(search, BorderLayout.WEST);
		add(text, BorderLayout.CENTER);
		add(clear, BorderLayout.EAST);
	}

	/**
	 * Return the current text displayed by this {@link SearchBar}.
	 * 
	 * @return the text
	 */
	public String getText() {
		return text.getText();
	}
}
