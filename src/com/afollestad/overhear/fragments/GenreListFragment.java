package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.GenreAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.ui.GenreViewer;
import com.afollestad.overhearapi.Genre;
import com.afollestad.silk.adapters.SilkCursorAdapter;

/**
 * Loads and displays a list of genres based on all songs on the device.
 *
 * @author Aidan Follestad
 */
public class GenreListFragment extends OverhearListFragment<Genre> {

    public GenreListFragment() {
    }

    @Override
    public Uri getLoaderUri() {
        return MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
    }

    @Override
    public String getLoaderSelection() {
        return MediaStore.Audio.Genres.NAME + " != '' AND " + MediaStore.Audio.Genres.NAME + " IS NOT NULL";
    }

    @Override
    public String[] getLoaderProjection() {
        return null;
    }

    @Override
    public String getLoaderSort() {
        return MediaStore.Audio.Genres.DEFAULT_SORT_ORDER;
    }

    @Override
    public BroadcastReceiver getReceiver() {
        return null;
    }

    @Override
    public IntentFilter getFilter() {
        return null;
    }

    @Override
    public int getEmptyText() {
        return R.string.no_genres;
    }

    @Override
    protected SilkCursorAdapter<Genre> initializeAdapter() {
        return new GenreAdapter(getActivity(), null);
    }

    @Override
    protected void onItemTapped(int index, Genre item, View view) {
        startActivity(new Intent(getActivity(), GenreViewer.class)
                .putExtra("genre", item.getJSON().toString()));
    }

    @Override
    protected boolean onItemLongTapped(int index, Genre item, View view) {
        return false;
    }
}