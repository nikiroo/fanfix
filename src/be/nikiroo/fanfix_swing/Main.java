package be.nikiroo.fanfix_swing;

import javax.swing.JFrame;

import be.nikiroo.fanfix.DataLoader;
import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.library.BasicLibrary;
import be.nikiroo.fanfix.library.LocalLibrary;
import be.nikiroo.fanfix_swing.gui.MainFrame;
import be.nikiroo.utils.ui.UIUtils;

public class Main {
	public static void main(String[] args) {
		UIUtils.setLookAndFeel();

		final String forceLib = null;
		// = "$HOME/Books/local";

		if (forceLib == null) {
			Instance.init();
		} else {
			Instance.init(new Instance() {
				private DataLoader cache;
				private BasicLibrary lib;

				@Override
				public DataLoader getCache() {
					if (cache == null) {
						cache = new DataLoader(getConfig().getString(Config.NETWORK_USER_AGENT));
					}

					return cache;
				}

				@Override
				public BasicLibrary getLibrary() {
					if (lib == null) {
						lib = new LocalLibrary(getFile(forceLib), getConfig()) {
						};
					}

					return lib;
				}
			});
		}

		JFrame main = new MainFrame(true, true);
		main.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		main.setVisible(true);
	}
}
