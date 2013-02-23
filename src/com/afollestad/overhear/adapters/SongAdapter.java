package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhear.utils.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhear.ui.ArtistViewer;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;

public class SongAdapter extends CursorAdapter {

    public SongAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        this.activity = (Activity)context;
    }

    private boolean showTrackNumher = false;
    private Activity activity;

    public void setShowTrackNumber(boolean show) {
        this.showTrackNumher = show;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.song_item, null);
    }

    public static View getViewForSong(final Activity context, final Song song, View view, int trackNumber) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.song_item, null);
        }

        ((TextView) view.findViewById(R.id.title)).setText((trackNumber > -1 ? (trackNumber + 1) + ". " : "") + song.getTitle());

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
                            case R.id.addToPlaylist: {
                                AlertDialog diag = MusicUtils.createPlaylistChooseDialog(context, song, null);
                                diag.show();
                                return true;
                            }
                            case R.id.addToQueue: {
                                Queue.addToQueue(context, song);
                                return true;
                            }
                            case R.id.viewAlbum: {
                                Album album = Album.getAlbum(context, song.getAlbum(), song.getArtist());
                                context.startActivity(new Intent(context, AlbumViewer.class)
                                        .putExtra("album", album.getJSON().toString()));
                                return true;
                            }
                            case R.id.viewArtist: {
                                Artist artist = Artist.getArtist(context, song.getArtist());
                                context.startActivity(new Intent(context, ArtistViewer.class)
                                        .putExtra("artist", artist.getJSON().toString()));
                                return true;
                            }
                        }
                        return false;
                    }
                });
                menu.show();
            }
        });

        ImageView peakOne = (ImageView) view.findViewById(R.id.peak_one);
        ImageView peakTwo = (ImageView) view.findViewById(R.id.peak_two);
        peakOne.setImageResource(R.anim.peak_meter_1);
        peakTwo.setImageResource(R.anim.peak_meter_2);
        AnimationDrawable mPeakOneAnimation = (AnimationDrawable) peakOne.getDrawable();
        AnimationDrawable mPeakTwoAnimation = (AnimationDrawable) peakTwo.getDrawable();

        Song focused = Queue.getFocused(context);
        if (focused != null && song.getId() == focused.getId()) {
            peakOne.setVisibility(View.VISIBLE);
            peakTwo.setVisibility(View.VISIBLE);
            if (focused.isPlaying()) {
                if (!mPeakOneAnimation.isRunning()) {
                    mPeakOneAnimation.start();
                    mPeakTwoAnimation.start();
                }
            } else {
                mPeakOneAnimation.stop();
                mPeakOneAnimation.selectDrawable(0);
                mPeakTwoAnimation.stop();
                mPeakTwoAnimation.selectDrawable(0);
            }
        } else {
            peakOne.setVisibility(View.GONE);
            peakTwo.setVisibility(View.GONE);
            if (mPeakOneAnimation.isRunning()) {
                mPeakOneAnimation.stop();
                mPeakTwoAnimation.stop();
            }
        }

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Song song = Song.fromCursor(cursor);
        getViewForSong(activity, song, view, showTrackNumher ? cursor.getPosition() : -1);
    }
}