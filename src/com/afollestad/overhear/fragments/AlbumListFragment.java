package com.afollestad.overhear.fragments;

import android.widget.AdapterView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhearapi.Album;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ListView;

public class AlbumListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private AlbumAdapter adapter;
	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if(adapter != null)
    			adapter.notifyDataSetChanged();
        }
    };
	
	
	public AlbumListFragment() { }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	@Override
	public void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        getActivity().registerReceiver(mStatusReceiver, filter);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		getActivity().unregisterReceiver(mStatusReceiver);
	}

	@Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new AlbumAdapter(getActivity(), 0, null, new String[] { }, new int[] { }, 0);
        setListAdapter(adapter);
        getLoaderManager().initLoader(0, null, this);
    }
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		int pad = getResources().getDimensionPixelSize(R.dimen.list_side_padding);
		getListView().setPadding(pad, 0, pad, 0);
		getListView().setSmoothScrollbarEnabled(true);
		getListView().setFastScrollEnabled(true);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.findViewById(R.id.options).performClick();
                return true;
            }
        });
		setEmptyText(getString(R.string.no_albums));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		adapter.getCursor().moveToPosition(position);
		Album album = Album.fromCursor(getActivity(), adapter.getCursor());
		startActivity(new Intent(getActivity(), AlbumViewer.class)
		.putExtra("album", album.getJSON().toString()));
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
		String where = null;
		if(getArguments() != null && getArguments().containsKey("artist_name")) {
			String name = getArguments().getString("artist_name").replace("'", "''");
			where = MediaStore.Audio.AlbumColumns.ARTIST + " = '" + name + "'";
		}
		return new CursorLoader(getActivity(), 
				uri, 
				null, 
				where, 
				null, 
				MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
		if(data == null)
			return;
		adapter.changeCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (adapter != null)
			adapter.changeCursor(null);
	}
}