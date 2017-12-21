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

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.Reader;
import be.nikiroo.utils.Image;
import be.nikiroo.utils.android.ImageUtilsAndroid;

public class AndroidReaderBook extends Fragment {
	private OnFragmentInteractionListener listener;

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

	public void fill(final MetaData meta, final Reader reader) {
		ViewHolder viewHolder = new ViewHolder(getView());

		viewHolder.title.setText(meta.getTitle());
		viewHolder.author.setText(meta.getAuthor());
		viewHolder.frame.setClickable(true);
		viewHolder.frame.setFocusable(true);
		viewHolder.frame.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OnFragmentInteractionListener llistener = listener;
				if (llistener != null) {
					llistener.onFragmentInteraction(meta);
				}
			}
		});

		new AsyncTask<MetaData, Void, Image>() {
			@Override
			protected Image doInBackground(MetaData[] metas) {
				if (metas[0].getCover() != null) {
					return metas[0].getCover();
				}

				return reader.getLibrary().getCover(metas[0].getLuid());
			}

			@Override
			protected void onPostExecute(Image coverImage) {
				ViewHolder viewHolder = new ViewHolder(getView());

				try {
					if (coverImage != null) {
						Bitmap coverBitmap = ImageUtilsAndroid
								.fromImage(coverImage);
						coverBitmap = Bitmap.createScaledBitmap(coverBitmap,
								128, 128, true);
						viewHolder.cover.setImageBitmap(coverBitmap);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.execute(meta);
	}

	private class ViewHolder {
		public FrameLayout frame;
		public TextView title;
		public TextView author;
		public ImageView cover;

		public ViewHolder(View book) {
			frame = book.findViewById(R.id.Book);
			title = book.findViewById(R.id.Book_lblTitle);
			author = book.findViewById(R.id.Book_lblAuthor);
			cover = book.findViewById(R.id.Book_imgCover);
		}
	}
}
