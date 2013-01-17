package com.afollestad.overhear.fragments;

import com.afollestad.overhear.MusicService;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
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

public class RecentsListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private AlbumAdapter adapter;
	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if(adapter != null)
    			adapter.notifyDataSetChanged();
        }
    };
	
	
	public RecentsListFragment() { }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		Album album = Album.getAlbum(getActivity(), "Believe", "Justin Bieber");
		getActivity().getContentResolver().insert(Uri.parse("content://com.afollestad.overhear.recentsprovider"), 
				album.getContentValues());
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
		setEmptyText(getString(R.string.no_recents));
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
		return new CursorLoader(getActivity(), 
				Uri.parse("content://com.afollestad.overhear.recentsprovider"), 
				null, 
				null, 
				null, 
				MediaStore.Audio.Albums.ALBUM);
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