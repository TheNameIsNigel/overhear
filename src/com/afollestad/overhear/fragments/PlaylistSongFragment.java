package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.widget.CursorAdapter;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SongAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.overhearapi.Song;

public class PlaylistSongFragment extends OverhearListFragment {

    private SongAdapter adapter;
    private Playlist playlist;

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicService.RECENTS_UPDATED)) {
                getLoaderManager().restartLoader(0, null, PlaylistSongFragment.this);
            } else if (intent.getAction().equals(MusicService.PLAYING_STATE_CHANGED) && adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };


    public PlaylistSongFragment() {
    }

    @Override
    public Uri getLoaderUri() {
        return playlist.getSongUri();
    }

    @Override
    public String getLoaderSelection() {
        return null;
    }

    @Override
    public String getLoaderSort() {
        return null;
    }

    @Override
    public CursorAdapter getAdapter() {
        if(adapter == null) {
            adapter = new SongAdapter(getActivity(), null, 0);
            adapter.setShowTrackNumber(true);
        }
        return adapter;
    }

    @Override
    public BroadcastReceiver getReceiver() {
        return mStatusReceiver;
    }

    @Override
    public void onInitialize() {
        playlist = Playlist.fromJSON(getArguments().getString("playlist"));
    }

    @Override
    public IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.RECENTS_UPDATED);
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
        performClick(getActivity(), song, playlist, position);
    }

    public static void performClick(Activity context, Song song, Playlist list, int position) {
        context.startService(new Intent(context, MusicService.class)
                .setAction(MusicService.ACTION_PLAY_ALL)
                .putExtra("song", song.getJSON().toString())
                .putExtra("playlist", list.getJSON().toString())
                .putExtra("position", position));
    }
}