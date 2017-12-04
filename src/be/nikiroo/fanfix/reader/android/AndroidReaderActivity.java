package be.nikiroo.fanfix.reader.android;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.BasicReader;
import be.nikiroo.fanfix.reader.Reader;
import be.nikiroo.utils.TraceHandler;

public class AndroidReaderActivity extends Activity implements
		AndroidReaderBook.OnFragmentInteractionListener {
	private static Reader reader = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		config();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	int i = 1;

	public void buttonClick(View view) {
		AndroidReaderGroup group = null;
		if (group == null) {
			group = new AndroidReaderGroup();
		}

		FragmentTransaction trans = getFragmentManager().beginTransaction();
		trans.replace(R.id.dropZone, group);
		trans.commit();
		getFragmentManager().executePendingTransactions();

		group.fill(reader, null);
	}

	public void onClick(View view) {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected void onPreExecute() {
				EditText editText = findViewById(R.id.editText);
				editText.setText("Downloading...");
			}

			@Override
			protected String doInBackground(Void... voids) {
				try {
					URL[] urls = new URL[] {
							new URL("https://e621.net/pool/show/13124"),
							new URL("https://e621.net/pool/show/13121"), };

					for (int i = 0; i < urls.length; i++) {
						if (reader.getLibrary().getList().size() <= i) {
							reader.getLibrary().imprt(urls[i], null);
						}
					}

					String message = "";
					for (MetaData meta : reader.getLibrary().getList()) {
						message += meta.getTitle() + "\n";
					}

					return message;
				} catch (Exception e) {
					return e.getClass() + ": " + e.getMessage();
				}
			}

			@Override
			protected void onPostExecute(String message) {
				EditText editText = findViewById(R.id.editText);
				editText.setText("testy");

				Intent intent = new Intent(AndroidReaderActivity.this,
						SayIt.class);
				intent.putExtra(SayIt.MESSAGE, message);
				startActivity(intent);
			}
		}.execute();
	}

	@Override
	public void onFragmentInteraction(MetaData meta) {
		AndroidReader reader = new AndroidReader(this);
		try {
			reader.openExternal(Instance.getLibrary(), meta.getLuid());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void config() {
		if (reader != null) {
			return;
		}

		String internal = getExternalFilesDir(null).toString();
		File user = Environment
				.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);

		try {
			File parent = user.getParentFile();
			if (parent.exists() || parent.mkdirs()) {
				File test = new File(parent, "test");
				if (test.exists() || (test.createNewFile() && test.delete())) {
					user = parent;
				}
			}
		} catch (Exception e) {
			// Fall back to Documents/Books
		}

		System.setProperty("DEBUG", "1");
		System.setProperty("fanfix.home", internal);
		System.setProperty("fanfix.libdir", new File(user, "Books").toString());

		Instance.resetConfig(false);
		Instance.setTraceHandler(new TraceHandler(true, true, 2));

		BasicReader.setDefaultReaderType(Reader.ReaderType.ANDROID);
		reader = BasicReader.getReader();
	}
}