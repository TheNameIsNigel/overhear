package com.afollestad.overhear.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Playlist;

public class PlaylistAdapter extends CursorAdapter {

    public PlaylistAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.song_item, null);
    }

    public static View getViewForPlaylist(final Context context, final Playlist playlist, View view) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.song_item, null);
        }

        ((TextView) view.findViewById(R.id.title)).setText(playlist.getName());

        View options = view.findViewById(R.id.options);
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(context, view);
                menu.inflate(R.menu.song_item_popup);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.addToQueue: {
                                //Queue.addToQueue(context, song);
                                return true;
                            }
                            //TODO
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
        getViewForPlaylist(context, playlist, view);
    }
}