package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.CursorAdapter;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.PlaylistAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.PlaylistViewer;
import com.afollestad.overhearapi.Playlist;

public class PlayListFragment extends OverhearListFragment {

    private PlaylistAdapter adapter;

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicService.RECENTS_UPDATED)) {
                getLoaderManager().restartLoader(0, null, PlayListFragment.this);
            } else if (intent.getAction().equals(MusicService.PLAYING_STATE_CHANGED) && adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };


    public PlayListFragment() {
    }

    @Override
    public Uri getLoaderUri() {
        return MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
    }

    @Override
    public String getLoaderSelection() {
        return null;
    }

    @Override
    public String getLoaderSort() {
        return MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER;
    }

    @Override
    public CursorAdapter getAdapter() {
        if(adapter == null)
            adapter = new PlaylistAdapter(getActivity(), null, 0);
        return adapter;
    }

    @Override
    public BroadcastReceiver getReceiver() {
        return null;
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
        return getString(R.string.no_playlists);
    }

    @Override
    public void onItemClick(int position, Cursor cursor) {
        Playlist list = Playlist.fromCursor(adapter.getCursor());
        startActivity(new Intent(getActivity(), PlaylistViewer.class)
                .putExtra("playlist", list.getJSON().toString()));
    }

    @Override
    public void onInitialize() {
    }
}