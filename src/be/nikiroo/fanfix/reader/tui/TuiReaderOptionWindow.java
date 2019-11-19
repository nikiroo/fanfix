package be.nikiroo.fanfix.reader.tui;

import jexer.TStatusBar;
import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.bundles.Config;
import be.nikiroo.fanfix.bundles.UiConfig;

class TuiReaderOptionWindow extends TOptionWindow {
	public TuiReaderOptionWindow(TuiReaderApplication reader, boolean uiOptions) {
		super(reader, uiOptions ? UiConfig.class : Config.class,
				uiOptions ? Instance.getUiConfig() : Instance.getConfig(),
				"Options");

		TStatusBar statusBar = reader.setStatusBar(this, "Options");
	}
}
