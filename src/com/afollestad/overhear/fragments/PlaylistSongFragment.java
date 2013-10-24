package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.PlaylistSongAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.silk.adapters.SilkCursorAdapter;

/**
 * Loads and displays a list of songs from a playlist.
 *
 * @author Aidan Follestad
 */
public class PlaylistSongFragment extends OverhearListFragment<QueueItem> {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicService.RECENTS_UPDATED)) {
                getLoaderManager().restartLoader(0, null, PlaylistSongFragment.this);
            } else if (intent.getAction().equals(MusicService.PLAYING_STATE_CHANGED) && getAdapter() != null) {
                getAdapter().notifyDataSetChanged();
            }
        }
    };
    private Playlist playlist;

    public PlaylistSongFragment() {
    }

    public static void performClick(Activity context, int songId, Playlist list, int position) {
        context.startService(new Intent(context, MusicService.class)
                .setAction(MusicService.ACTION_PLAY_ALL)
                .putExtra("song", songId)
                .putExtra("playlist", list.getJSON().toString())
                .putExtra("scope", QueueItem.SCOPE_PLAYLIST)
                .putExtra("position", position));
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        playlist = Playlist.fromJSON(getArguments().getString("playlist"));
    }

    @Override
    public Uri getLoaderUri() {
        return playlist.getSongUri();
    }

    @Override
    public String getLoaderSelection() {
        return null;
    }

    @Override
    public String[] getLoaderProjection() {
        return QueueItem.getProjection(MediaStore.Audio.Playlists.Members.AUDIO_ID);
    }

    @Override
    public String getLoaderSort() {
        return null;
    }

    @Override
    protected SilkCursorAdapter<QueueItem> initializeAdapter() {
        return new PlaylistSongAdapter(getActivity(), null, playlist);
    }

    @Override
    protected void onItemTapped(int index, QueueItem item, View view) {
        performClick(getActivity(), item.getSongId(), playlist, index);
    }

    @Override
    protected boolean onItemLongTapped(int index, QueueItem item, View view) {
        return false;
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
        return R.string.no_songs;
    }
}