package be.nikiroo.fanfix.reader.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
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
		reader = config();
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	protected void onStart() {
		super.onStart();
		refresh();
	}

	private void refresh() {
		AndroidReaderGroup group = new AndroidReaderGroup();

		FragmentTransaction trans = getFragmentManager().beginTransaction();
		trans.replace(R.id.Main_pnlStories, group);
		trans.commit();
		getFragmentManager().executePendingTransactions();

		group.fill(reader.getLibrary().getList(), reader);
	}

	public void onAdd(View view) {
		final View root = findViewById(R.id.Main);

		ask(this,
				"Import new story",
				"Enter the story URL (the program will then download it -- the interface will not be usable until it is downloaded",
				"Download", new AnswerListener() {
					@Override
					public void onAnswer(final String answer) {
						root.setEnabled(false);
						new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									URL url = new URL(answer);
									reader.getLibrary().imprt(url, null);
								} catch (Throwable e) {
									// TODO: show error message correctly
									String mess = "";
									for (String tab = ""; e != null
											&& e != e.getCause(); e = e
											.getCause()) {
										mess += tab + "["
												+ e.getClass().getSimpleName()
												+ "] " + e.getMessage() + "\n";
										tab += "\t";
									}

									final String messf = mess;
									AndroidReaderActivity.this
											.runOnUiThread(new Runnable() {
												@Override
												public void run() {
													ask(AndroidReaderActivity.this,
															"Error",
															"Cannot import URL: \n"
																	+ messf,
															"OK", null);
												}
											});

								}

								AndroidReaderActivity.this
										.runOnUiThread(new Runnable() {
											@Override
											public void run() {
												refresh();
												root.setEnabled(true);
											}
										});
							}
						}).start();
					}
				});

		/*
		 * Intent intent = new Intent(AndroidReaderActivity.this, SayIt.class);
		 * intent.putExtra(SayIt.MESSAGE, message); startActivity(intent);
		 */
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

	private Reader config() {
		if (reader != null) {
			return reader;
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
		return BasicReader.getReader();
	}

	public static void ask(Context context, String title, String message,
			String okMessage, final AnswerListener listener) {
		final EditText input = new EditText(context);
		input.setFocusable(true);
		input.setInputType(InputType.TYPE_CLASS_TEXT);

		AlertDialog.Builder alert = new AlertDialog.Builder(context);
		alert.setTitle(title);
		alert.setMessage(message);
		alert.setCancelable(true);
		alert.setView(input);

		if (listener != null) {
			alert.setPositiveButton(okMessage,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							listener.onAnswer(input.getText().toString());
						}
					});

			alert.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					listener.onAnswer(null);
				}
			});
		}

		alert.show();
	}

	private interface AnswerListener {
		public void onAnswer(String answer);
	}
}