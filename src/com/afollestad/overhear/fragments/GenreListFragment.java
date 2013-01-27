package com.afollestad.overhear.fragments;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ListView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.GenreAdapter;
import com.afollestad.overhear.ui.GenreViewer;
import com.afollestad.overhearapi.Genre;

public class GenreListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private GenreAdapter adapter;	
	
	public GenreListFragment() {  }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}
	
	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		int pad = getResources().getDimensionPixelSize(R.dimen.list_side_padding);
		getListView().setPadding(pad, 0, pad, 0);
		getListView().setSmoothScrollbarEnabled(true);
		getListView().setFastScrollEnabled(true);
		setEmptyText(getString(R.string.no_genres));
	}

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        adapter.getCursor().moveToPosition(position);
        Genre genre = Genre.fromCursor(adapter.getCursor());
        startActivity(new Intent(getActivity(), GenreViewer.class)
                .putExtra("genre", genre.getJSON().toString()));
    }
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
		return new CursorLoader(getActivity(), 
				uri, 
				null, 
				null, 
				null, 
				MediaStore.Audio.Genres.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
		if(data == null)
			return;
		adapter = new GenreAdapter(getActivity(), data, 0);
        setListAdapter(adapter);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (adapter != null)
			adapter.changeCursor(null);
	}
}