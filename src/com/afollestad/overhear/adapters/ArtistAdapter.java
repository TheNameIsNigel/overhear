package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.tasks.LastfmGetArtistImage;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;
import com.androidquery.AQuery;

public class ArtistAdapter extends SimpleCursorAdapter {

    public ArtistAdapter(Activity context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.context = context;
        this.listAq = new AQuery(context);
    }

    private Activity context;
    public final static String ARTIST_IMAGE = "artist_image";
    private AQuery listAq;

    private static int getTargetWidth(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.gridview_image);
    }

    public static void retrieveArtistArt(Activity context, AQuery aq, String url, Artist artist, int viewId, int targetWidth, float ratio) {
        if(url == null) {
            url = MusicUtils.getImageURL(context, artist.getName(), ARTIST_IMAGE);
        }
        if (url == null) {
            new LastfmGetArtistImage(context, viewId, aq, targetWidth, ratio).execute(artist);
        } else {
            Bitmap bitmap = aq.getCachedImage(url);
            if (bitmap != null) {
                Log.i("Overhear.ArtistAdapter", "Loading image for " + artist.getName() + " from cache.");
                aq.id(viewId).image(bitmap);
            } else {
                aq.id(viewId).image(url, true, true, targetWidth, 0, null, 0, ratio);
            }
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        if (view == null) {
            if (convertView != null) {
                view = convertView;
            } else {
                view = newView(context, getCursor(), parent);
            }
        }

        Artist artist = Artist.fromCursor(getCursor());
        ((TextView) view.findViewById(R.id.title)).setText(artist.getName());

        AQuery aq = listAq.recycle(view);
        String url = MusicUtils.getImageURL(context, artist.getName(), ARTIST_IMAGE);
        if (aq.shouldDelay(position, view, parent, url)) {
            aq.id(R.id.image).image((Bitmap)null);
        } else {
            retrieveArtistArt(context, aq, url, artist, R.id.image, getTargetWidth(context), 1.0f / 1.0f);
        }

        ImageView peakOne = (ImageView) view.findViewById(R.id.peak_one);
        ImageView peakTwo = (ImageView) view.findViewById(R.id.peak_two);
        peakOne.setImageResource(R.anim.peak_meter_1);
        peakTwo.setImageResource(R.anim.peak_meter_2);
        AnimationDrawable mPeakOneAnimation = (AnimationDrawable) peakOne.getDrawable();
        AnimationDrawable mPeakTwoAnimation = (AnimationDrawable) peakTwo.getDrawable();

        Song focused = Queue.getFocused(context);
        if (focused != null && focused.isPlaying() && artist.getName().equals(focused.getArtist())) {
            peakOne.setVisibility(View.VISIBLE);
            peakTwo.setVisibility(View.VISIBLE);
            if (!mPeakOneAnimation.isRunning()) {
                mPeakOneAnimation.start();
                mPeakTwoAnimation.start();
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
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.grid_view_item, null);
    }
}