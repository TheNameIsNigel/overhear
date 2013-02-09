package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.App;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.WebArtUtils;
import com.afollestad.overhear.tasks.LastfmGetArtistImage;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;

public class ArtistAdapter extends SimpleCursorAdapter {

    public ArtistAdapter(Activity context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        this.context = context;
    }

    private Activity context;

    public static void retrieveArtistArt(Activity context, Artist artist, AImageView view) {
        view.setImageBitmap(null);
        String url = WebArtUtils.getImageURL(context, artist);
        if (url == null) {
            new LastfmGetArtistImage(context, view).execute(artist);
        } else {
            view.setManager(((App) context.getApplication()).getManager()).setSource(url).load();
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

        AImageView image = (AImageView) view.findViewById(R.id.image);
        retrieveArtistArt(context, artist, image);

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