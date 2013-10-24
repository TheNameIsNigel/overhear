package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SongAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Genre;
import com.afollestad.silk.adapters.SilkCursorAdapter;

/**
 * Loads and displays a list of songs; used for displaying all songs, songs from albums, songs from artists, etc.
 *
 * @author Aidan Follestad
 */
public class SongListFragment extends OverhearListFragment<QueueItem> {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getAdapter() != null)
                getAdapter().notifyDataSetChanged();
        }
    };
    private Genre genre;

    public SongListFragment() {
    }

    public static void performOnClick(Activity context, QueueItem song, Album album, Artist artist, Genre genre, int scope, int position) {
        Intent intent = new Intent(context, MusicService.class)
                .setAction(MusicService.ACTION_PLAY_ALL)
                .putExtra("song", song.getSongId())
                .putExtra("scope", scope)
                .putExtra("position", position);
        if (album != null)
            intent.putExtra("album", album.getJSON().toString());
        if (artist != null)
            intent.putExtra("artist", artist.getJSON().toString());
        if (genre != null)
            intent.putExtra("genre", genre.getJSON().toString());
        context.startService(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey("genre"))
            genre = Genre.fromJSON(getArguments().getString("genre"));
    }

    @Override
    public Uri getLoaderUri() {
        Uri uri;
        if (getArguments() != null && getArguments().containsKey("genre")) {
            genre = Genre.fromJSON(getArguments().getString("genre"));
            uri = MediaStore.Audio.Genres.Members.getContentUri("external", genre.getId());
        } else {
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }
        return uri;
    }

    @Override
    public String getLoaderSelection() {
        String[] scope;
        if (genre != null) {
            scope = new String[]{null, null};
        } else {
            scope = getScope();
        }
        return scope[0];
    }

    @Override
    public String[] getLoaderProjection() {
        return QueueItem.getProjection("_id");
    }

    @Override
    public String getLoaderSort() {
        String[] scope;
        if (genre != null) {
            scope = new String[]{null, null};
        } else {
            scope = getScope();
        }
        return scope[1];
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
        return R.string.no_songs;
    }

    @Override
    protected SilkCursorAdapter<QueueItem> initializeAdapter() {
        SongAdapter adapter = new SongAdapter(getActivity(), null);
        adapter.setIsAlbum(getArguments() != null && getArguments().containsKey("album"));
        return adapter;
    }

    @Override
    protected void onItemTapped(int index, QueueItem item, View view) {
        Album album = null;
        Artist artist = null;
        if (getArguments() != null) {
            if (getArguments().containsKey("album"))
                album = Album.fromJSON(getArguments().getString("album"));
            if (getArguments().containsKey("artist"))
                artist = Artist.fromJSON(getArguments().getString("artist"));
        }
        performOnClick(getActivity(), item, album, artist, genre, getScopeInt(), index);
    }

    @Override
    protected boolean onItemLongTapped(int index, QueueItem item, View view) {
        return false;
    }

    private String[] getScope() {
        String sort = MediaStore.Audio.Media.TITLE;
        String where = MediaStore.Audio.Media.IS_MUSIC + " = 1";

        if (getArguments() != null) {
            switch (getScopeInt()) {
                default:
                    break;
                case QueueItem.SCOPE_ARTIST: {
                    Artist artist = Artist.fromJSON(getArguments().getString("artist"));
                    sort = MediaStore.Audio.Media.ALBUM;
                    where += " AND " + MediaStore.Audio.Media.ARTIST_ID + " = " + artist.getId();
                    break;
                }
                case QueueItem.SCOPE_ALBUM: {
                    Album album = Album.fromJSON(getArguments().getString("album"));
                    sort = MediaStore.Audio.Media.TRACK;
                    where += " AND " + MediaStore.Audio.Media.ALBUM_ID + " = " + album.getAlbumId();
                    break;
                }
                case QueueItem.SCOPE_GENRE: {
                    sort = null;
                    break;
                }
            }
        }

        return new String[]{where, sort};
    }

    public int getScopeInt() {
        if (getArguments() != null) {
            if (getArguments().containsKey("artist")) {
                return QueueItem.SCOPE_ARTIST;
            } else if (getArguments().containsKey("album")) {
                return QueueItem.SCOPE_ALBUM;
            } else if (getArguments().containsKey("genre")) {
                return QueueItem.SCOPE_GENRE;
            } else {
                return QueueItem.SCOPE_All_SONGS;
            }
        } else {
            return QueueItem.SCOPE_All_SONGS;
        }
    }
}