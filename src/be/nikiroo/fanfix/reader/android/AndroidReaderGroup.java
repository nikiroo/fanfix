package be.nikiroo.fanfix.reader.android;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import be.nikiroo.fanfix.data.MetaData;
import be.nikiroo.fanfix.reader.Reader;

/**
 * A simple {@link Fragment} subclass. Activities that contain this fragment
 * must implement the {@link AndroidReaderGroup.OnFragmentInteractionListener}
 * interface to handle interaction events.
 */
public class AndroidReaderGroup extends Fragment {
	private OnFragmentInteractionListener listener;
	private Map<View, AndroidReaderBook> books = new HashMap<View, AndroidReaderBook>();

	public interface OnFragmentInteractionListener {
		void onFragmentInteraction(MetaData meta);
	}

	public AndroidReaderGroup() {
		// Required empty public constructor
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
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

	public void fill(final List<MetaData> metas, final Reader reader) {
		final List<MetaData> datas = new ArrayList<MetaData>(metas);

		ListView list = getView().findViewById(R.id.Group_root);
		list.setAdapter(new BaseAdapter() {
			@Override
			public int getCount() {
				return datas.size();
			}

			@Override
			public long getItemId(int position) {
				return -1; // TODO: what is a "row id" in this context?
			}

			@Override
			public Object getItem(int position) {
				return datas.get(position);
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				AndroidReaderBook book = books.get(convertView);
				if (book == null) {
					book = new AndroidReaderBook();

					FragmentTransaction trans = getFragmentManager()
							.beginTransaction();
					trans.add(book, null);
					trans.commit();
					getFragmentManager().executePendingTransactions();

					books.put(book.getView(), book);
				}

				MetaData meta = (MetaData) getItem(position);
				book.fill(meta, reader);

				return book.getView();
			}
		});
	}
}
