package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.silk.adapters.SilkCursorAdapter;

/**
 * Loads and displays a list of albums based on all songs on the device.
 *
 * @author Aidan Follestad
 */
public class AlbumListFragment extends OverhearListFragment<Album> {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getAdapter() != null)
                getAdapter().notifyDataSetChanged();
        }
    };

    public AlbumListFragment() {
    }

    @Override
    public Uri getLoaderUri() {
        return MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    }

    @Override
    public String getLoaderSelection() {
        if (getArguments() != null && getArguments().containsKey("artist")) {
            Artist artist = Artist.fromJSON(getArguments().getString("artist"));
            return MediaStore.Audio.AlbumColumns.ARTIST + " = '" + artist.getName().replace("'", "''") + "'";
        }
        return null;
    }

    @Override
    public String[] getLoaderProjection() {
        return null;
    }

    @Override
    public String getLoaderSort() {
        return MediaStore.Audio.Albums.DEFAULT_SORT_ORDER;
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
    public int getEmptyText() {
        return R.string.no_albums;
    }
}