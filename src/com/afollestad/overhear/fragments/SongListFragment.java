package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SongAdapter;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Genre;
import com.afollestad.overhearapi.Song;

public class SongListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private SongAdapter adapter;
    private Genre genre;

	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(adapter != null)
				adapter.notifyDataSetChanged();
		}
	};


	public SongListFragment() {  }

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
        performOnClick(getActivity(), song, getScope(song));
	}

    public static void performOnClick(Activity context, Song song, String[] scope) {
        context.startService(new Intent(context, MusicService.class)
                .setAction(MusicService.ACTION_PLAY_ALL)
                .putExtra("song", song.getJSON().toString())
                .putExtra("scope", scope));
    }
	
	private String[] getScope(Song genreSong) {
		String sort = MediaStore.Audio.Media.TITLE;
        String where = MediaStore.Audio.Media.IS_MUSIC + " = 1";

		if(getArguments() != null) {
			sort = MediaStore.Audio.Media.TRACK;
            if(getArguments().containsKey("artist_id")) {
                where += " AND " + MediaStore.Audio.Media.ARTIST_ID + " = " + getArguments().getInt("artist_id");
            } else if(getArguments().containsKey("album_id")) {
                where += " AND " + MediaStore.Audio.Media.ALBUM_ID + " = " + getArguments().getInt("album_id");
            } else if(getArguments().containsKey("artist_name")) {
                where += " AND " + MediaStore.Audio.Media.ARTIST + " = '" + getArguments().getString("artist_name").replace("'", "''") + "'";
            } else if(getArguments().containsKey("genre")) {
                where += " AND " + MediaStore.Audio.Media.ALBUM + " = '" + genreSong.getAlbum().replace("'", "''") + "'";
            }
		}

		return new String[] { where, sort };
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = null;
        String[] scope = null;
        if(getArguments() != null && getArguments().containsKey("genre")) {
            genre = Genre.fromJSON(getArguments().getString("genre"));
            uri = MediaStore.Audio.Genres.Members.getContentUri("external", genre.getId());
            scope = new String[] { null, null };
        } else {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            scope = getScope(null);
        }
        return new CursorLoader(getActivity(), uri, null, scope[0], null, scope[1]);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
		if(data == null)
			return;
		adapter = new SongAdapter(getActivity(), data, 0);
        if(getArguments() != null) {
            adapter.setShowTrackNumber(getArguments().getBoolean("show_track_number", false));
        }
		setListAdapter(adapter);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (adapter != null)
			adapter.changeCursor(null);
	}
}