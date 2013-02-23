package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.fragments.PlaylistSongFragment;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.overhearapi.Song;

import java.util.ArrayList;

public class PlaylistAdapter extends CursorAdapter {

    public PlaylistAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        this.activity = (Activity)context;
    }

    private Activity activity;

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.song_item, null);
    }

    public static View getViewForPlaylist(final Activity context, final Playlist playlist, View view, final int position) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.song_item, null);
        }

        ((TextView) view.findViewById(R.id.title)).setText(playlist.getName());

        View options = view.findViewById(R.id.options);
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(context, view);
                menu.inflate(R.menu.playlist_item_popup);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        final ArrayList<Song> listSongs = playlist.getSongs(context);
                        switch (menuItem.getItemId()) {
                            case R.id.playAll: {
                                PlaylistSongFragment.performOnClick(context, listSongs.get(0), playlist, position);
                                return true;
                            }
                            case R.id.addToQueue: {
                                Queue.addToQueue(context, listSongs);
                                return true;
                            }
                            case R.id.rename: {
                                //TODO
                                return true;
                            }
                            case R.id.delete: {
                                //TODO
                                return true;
                            }
                        }
                        return false;
                    }
                });
                menu.show();
            }
        });

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Playlist playlist = Playlist.fromCursor(cursor);
        getViewForPlaylist(activity, playlist, view, cursor.getPosition());
    }
}