package be.nikiroo.utils.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import be.nikiroo.utils.Progress;

public class ProgressBar extends JPanel {
	private static final long serialVersionUID = 1L;

	private JProgressBar bar;
	private JLabel label;
	private List<ActionListener> listeners;

	public ProgressBar() {
		bar = new JProgressBar();
		label = new JLabel();
		listeners = new ArrayList<ActionListener>();

		setLayout(new BorderLayout());
	}

	public void setProgress(Progress pg) {
		if (pg == null) {
			setPresent(false);
		} else {
			label.setText(pg.getName());
			bar.setMinimum(pg.getMin());
			bar.setMaximum(pg.getMax());
			bar.setValue(pg.getProgress());

			pg.addProgressListener(new Progress.ProgressListener() {
				public void progress(final Progress progress, final String name) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							label.setText(name);
							bar.setValue(progress.getProgress());

							if (progress.isDone()) {
								for (ActionListener listener : listeners) {
									listener.actionPerformed(new ActionEvent(
											ProgressBar.this, 0, "done"));
								}
							}
						}
					});
				}
			});

			setPresent(true);
		}
	}

	public void addActioListener(ActionListener l) {
		listeners.add(l);
	}

	public void clearActionListeners() {
		listeners.clear();
	}

	private void setPresent(boolean present) {
		removeAll();

		if (present) {
			add(label, BorderLayout.NORTH);
			add(bar, BorderLayout.CENTER);
		}

		validate();
		repaint();
	}
}
