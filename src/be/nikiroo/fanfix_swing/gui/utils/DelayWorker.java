package be.nikiroo.fanfix_swing.gui.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.SwingWorker;

@SuppressWarnings("rawtypes")
public class DelayWorker {
	private Map<String, SwingWorker> lazyEnCours;
	private Object lazyEnCoursLock;

	private Object waiter;

	private boolean cont;
	private boolean paused;
	private Thread loop;

	public DelayWorker(final int delayMs) {
		lazyEnCours = new HashMap<String, SwingWorker>();
		lazyEnCoursLock = new Object();
		waiter = new Object();
		cont = true;
		paused = false;

		loop = new Thread(new Runnable() {
			@Override
			public void run() {
				while (cont) {
					try {
						Thread.sleep(delayMs);
					} catch (InterruptedException e) {
					}

					List<SwingWorker> workers;
					synchronized (lazyEnCoursLock) {
						workers = new LinkedList<SwingWorker>(
								lazyEnCours.values());
						lazyEnCours.clear();
					}
					for (SwingWorker worker : workers) {
						worker.execute();
					}

					synchronized (waiter) {
						do {
							try {
								if (cont)
									waiter.wait();
							} catch (InterruptedException e) {
							}
						} while (cont && paused);
					}
				}
			}
		});
		loop.setDaemon(true);
		loop.setName("Loop for DelayWorker");
	}

	// twice = not legal
	public void start() {
		loop.start();
	}

	public void pause() {
		paused = true;
	}

	public boolean isPaused() {
		return paused;
	}

	public void resume() {
		synchronized (waiter) {
			paused = false;
			wakeup();
		}
	}

	public void stop() {
		synchronized (waiter) {
			cont = false;
			wakeup();
		}
	}

	public void clear() {
		synchronized (lazyEnCoursLock) {
			lazyEnCours.clear();
		}
	}

	public <T, V> void delay(final String id, final SwingWorker<T, V> worker) {
		synchronized (lazyEnCoursLock) {
			lazyEnCours.put(id, worker);
		}

		wakeup();
	}

	private void wakeup() {
		synchronized (waiter) {
			waiter.notifyAll();
		}
	}
}
