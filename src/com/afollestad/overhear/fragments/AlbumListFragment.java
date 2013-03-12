package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.SimpleCursorAdapter;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;

/**
 * Loads and displays a list of albums based on all songs on the device.
 * 
 * @author Aidan Follestad
 */
public class AlbumListFragment extends OverhearListFragment {

	private AlbumAdapter adapter;
	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if(adapter != null)
    			adapter.notifyDataSetChanged();
        }
    };

	public AlbumListFragment() { }


    @Override
    public Uri getLoaderUri() {
        return MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
    }

    @Override
    public String getLoaderSelection() {
        if(getArguments() != null && getArguments().containsKey("artist")) {
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
    public SimpleCursorAdapter getAdapter() {
        if(adapter == null)
            adapter = new AlbumAdapter(getActivity(), 0, null, new String[] { }, new int[] { }, 0);
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
        return getString(R.string.no_albums);
    }

    @Override
    public void onItemClick(int position, Cursor cursor) {
        Album album = Album.fromCursor(adapter.getCursor());
        startActivity(new Intent(getActivity(), AlbumViewer.class)
                .putExtra("album", album.getJSON().toString()));
    }

    @Override
    public void onInitialize() { }
}