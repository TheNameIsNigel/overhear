package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.base.OverhearGridFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.ArtistViewer;
import com.afollestad.overhearapi.Artist;
import com.afollestad.silk.adapters.SilkCursorAdapter;

/**
 * Loads and displays a list of artists based on all songs on the device.
 *
 * @author Aidan Follestad
 */
public class ArtistListFragment extends OverhearGridFragment<Artist> {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getAdapter() != null)
                getAdapter().notifyDataSetChanged();
        }
    };

    public ArtistListFragment() {
    }

    @Override
    public Uri getLoaderUri() {
        return MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    }

    @Override
    public String getLoaderSelection() {
        return null;
    }

    @Override
    protected String[] getLoaderProjection() {
        return new String[0];
    }

    @Override
    public String getLoaderSort() {
        return MediaStore.Audio.Artists.DEFAULT_SORT_ORDER;
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
        return R.string.no_artists;
    }

    @Override
    protected SilkCursorAdapter<Artist> initializeAdapter() {
        return new ArtistAdapter(getActivity(), null);
    }

    @Override
    protected void onItemTapped(int index, Artist item, View view) {
        startActivity(new Intent(getActivity(), ArtistViewer.class)
                .putExtra("artist", item.getJSON().toString()));
    }

    @Override
    protected boolean onItemLongTapped(int index, Artist item, View view) {
        ArtistAdapter.showPopup(getActivity(), item, view);
        return false;
    }
}