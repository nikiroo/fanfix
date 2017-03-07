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
		// will fire an action event:
		setProgress(this.localProgress);
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
	 */
	public void setMin(int min) {
		if (min < 0) {
			throw new Error("negative values not supported");
		}

		if (min > max) {
			throw new Error(
					"The minimum progress value must be <= the maximum progress value");
		}

		this.min = min;
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
	 */
	public void setMax(int max) {
		if (max < min) {
			throw new Error(
					"The maximum progress value must be >= the minimum progress value");
		}

		this.max = max;
	}

	/**
	 * Set both the minimum and maximum progress values.
	 * 
	 * @param min
	 *            the min
	 * @param max
	 *            the max
	 */
	public void setMinMax(int min, int max) {
		if (min < 0) {
			throw new Error("negative values not supported");
		}

		if (min > max) {
			throw new Error(
					"The minimum progress value must be <= the maximum progress value");
		}

		this.min = min;
		this.max = max;
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
		int diff = this.progress - this.localProgress;
		this.localProgress = progress;
		setTotalProgress(this, name, progress + diff);
	}

	/**
	 * Check if the action corresponding to this {@link Progress} is done (i.e.,
	 * if its progress value is >= its max value).
	 * 
	 * @return TRUE if it is
	 */
	public boolean isDone() {
		return progress >= max;
	}

	/**
	 * Get the total progress value (including the optional children
	 * {@link Progress}) on a 0.0 to 1.0 scale.
	 * 
	 * @return the progress
	 */
	public double getRelativeProgress() {
		return (((double) progress) / (max - min));
	}

	/**
	 * Return the list of direct children of this {@link Progress}.
	 * 
	 * @return the children (who will think of them??)
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
		this.progress = progress;

		for (ProgressListener l : listeners) {
			l.progress(pg, name);
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
	 */
	public void addProgress(Progress progress, double weight) {
		if (weight < min || weight > max) {
			throw new Error(
					"A Progress object cannot have a weight outside its parent range");
		}

		// Note: this is quite inefficient, especially with many children
		// TODO: improve it?
		progress.addProgressListener(new ProgressListener() {
			public void progress(Progress pg, String name) {
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
		});

		this.children.put(progress, weight);
	}
}
