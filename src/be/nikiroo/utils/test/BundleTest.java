package be.nikiroo.utils.test;

import java.io.File;

import be.nikiroo.utils.IOUtils;
import be.nikiroo.utils.resources.Bundle;
import be.nikiroo.utils.resources.Bundles;
import be.nikiroo.utils.resources.Meta;

class BundleTest extends TestLauncher {
	private File tmp;
	private B b = new B();

	protected boolean isMain() {
		return true;
	}

	public BundleTest(String[] args) {
		this("Bundle test", args);
	}

	protected BundleTest(String name, String[] args) {
		super(name, args);

		addTests();

		if (isMain()) {
			addSeries(new BundleTest("After saving/reloading the resources",
					args) {
				@Override
				protected void start() throws Exception {
					tmp = File.createTempFile("nikiroo-utils", ".test");
					tmp.delete();
					tmp.mkdir();
					b.updateFile(tmp.getAbsolutePath());
					Bundles.setDirectory(tmp.getAbsolutePath());
					b.reload();
				}

				@Override
				protected void stop() {
					IOUtils.deltree(tmp);
				}

				@Override
				protected boolean isMain() {
					return false;
				}
			});
		}
	}

	private void addTests() {
		String pre = "";

		addTest(new TestCase(pre + "getString simple") {
			@Override
			public void test() throws Exception {
				assertEquals("un", b.getString(E.ONE));
			}
		});

		addTest(new TestCase(pre + "getStringX with null suffix") {
			@Override
			public void test() throws Exception {
				assertEquals("un", b.getStringX(E.ONE, null));
			}
		});

		addTest(new TestCase(pre + "getStringX with empty suffix") {
			@Override
			public void test() throws Exception {
				assertEquals(null, b.getStringX(E.ONE, ""));
			}
		});

		addTest(new TestCase(pre + "getStringX with existing suffix") {
			@Override
			public void test() throws Exception {
				assertEquals("un + suffix", b.getStringX(E.ONE, "suffix"));
			}
		});

		addTest(new TestCase(pre + "getStringX with not existing suffix") {
			@Override
			public void test() throws Exception {
				assertEquals(null, b.getStringX(E.ONE, "fake"));
			}
		});

		addTest(new TestCase(pre + "getString with UTF-8 content") {
			@Override
			public void test() throws Exception {
				assertEquals("日本語 Nihongo", b.getString(E.JAPANESE));
			}
		});
	}

	/**
	 * {@link Bundle}.
	 * 
	 * @author niki
	 */
	private class B extends Bundle<E> {
		protected B() {
			super(E.class, N.bundle_test);
		}

	}

	/**
	 * Key enum for the {@link Bundle}.
	 * 
	 * @author niki
	 */
	private enum E {
		@Meta(what = "", where = "", format = "", info = "")
		ONE, //
		@Meta(what = "", where = "", format = "", info = "")
		ONE_SUFFIX, //
		@Meta(what = "", where = "", format = "", info = "")
		TWO, //
		@Meta(what = "", where = "", format = "", info = "")
		JAPANESE
	}

	/**
	 * Name enum for the {@link Bundle}.
	 * 
	 * @author niki
	 */
	private enum N {
		bundle_test
	}
}
