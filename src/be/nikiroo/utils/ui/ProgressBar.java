package be.nikiroo.utils.ui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import be.nikiroo.utils.Progress;

public class ProgressBar extends JPanel {
	private static final long serialVersionUID = 1L;

	private Map<Progress, JProgressBar> bars;
	private List<ActionListener> listeners;
	private Progress pg;

	public ProgressBar() {
		bars = new HashMap<Progress, JProgressBar>();
		listeners = new ArrayList<ActionListener>();
	}

	public void setProgress(final Progress pg) {
		this.pg = pg;

		if (pg == null) {
			setPresent(false);
		} else {
			final JProgressBar bar = new JProgressBar();
			bar.setStringPainted(true);

			bars.clear();
			bars.put(pg, bar);

			bar.setMinimum(pg.getMin());
			bar.setMaximum(pg.getMax());
			bar.setValue(pg.getProgress());
			bar.setString(pg.getName());

			pg.addProgressListener(new Progress.ProgressListener() {
				public void progress(Progress progress, String name) {

					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							Map<Progress, JProgressBar> newBars = new HashMap<Progress, JProgressBar>();
							newBars.put(pg, bar);

							bar.setMinimum(pg.getMin());
							bar.setMaximum(pg.getMax());
							bar.setValue(pg.getProgress());
							bar.setString(pg.getName());

							for (Progress pg : getChildrenAsOrderedList(pg)) {
								JProgressBar bar = bars.get(pg);
								if (bar == null) {
									bar = new JProgressBar();
									bar.setStringPainted(true);
								}

								newBars.put(pg, bar);

								bar.setMinimum(pg.getMin());
								bar.setMaximum(pg.getMax());
								bar.setValue(pg.getProgress());
								bar.setString(pg.getName());
							}

							bars = newBars;

							if (pg.isDone()) {
								for (ActionListener listener : listeners) {
									listener.actionPerformed(new ActionEvent(
											ProgressBar.this, 0, "done"));
								}
							}

							setPresent(true);
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

	private List<Progress> getChildrenAsOrderedList(Progress pg) {
		List<Progress> children = new ArrayList<Progress>();
		for (Progress child : pg.getChildren()) {
			children.add(child);
			children.addAll(getChildrenAsOrderedList(child));
		}

		return children;
	}

	private void setPresent(boolean present) {
		removeAll();

		if (present) {
			setLayout(new GridLayout(bars.size(), 1));
			add(bars.get(pg), 0);
			for (Progress child : getChildrenAsOrderedList(pg)) {
				add(bars.get(child));
			}
		}

		revalidate();
		repaint();
	}
}
