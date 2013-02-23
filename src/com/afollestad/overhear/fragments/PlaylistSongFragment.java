package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SongAdapter;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.overhearapi.Song;

public class PlaylistSongFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private SongAdapter adapter;
    private Playlist playlist;

	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(adapter != null)
				adapter.notifyDataSetChanged();
		}
	};


	public PlaylistSongFragment() {  }

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
    public void onResume() {
        super.onResume();
        if(adapter != null)
            adapter.notifyDataSetChanged();
    }

	@Override
	public void onStop() {
		super.onStop();
		getActivity().unregisterReceiver(mStatusReceiver);
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
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.findViewById(R.id.options).performClick();
                return true;
            }
        });
		setEmptyText(getString(R.string.no_songs));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
        adapter.getCursor().moveToPosition(position);
        Song song = Song.fromCursor(adapter.getCursor());
        performOnClick(getActivity(), song, playlist, position);
	}

    public static void performOnClick(Activity context, Song song, Playlist list, int position) {
        context.startService(new Intent(context, MusicService.class)
                .setAction(MusicService.ACTION_PLAY_ALL)
                .putExtra("song", song.getJSON().toString())
                .putExtra("playlist", list.getJSON().toString())
                .putExtra("position", position));
    }
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        playlist = Playlist.fromJSON(getArguments().getString("playlist"));
        return new CursorLoader(getActivity(), playlist.getSongUri(), null, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
		if(data == null)
			return;
		adapter = new SongAdapter(getActivity(), data, 0);
        adapter.setShowTrackNumber(true);
		setListAdapter(adapter);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (adapter != null)
			adapter.changeCursor(null);
	}
}