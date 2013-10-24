package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.view.View;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhear.utils.Recents;
import com.afollestad.overhearapi.Album;
import com.afollestad.silk.adapters.SilkCursorAdapter;

/**
 * Loads and displays your recently played music from the recents content provider.
 *
 * @author Aidan Follestad
 */
public class RecentsListFragment extends OverhearListFragment<Album> {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicService.RECENTS_UPDATED)) {
                getLoaderManager().restartLoader(0, null, RecentsListFragment.this);
            } else if (intent.getAction().equals(MusicService.PLAYING_STATE_CHANGED) && getAdapter() != null) {
                getAdapter().notifyDataSetChanged();
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
        return R.string.no_recents;
    }

    @Override
    protected SilkCursorAdapter<Album> initializeAdapter() {
        return new AlbumAdapter(getActivity(), null);
    }

    @Override
    protected void onItemTapped(int index, Album item, View view) {
        startActivity(new Intent(getActivity(), AlbumViewer.class)
                .putExtra("album", item.getJSON().toString()));
    }

    @Override
    protected boolean onItemLongTapped(int index, Album item, View view) {
        return false;
    }
}