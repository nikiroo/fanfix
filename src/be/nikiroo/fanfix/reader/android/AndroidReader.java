package be.nikiroo.fanfix.reader.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

import be.nikiroo.fanfix.reader.BasicReader;

public class AndroidReader extends BasicReader {
	private Activity app;

	/**
	 * Do not use.
	 */
	private AndroidReader() {
		// Required for reflection
	}

	public AndroidReader(Activity app) {
		this.app = app;
	}

	@Override
	public void read(boolean sync) throws IOException {
	}

	@Override
	public void browse(String source) {
	}

	@Override
	protected void start(File target, String program, boolean sync) throws IOException {
		if (program == null) {
			try {
				Intent[] intents = new Intent[] { //
				new Intent(Intent.ACTION_VIEW), //
						new Intent(Intent.ACTION_OPEN_DOCUMENT) //
				};

				for (Intent intent : intents) {
					intent.setDataAndType(Uri.parse(target.toURI().toString()),
							"application/x-cbz");
				}

				Intent chooserIntent = Intent.createChooser(intents[0],
						"Open CBZ in...");

				// chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
				// intents);

				app.startActivity(chooserIntent);
			} catch (UnsupportedOperationException e) {
				super.start(target, program, sync);
			}
		} else {
			super.start(target, program, sync);
		}
	}
}
