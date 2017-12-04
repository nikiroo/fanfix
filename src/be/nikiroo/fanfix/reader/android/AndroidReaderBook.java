package be.nikiroo.fanfix.reader.android;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

import be.nikiroo.fanfix.Instance;
import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.Reader;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.android.ImageUtilsAndroid;

public class AndroidReaderBook extends Fragment {
	private Reader reader;
	private OnFragmentInteractionListener listener;
	private MetaData meta;

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated to
	 * the activity and potentially other fragments contained in that activity.
	 * <p>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		void onFragmentInteraction(MetaData meta);
	}

	public AndroidReaderBook() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_android_reader_book,
				container, false);
	}

	@Override
	public void onAttach(Activity context) {
		super.onAttach(context);
		if (context instanceof OnFragmentInteractionListener) {
			listener = (OnFragmentInteractionListener) context;
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		listener = null;
	}

	public void fill(final Reader reader, final String luid) {
		View view = getView();
		if (view == null) {
			return;
		}

		final ImageView cover = view.findViewById(R.id.cover);
		final TextView title = view.findViewById(R.id.title);
		final FrameLayout frame = view.findViewById(R.id.coverWidget);

		new AsyncTask<Void, Void, MetaData>() {
			@Override
			protected MetaData doInBackground(Void[] objects) {
				return Instance.getLibrary().getInfo(luid);
			}

			@Override
			protected void onPostExecute(MetaData meta) {
				AndroidReaderBook.this.meta = meta;

				if (meta != null) {
					title.setText(meta.getTitle());
					try {
						Image coverImage = reader.getLibrary().getCover(
								meta.getLuid());
						if (coverImage != null) {
							Bitmap coverBitmap = ImageUtilsAndroid
									.fromImage(coverImage);
							coverBitmap = Bitmap.createScaledBitmap(
									coverBitmap, 128, 128, true);
							cover.setImageBitmap(coverBitmap);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				frame.setClickable(true);
				frame.setFocusable(true);
				frame.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						OnFragmentInteractionListener llistener = listener;
						if (llistener != null) {
							llistener
									.onFragmentInteraction(AndroidReaderBook.this.meta);
						}
					}
				});
			}
		}.execute();
	}
}
