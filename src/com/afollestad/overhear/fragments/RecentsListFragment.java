package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.widget.CursorAdapter;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhear.utils.Recents;
import com.afollestad.overhearapi.Album;

/**
 * Loads and displays your recently played music from the recents content provider.
 *
 * @author Aidan Follestad
 */
public class RecentsListFragment extends OverhearListFragment {

    private AlbumAdapter adapter;
    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicService.RECENTS_UPDATED)) {
                getLoaderManager().restartLoader(0, null, RecentsListFragment.this);
            } else if (intent.getAction().equals(MusicService.PLAYING_STATE_CHANGED) && adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };


    public RecentsListFragment() {
    }

    @Override
    public Uri getLoaderUri() {
        return Recents.PROVIDER_URI;
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
        return Recents.SORT;
    }

    @Override
    public CursorAdapter getAdapter() {
        if (adapter == null)
            adapter = new AlbumAdapter(getActivity(), 0, null, new String[]{}, new int[]{}, 0);
        return adapter;
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
    public String getEmptyText() {
        return getString(R.string.no_recents);
    }

    @Override
    public void onItemClick(int position, Cursor cursor) {
        Album album = Album.fromCursor(adapter.getCursor());
        startActivity(new Intent(getActivity(), AlbumViewer.class)
                .putExtra("album", album.getJSON().toString()));
    }

    @Override
    public void onInitialize() {
    }
}