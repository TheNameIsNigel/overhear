package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.CursorAdapter;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.base.OverhearGridFragment;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.ArtistViewer;
import com.afollestad.overhearapi.Artist;

/**
 * Loads and displays a list of artists based on all songs on the device.
 * 
 * @author Aidan Follestad
 */
public class ArtistListFragment extends OverhearGridFragment {

	private ArtistAdapter adapter;
	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if(adapter != null)
    			adapter.notifyDataSetChanged();
        }
    };

	public ArtistListFragment() { }


	@Override
    public Uri getLoaderUri() {
        return MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
    }

    @Override
    public String getLoaderSelection() {
        return null;
    }

    @Override
    public String getLoaderSort() {
        return MediaStore.Audio.Artists.DEFAULT_SORT_ORDER;
    }

    @Override
    public CursorAdapter getAdapter() {
        if(adapter == null)
            adapter = new ArtistAdapter(getActivity(), 0, null, new String[]{}, new int[]{}, 0);
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
        return getString(R.string.no_artists);
    }

    @Override
    public void onItemClick(int position, Cursor cursor) {
        Artist artist = Artist.fromCursor(getAdapter().getCursor());
        startActivity(new Intent(getActivity(), ArtistViewer.class)
                .putExtra("artist", artist.getJSON().toString()));
    }

    @Override
    public void onItemLongClick(int position, Cursor cursor, View view) {
        Artist artist = Artist.fromCursor(getAdapter().getCursor());
        ArtistAdapter.showPopup(getActivity(), artist, view);
    }

    @Override
    public void onInitialize() {
    }
}