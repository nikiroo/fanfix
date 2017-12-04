package be.nikiroo.fanfix.reader.android;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.Reader;

/**
 * A simple {@link Fragment} subclass. Activities that contain this fragment
 * must implement the {@link AndroidReaderGroup.OnFragmentInteractionListener}
 * interface to handle interaction events.
 */
public class AndroidReaderGroup extends Fragment {
	private OnFragmentInteractionListener listener;

	public interface OnFragmentInteractionListener {
		void onFragmentInteraction(MetaData meta);
	}

	public AndroidReaderGroup() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.fragment_android_reader_group,
				container, false);
	}

	@Override
	public void onAttach(Context context) {
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

	public void fill(final Reader reader, final String source) {
		new AsyncTask<Void, Void, List<MetaData>>() {
			@Override
			protected List<MetaData> doInBackground(Void... voids) {
				return reader.getLibrary().getListBySource(source);
			}

			@Override
			protected void onPostExecute(List<MetaData> metas) {
				for (MetaData meta : metas) {
					String tag = "Book_" + meta.getLuid();
					tag = null; // TODO: how does it work?
					AndroidReaderBook book = null;// (AndroidReaderBook)
													// getFragmentManager().findFragmentByTag(tag);
					if (book == null) {
						book = new AndroidReaderBook();
						FragmentTransaction trans = getFragmentManager()
								.beginTransaction();
						trans.add(R.id.AndroidReaderGroup_root, book, tag);
						trans.commit();
						getFragmentManager().executePendingTransactions();
					}
					book.fill(reader, meta.getLuid());
				}
			}
		}.execute();
	}
}
