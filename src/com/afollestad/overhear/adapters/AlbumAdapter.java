package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.App;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.WebArtUtils;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.tasks.LastfmGetAlbumImage;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

import java.util.ArrayList;

public class AlbumAdapter extends SimpleCursorAdapter {

    public AlbumAdapter(Activity context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.context = context;
    }

    private Activity context;

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.album_item, null);
    }

    public static void retrieveAlbumArt(Activity context, Album album, AImageView view) {
        view.setImageBitmap(null);
        String url = WebArtUtils.getImageURL(context, album);
        if (url == null) {
            new LastfmGetAlbumImage(context, context.getApplication(), view).execute(album);
        } else {
            view.setManager(((App) context.getApplication()).getManager()).setSource(url).load();
        }
    }

    public static View getViewForAlbum(final Activity context, final Album album, View view) {
        if (view == null)
            view = LayoutInflater.from(context).inflate(R.layout.album_item, null);
        ((TextView) view.findViewById(R.id.title)).setText(album.getName());
        ((TextView) view.findViewById(R.id.artist)).setText(album.getArtist().getName());

        View options = view.findViewById(R.id.options);
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(context, view);
                menu.inflate(R.menu.album_item_popup);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        switch (menuItem.getItemId()) {
                            case R.id.playAll: {
                                context.startService(new Intent(context, MusicService.class)
                                        .setAction(MusicService.ACTION_PLAY_ALL).putExtra("album_id", album.getAlbumId()));
                                return true;
                            }
                            case R.id.addToQueue: {
                                ArrayList<Song> content = Song.getAllFromScope(context, new String[]{
                                        MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " +
                                                MediaStore.Audio.Media.ALBUM_ID + " = " + album.getAlbumId(),
                                        MediaStore.Audio.Media.TRACK});
                                Queue.addToQueue(context, content);
                                return true;
                            }
                        }
                        return false;
                    }
                });
                menu.show();
            }
        });

        AImageView image = (AImageView) view.findViewById(R.id.image);
        retrieveAlbumArt(context, album, image);

        ImageView peakOne = (ImageView) view.findViewById(R.id.peak_one);
        ImageView peakTwo = (ImageView) view.findViewById(R.id.peak_two);
        peakOne.setImageResource(R.anim.peak_meter_1);
        peakTwo.setImageResource(R.anim.peak_meter_2);
        AnimationDrawable mPeakOneAnimation = (AnimationDrawable) peakOne.getDrawable();
        AnimationDrawable mPeakTwoAnimation = (AnimationDrawable) peakTwo.getDrawable();

        Song focused = Queue.getFocused(context);
        if (focused != null && album.getName().equals(focused.getAlbum()) && album.getArtist().getName().equals(focused.getArtist())) {
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (view == null) {
            view = convertView;
        }

        Album album = Album.fromCursor(getCursor());
        view = getViewForAlbum(context, album, view);

        int pad = context.getResources().getDimensionPixelSize(R.dimen.list_top_padding);
        if (position == 0) {
            if (getCount() == 1) {
                view.setPadding(0, pad, 0, pad);
            } else {
                view.setPadding(0, pad, 0, 0);
            }
        } else if (position == getCount() - 1) {
            view.setPadding(0, 0, 0, pad);
        } else {
            view.setPadding(0, 0, 0, 0);
        }

        return view;
    }
}