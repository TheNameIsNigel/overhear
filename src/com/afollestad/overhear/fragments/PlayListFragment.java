package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.PlaylistAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.PlaylistViewer;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.silk.adapters.SilkCursorAdapter;

/**
 * Loads and displays a list of playlists on the device.
 *
 * @author Aidan Follestad
 */
public class PlayListFragment extends OverhearListFragment<Playlist> {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicService.RECENTS_UPDATED)) {
                getLoaderManager().restartLoader(0, null, PlayListFragment.this);
            } else if (intent.getAction().equals(MusicService.PLAYING_STATE_CHANGED) && getAdapter() != null) {
                getAdapter().notifyDataSetChanged();
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
    public String[] getLoaderProjection() {
        return null;
    }

    @Override
    public String getLoaderSort() {
        return MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER;
    }

    @Override
    public BroadcastReceiver getReceiver() {
        return mStatusReceiver;
    }

    @Override
    public IntentFilter getFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.RECENTS_UPDATED);
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        return filter;
    }

    @Override
    public int getEmptyText() {
        return R.string.no_playlists;
    }

    @Override
    protected SilkCursorAdapter<Playlist> initializeAdapter() {
        return new PlaylistAdapter(getActivity(), null);
    }

    @Override
    protected void onItemTapped(int index, Playlist item, View view) {
        startActivity(new Intent(getActivity(), PlaylistViewer.class)
                .putExtra("playlist", item.getJSON().toString()));
    }

    @Override
    protected boolean onItemLongTapped(int index, Playlist item, View view) {
        return false;
    }
}