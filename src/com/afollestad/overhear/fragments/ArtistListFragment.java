package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.PopupMenu;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.base.OverhearGridFragment;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.tasks.LastfmGetArtistImage;
import com.afollestad.overhear.ui.ArtistViewer;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;

import java.util.ArrayList;

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
    public void onItemLongClick(int position, Cursor cursor, final View view) {
        final Activity context = getActivity();
        final Artist artist = Artist.fromCursor(cursor);

        PopupMenu menu = new PopupMenu(context, view);
        menu.inflate(R.menu.artist_item_popup);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.addToPlaylist: {
                        AlertDialog diag = MusicUtils.createPlaylistChooseDialog(context, null, null, artist);
                        diag.show();
                        return true;
                    }
                    case R.id.playAll: {
                        context.startService(new Intent(context, MusicService.class)
                                .setAction(MusicService.ACTION_PLAY_ALL)
                                .putExtra("scope", QueueItem.SCOPE_ARTIST)
                                .putExtra("artist", artist.getJSON().toString()));
                        return true;
                    }
                    case R.id.addToQueue: {
                        ArrayList<Song> content = Song.getAllFromScope(context, new String[]{
                                MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " +
                                        MediaStore.Audio.Media.ARTIST + " = '" + artist.getName().replace("'", "''") + "'",
                                MediaStore.Audio.Media.ALBUM});
                        MusicUtils.addToQueue(context, content, QueueItem.SCOPE_ARTIST);
                        return true;
                    }
                    case R.id.redownloadArt: {
                        new LastfmGetArtistImage(context, ((AImageView)view.findViewById(R.id.image))).execute(artist);
                        return true;
                    }
                }
                return false;
            }
        });
        menu.show();
    }

    @Override
    public void onInitialize() {
    }
}