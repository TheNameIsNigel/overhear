package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.CursorAdapter;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.GenreAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.ui.GenreViewer;
import com.afollestad.overhearapi.Genre;

/**
 * Loads and displays a list of genres based on all songs on the device.
 * 
 * @author Aidan Follestad
 */
public class GenreListFragment extends OverhearListFragment {

	private GenreAdapter adapter;	
	
	public GenreListFragment() { }

    @Override
    public Uri getLoaderUri() {
        return MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI;
    }

    @Override
    public String getLoaderSelection() {
        return MediaStore.Audio.Genres.NAME + " != '' AND " + MediaStore.Audio.Genres.NAME + " IS NOT NULL";
    }

    @Override
    public String getLoaderSort() {
        return MediaStore.Audio.Genres.DEFAULT_SORT_ORDER;
    }

    @Override
    public CursorAdapter getAdapter() {
        if(adapter == null)
            adapter = new GenreAdapter(getActivity(), null, 0);
        return adapter;
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
    public String getEmptyText() {
        return getString(R.string.no_genres);
    }

    @Override
    public void onItemClick(int position, Cursor cursor) {
        Genre genre = Genre.fromCursor(adapter.getCursor());
        startActivity(new Intent(getActivity(), GenreViewer.class)
                .putExtra("genre", genre.getJSON().toString()));
    }

    @Override
    public void onInitialize() { }
}