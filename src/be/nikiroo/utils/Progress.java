package be.nikiroo.utils;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Progress reporting system, possibly nested.
 * 
 * @author niki
 */
public class Progress {
	public interface ProgressListener extends EventListener {
		/**
		 * A progression event.
		 * 
		 * @param progress
		 *            the {@link Progress} object that generated it, not
		 *            necessarily the same as the one where the listener was
		 *            attached (it could be a child {@link Progress} of this
		 *            {@link Progress}).
		 * @param name
		 *            the first non-null name of the {@link Progress} step that
		 *            generated this event
		 */
		public void progress(Progress progress, String name);
	}

	private Progress parent = null;
	private Object lock = new Object();
	private String name;
	private Map<Progress, Double> children;
	private List<ProgressListener> listeners;
	private int min;
	private int max;
	private int localProgress;
	private int progress; // children included

	/**
	 * Create a new default unnamed {@link Progress}, from 0 to 100.
	 */
	public Progress() {
		this(null);
	}

	/**
	 * Create a new default {@link Progress}, from 0 to 100.
	 * 
	 * @param name
	 *            the name of this {@link Progress} step
	 */
	public Progress(String name) {
		this(name, 0, 100);
	}

	/**
	 * Create a new unnamed {@link Progress}, from min to max.
	 * 
	 * @param min
	 *            the minimum progress value (and starting value) -- must be
	 *            non-negative
	 * @param max
	 *            the maximum progress value
	 */
	public Progress(int min, int max) {
		this(null, min, max);
	}

	/**
	 * Create a new {@link Progress}, from min to max.
	 * 
	 * @param name
	 *            the name of this {@link Progress} step
	 * @param min
	 *            the minimum progress value (and starting value) -- must be
	 *            non-negative
	 * @param max
	 *            the maximum progress value
	 */
	public Progress(String name, int min, int max) {
		this.name = name;
		this.children = new HashMap<Progress, Double>();
		this.listeners = new ArrayList<Progress.ProgressListener>();
		setMinMax(min, max);
		setProgress(min);
	}

	/**
	 * The name of this {@link Progress} step.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * The name of this {@link Progress} step.
	 * 
	 * @param name
	 *            the new name
	 */
	public void setName(String name) {
		this.name = name;
		changed(this);
	}

	/**
	 * The minimum progress value.
	 * 
	 * @return the min
	 */
	public int getMin() {
		return min;
	}

	/**
	 * The minimum progress value.
	 * 
	 * @param min
	 *            the min to set
	 * 
	 * 
	 * @throws Error
	 *             if min &lt; 0 or if min &gt; max
	 */
	public void setMin(int min) {
		if (min < 0) {
			throw new Error("negative values not supported");
		}

		synchronized (getLock()) {
			if (min > max) {
				throw new Error(
						"The minimum progress value must be <= the maximum progress value");
			}

			this.min = min;
		}
	}

	/**
	 * The maximum progress value.
	 * 
	 * @return the max
	 */
	public int getMax() {
		return max;
	}

	/**
	 * The maximum progress value (must be >= the minimum progress value).
	 * 
	 * @param max
	 *            the max to set
	 * 
	 * 
	 * @throws Error
	 *             if max &lt; min
	 */
	public void setMax(int max) {
		synchronized (getLock()) {
			if (max < min) {
				throw new Error(
						"The maximum progress value must be >= the minimum progress value");
			}

			this.max = max;
		}
	}

	/**
	 * Set both the minimum and maximum progress values.
	 * 
	 * @param min
	 *            the min
	 * @param max
	 *            the max
	 * 
	 * @throws Error
	 *             if min &lt; 0 or if min &gt; max
	 */
	public void setMinMax(int min, int max) {
		if (min < 0) {
			throw new Error("negative values not supported");
		}

		if (min > max) {
			throw new Error(
					"The minimum progress value must be <= the maximum progress value");
		}

		synchronized (getLock()) {
			this.min = min;
			this.max = max;
		}
	}

	/**
	 * Get the total progress value (including the optional children
	 * {@link Progress}) on a {@link Progress#getMin()} to
	 * {@link Progress#getMax()} scale.
	 * 
	 * @return the progress the value
	 */
	public int getProgress() {
		return progress;
	}

	/**
	 * Set the local progress value (not including the optional children
	 * {@link Progress}), on a {@link Progress#getMin()} to
	 * {@link Progress#getMax()} scale.
	 * 
	 * @param progress
	 *            the progress to set
	 */
	public void setProgress(int progress) {
		synchronized (getLock()) {
			int diff = this.progress - this.localProgress;
			this.localProgress = progress;
			setTotalProgress(this, name, progress + diff);
		}
	}

	/**
	 * Add some value to the current progression of this {@link Progress}.
	 * 
	 * @param step
	 *            the amount to add
	 */
	public void add(int step) {
		synchronized (getLock()) {
			setProgress(localProgress + step);
		}
	}

	/**
	 * Check if the action corresponding to this {@link Progress} is done (i.e.,
	 * if its progress value == its max value).
	 * 
	 * @return TRUE if it is
	 */
	public boolean isDone() {
		return progress >= max;
	}

	/**
	 * Mark the {@link Progress} as done by setting its value to max.
	 */
	public void done() {
		setProgress(getMax());
	}

	/**
	 * Get the total progress value (including the optional children
	 * {@link Progress}) on a 0.0 to 1.0 scale.
	 * 
	 * @return the progress
	 */
	public double getRelativeProgress() {
		if (max == min) {
			return 1;
		}

		return (((double) progress) / (max - min));
	}

	/**
	 * Return the list of direct children of this {@link Progress}.
	 * 
	 * @return the children (Who will think of the children??)
	 */
	public Set<Progress> getChildren() {
		return children.keySet();
	}

	/**
	 * Set the total progress value (including the optional children
	 * {@link Progress}), on a {@link Progress#getMin()} to
	 * {@link Progress#getMax()} scale.
	 * 
	 * @param pg
	 *            the {@link Progress} to report as the progression emitter
	 * @param name
	 *            the current name (if it is NULL, the first non-null name in
	 *            the hierarchy will overwrite it) of the {@link Progress} who
	 *            emitted this change
	 * @param progress
	 *            the progress to set
	 */
	private void setTotalProgress(Progress pg, String name, int progress) {
		// TODO: name is not used... and this is probably a bug in this case
		synchronized (getLock()) {
			progress = Math.max(min, progress);
			progress = Math.min(max, progress);

			if (progress != this.progress) {
				this.progress = progress;
				changed(pg);
			}
		}
	}

	/**
	 * Notify the listeners that this {@link Progress} changed value.
	 * 
	 * @param pg
	 *            the emmiter
	 */
	private void changed(Progress pg) {
		if (pg == null) {
			pg = this;
		}

		synchronized (getLock()) {
			for (ProgressListener l : listeners) {
				l.progress(pg, name);
			}
		}
	}

	/**
	 * Add a {@link ProgressListener} that will trigger on progress changes.
	 * <p>
	 * Note: the {@link Progress} that will be reported will be the active
	 * progress, not necessarily the same as the current one (it could be a
	 * child {@link Progress} of this {@link Progress}).
	 * 
	 * @param l
	 *            the listener
	 */
	public void addProgressListener(ProgressListener l) {
		this.listeners.add(l);
	}

	/**
	 * Remove a {@link ProgressListener} that would trigger on progress changes.
	 * 
	 * @param l
	 *            the listener
	 * 
	 * @return TRUE if it was found (and removed)
	 */
	public boolean removeProgressListener(ProgressListener l) {
		return this.listeners.remove(l);
	}

	/**
	 * Add a child {@link Progress} of the given weight.
	 * 
	 * @param progress
	 *            the child {@link Progress} to add
	 * @param weight
	 *            the weight (on a {@link Progress#getMin()} to
	 *            {@link Progress#getMax()} scale) of this child
	 *            {@link Progress} in relation to its parent
	 * 
	 * @throws Error
	 *             if weight exceed {@link Progress#getMax()} or if progress
	 *             already has a parent
	 */
	public void addProgress(Progress progress, double weight) {
		if (weight < min || weight > max) {
			throw new Error(String.format(
					"Progress object %s cannot have a weight of %f, "
							+ "it is outside of its parent (%s) range (%f)",
					progress.name, weight, name, max));
		}

		if (progress.parent != null) {
			throw new Error(String.format(
					"Progress object %s cannot be added to %s, "
							+ "as it already has a parent (%s)", progress.name,
					name, progress.parent.name));
		}

		progress.addProgressListener(new ProgressListener() {
			@Override
			public void progress(Progress pg, String name) {
				synchronized (getLock()) {
					double total = ((double) localProgress) / (max - min);
					for (Entry<Progress, Double> entry : children.entrySet()) {
						total += (entry.getValue() / (max - min))
								* entry.getKey().getRelativeProgress();
					}

					if (name == null) {
						name = Progress.this.name;
					}

					setTotalProgress(pg, name,
							(int) Math.round(total * (max - min)));
				}
			}
		});

		this.children.put(progress, weight);
	}

	/**
	 * The lock object to use (this one or the recursively-parent one).
	 * 
	 * @return the lock object to use
	 */
	private Object getLock() {
		synchronized (lock) {
			if (parent != null) {
				return parent.getLock();
			}

			return lock;
		}
	}
}
