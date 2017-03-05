package be.nikiroo.utils.test;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

import be.nikiroo.utils.Progress;
import be.nikiroo.utils.ui.ProgressBar;

public class ProgressBarManualTest extends JFrame {
	private static final long serialVersionUID = 1L;
	private int i = 0;

	public ProgressBarManualTest() {
		final ProgressBar bar = new ProgressBar();
		final Progress pg = new Progress("name");
		final Progress pg2 = new Progress("second level");

		setLayout(new BorderLayout());
		this.add(bar, BorderLayout.SOUTH);

		final JButton b = new JButton("Set pg to 10%");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				switch (i) {
				case 0:
					pg.setProgress(10);
					b.setText("Set pg to 20%");
					break;
				case 1:
					pg.setProgress(20);
					b.setText("Add second pg");
					break;
				case 2:
					pg.addProgress(pg2, 80);
					pg2.setProgress(0);
					b.setText("set second pg to 100%");
					break;
				case 3:
					pg2.setProgress(100);
					b.setText("");
					break;
				}

				i++;
			}
		});
		this.add(b, BorderLayout.CENTER);

		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		bar.setProgress(pg);
	}

	public static void main(String[] args) {
		new ProgressBarManualTest().setVisible(true);
	}
}
