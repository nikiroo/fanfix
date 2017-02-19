package be.nikiroo.utils.test;

import be.nikiroo.utils.Progress;

public class ProgressTest extends TestLauncher {
	public ProgressTest(String[] args) {
		super("Progress reporting", args);

		addSeries(new TestLauncher("Simple progress", args) {
			{
				addTest(new TestCase("Relative values and direct values") {
					@Override
					public void test() throws Exception {
						Progress p = new Progress();
						assertEquals(0, p.getProgress());
						assertEquals(0, p.getRelativeProgress());
						p.setProgress(33);
						assertEquals(33, p.getProgress());
						assertEquals(0.33, p.getRelativeProgress());
						p.setMax(3);
						p.setProgress(1);
						assertEquals(1, p.getProgress());
						assertEquals(
								generateAssertMessage("0.33..",
										p.getRelativeProgress()), true,
								p.getRelativeProgress() >= 0.332);
						assertEquals(
								generateAssertMessage("0.33..",
										p.getRelativeProgress()), true,
								p.getRelativeProgress() <= 0.334);
					}
				});

				addTest(new TestCase("Listeners at first level") {
					int pg;

					@Override
					public void test() throws Exception {
						Progress p = new Progress();
						p.addProgressListener(new Progress.ProgressListener() {
							public void progress(Progress progress, String name) {
								pg = progress.getProgress();
							}
						});

						p.setProgress(42);
						assertEquals(42, pg);
						p.setProgress(0);
						assertEquals(0, pg);
					}
				});
			}
		});

		addSeries(new TestLauncher("Progress with children", args) {
			{
				addTest(new TestCase("One child") {
					@Override
					public void test() throws Exception {
						Progress p = new Progress();
						Progress child = new Progress();

						p.addProgress(child, 100);

						child.setProgress(42);
						assertEquals(42, p.getProgress());
					}
				});

				addTest(new TestCase("Multiple children") {
					@Override
					public void test() throws Exception {
						Progress p = new Progress();
						Progress child1 = new Progress();
						Progress child2 = new Progress();
						Progress child3 = new Progress();

						p.addProgress(child1, 20);
						p.addProgress(child2, 60);
						p.addProgress(child3, 20);

						child1.setProgress(50);
						assertEquals(10, p.getProgress());
						child2.setProgress(100);
						assertEquals(70, p.getProgress());
						child3.setProgress(100);
						assertEquals(90, p.getProgress());
						child1.setProgress(100);
						assertEquals(100, p.getProgress());
					}
				});

				addTest(new TestCase("Listeners with children") {
					int pg;

					@Override
					public void test() throws Exception {
						Progress p = new Progress();
						Progress child1 = new Progress();
						Progress child2 = new Progress();
						p.addProgress(child1, 50);
						p.addProgress(child2, 50);

						p.addProgressListener(new Progress.ProgressListener() {
							public void progress(Progress progress, String name) {
								pg = progress.getProgress();
							}
						});

						child1.setProgress(50);
						assertEquals(25, pg);
						child2.setProgress(100);
						assertEquals(75, pg);
						child1.setProgress(100);
						assertEquals(100, pg);
					}
				});

				addTest(new TestCase("Listeners with children, not 1-100") {
					int pg;

					@Override
					public void test() throws Exception {
						Progress p = new Progress();
						p.setMax(1000);

						Progress child1 = new Progress();
						child1.setMax(2);

						Progress child2 = new Progress();
						p.addProgress(child1, 500);
						p.addProgress(child2, 500);

						p.addProgressListener(new Progress.ProgressListener() {
							public void progress(Progress progress, String name) {
								pg = progress.getProgress();
							}
						});

						child1.setProgress(1);
						assertEquals(250, pg);
						child2.setProgress(100);
						assertEquals(750, pg);
						child1.setProgress(2);
						assertEquals(1000, pg);
					}
				});

				addTest(new TestCase(
						"Listeners with children, not 1-100, local progress") {
					int pg;

					@Override
					public void test() throws Exception {
						Progress p = new Progress();
						p.setMax(1000);

						Progress child1 = new Progress();
						child1.setMax(2);

						Progress child2 = new Progress();
						p.addProgress(child1, 400);
						p.addProgress(child2, 400);
						// 200 = local progress

						p.addProgressListener(new Progress.ProgressListener() {
							public void progress(Progress progress, String name) {
								pg = progress.getProgress();
							}
						});

						child1.setProgress(1);
						assertEquals(200, pg);
						child2.setProgress(100);
						assertEquals(600, pg);
						p.setProgress(100);
						assertEquals(700, pg);
						child1.setProgress(2);
						assertEquals(900, pg);
						p.setProgress(200);
						assertEquals(1000, pg);
					}
				});
			}
		});
	}
}
