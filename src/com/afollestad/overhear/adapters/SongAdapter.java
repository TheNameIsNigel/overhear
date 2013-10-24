package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhear.ui.ArtistViewer;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.silk.adapters.SilkCursorAdapter;

public class SongAdapter extends SilkCursorAdapter<QueueItem> {

    private boolean isAlbum;

    public SongAdapter(Activity context, Cursor c) {
        super(context, R.layout.song_item, c, new CursorConverter<QueueItem>() {
            @Override
            public QueueItem convert(Cursor cursor) {
                return QueueItem.fromCursor(cursor, -1, QueueItem.SCOPE_All_SONGS);
            }
        });
    }

    public static View getViewForSong(final Activity context, final QueueItem song, View view, final boolean isAlbum) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.song_item, null);
        }
        ((TextView) view.findViewById(R.id.title)).setText(song.getTitle(context));
        View options = view.findViewById(R.id.options);
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(context, view);
                menu.inflate(R.menu.song_item_popup);
                if (isAlbum)
                    menu.getMenu().findItem(R.id.viewAlbum).setVisible(false);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.addToPlaylist: {
                                AlertDialog diag = MusicUtils.createPlaylistChooseDialog(context, song, null, null);
                                diag.show();
                                return true;
                            }
                            case R.id.addToQueue: {
                                MusicUtils.addToQueue(context, song);
                                return true;
                            }
                            case R.id.viewAlbum: {
                                Album album = Album.getAlbum(context, song.getAlbum(context), song.getArtist(context));
                                context.startActivity(new Intent(context, AlbumViewer.class)
                                        .putExtra("album", album.getJSON().toString()));
                                return true;
                            }
                            case R.id.viewArtist: {
                                Artist artist = Artist.getArtist(context, song.getArtist(context));
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

        QueueItem focused = null;
        boolean isPlaying = false;
        if (context instanceof OverhearActivity) {
            if (((OverhearActivity) context).getService() != null) {
                focused = ((OverhearActivity) context).getService().getQueue().getFocused();
                isPlaying = ((OverhearActivity) context).getService().isPlaying();
            }
        } else {
            if (((OverhearListActivity) context).getService() != null) {
                focused = ((OverhearListActivity) context).getService().getQueue().getFocused();
                isPlaying = ((OverhearListActivity) context).getService().isPlaying();
            }
        }

        if (focused != null && song.getSongId() == focused.getSongId() && song.getPlaylistId() == focused.getPlaylistId()) {
            peakOne.setVisibility(View.VISIBLE);
            peakTwo.setVisibility(View.VISIBLE);
            if (isPlaying) {
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

    public void setIsAlbum(boolean isAlbum) {
        this.isAlbum = isAlbum;
    }

    @Override
    public View onViewCreated(int index, View recycled, QueueItem item) {
        return getViewForSong((Activity) getContext(), item, recycled, isAlbum);
    }
}