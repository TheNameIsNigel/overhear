package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.CursorAdapter;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SongAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Genre;
import com.afollestad.overhearapi.Song;

public class SongListFragment extends OverhearListFragment {

    private SongAdapter adapter;
    private Genre genre;

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    };


    public SongListFragment() {
    }

    @Override
    public Uri getLoaderUri() {
        Uri uri = null;
        if (getArguments() != null && getArguments().containsKey("genre")) {
            genre = Genre.fromJSON(getArguments().getString("genre"));
            uri = MediaStore.Audio.Genres.Members.getContentUri("external", genre.getId());
        } else {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return uri;
    }

    @Override
    public String getLoaderSelection() {
        String[] scope = null;
        if (genre != null) {
            scope = new String[]{null, null};
        } else {
            scope = getScope(null);
        }
        return scope[0];
    }

    @Override
    public String getLoaderSort() {
        String[] scope = null;
        if (genre != null) {
            scope = new String[]{null, null};
        } else {
            scope = getScope(null);
        }
        return scope[1];
    }

    @Override
    public CursorAdapter getAdapter() {
        if (adapter == null) {
            adapter = new SongAdapter(getActivity(), null, 0);
        }
        return adapter;
    }

    @Override
    public BroadcastReceiver getReceiver() {
        return mStatusReceiver;
    }

    @Override
    public IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        return filter;
    }

    @Override
    public String getEmptyText() {
        return getString(R.string.no_songs);
    }

    @Override
    public void onItemClick(int position, Cursor cursor) {
        Song song = Song.fromCursor(adapter.getCursor());
        performOnClick(getActivity(), song, genre, getScope(song), position);
    }

    @Override
    public void onInitialize() {
        if(getArguments() != null && getArguments().containsKey("genre"))
            genre = Genre.fromJSON(getArguments().getString("genre"));
    }


    public static void performOnClick(Activity context, Song song, Genre genre, String[] scope, int position) {
    	Intent intent = new Intent(context, MusicService.class)
        	.setAction(MusicService.ACTION_PLAY_ALL)
        	.putExtra("song", song.getJSON().toString())
        	.putExtra("position", position);
    	if(genre != null) {
    		intent.putExtra("genre", genre.getJSON().toString());
    	} else {
    		intent.putExtra("scope", scope);
    	}
        context.startService(intent);
    }

    private String[] getScope(Song genreSong) {
        String sort = MediaStore.Audio.Media.TITLE;
        String where = MediaStore.Audio.Media.IS_MUSIC + " = 1";

        if (getArguments() != null) {
            sort = MediaStore.Audio.Media.TRACK;
            if (getArguments().containsKey("artist_id")) {
                where += " AND " + MediaStore.Audio.Media.ARTIST_ID + " = " + getArguments().getInt("artist_id");
            } else if (getArguments().containsKey("album_id")) {
            	where += " AND " + MediaStore.Audio.Media.ALBUM_ID + " = " + getArguments().getInt("album_id");
            } else if (getArguments().containsKey("artist_name")) {
                where += " AND " + MediaStore.Audio.Media.ARTIST + " = '" + getArguments().getString("artist_name").replace("'", "''") + "'";
            } else if (getArguments().containsKey("genre")) {
                sort = null;
            }
        }

        return new String[]{where, sort};
    }
}