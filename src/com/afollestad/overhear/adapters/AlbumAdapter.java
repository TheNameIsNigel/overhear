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
import com.afollestad.overhear.tasks.LastfmGetAlbumImage;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;
import com.androidquery.AQuery;

public class AlbumAdapter extends SimpleCursorAdapter {

	public AlbumAdapter(Activity context, int layout, Cursor c, String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		this.context = context;
        this.listAq = new AQuery(context);
	}

    private AQuery listAq;
	private Activity context;
	public final static String ALBUM_IMAGE = "album_image";

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.album_item, null);
	}

    public static void retrieveAlbumArt(Activity context, AQuery aq, String url, Album album, int viewId) {
        if(url == null) {
            url = MusicUtils.getImageURL(context, album.getName() + ":" + album.getArtist().getName(), ALBUM_IMAGE);
        }
        if (url == null) {
            new LastfmGetAlbumImage(context, viewId, aq).execute(album);
        } else {
            Bitmap bitmap = aq.getCachedImage(url);
            if (bitmap != null) {
                Log.i("Overhear.AlbumAdapter", "Loading image for " + album.getName() + " (" + album.getArtist().getName() + ") from cache.");
                aq.id(viewId).image(bitmap);
            } else {
                aq.id(viewId).image(url, true, true);
            }
        }
    }

    public static View getViewForAlbum(Activity context, Album album, View view, int position, ViewGroup parent, AQuery listAq) {
        if(view == null)
            view = LayoutInflater.from(context).inflate(R.layout.album_item, null);
        ((TextView)view.findViewById(R.id.title)).setText(album.getName());
        ((TextView)view.findViewById(R.id.artist)).setText(album.getArtist().getName());

        AQuery aq = null;
        if(listAq != null) {
            aq = listAq.recycle(view);
        } else {
            aq = new AQuery(context);
        }
        String url = MusicUtils.getImageURL(context, album.getName() + ":" + album.getArtist().getName(), ALBUM_IMAGE);
        if (aq.shouldDelay(position, view, parent, url)) {
            aq.id(R.id.image).image((Bitmap)null);
        } else {
            retrieveAlbumArt(context, aq, url, album, R.id.image);
        }

        ImageView peakOne = (ImageView)view.findViewById(R.id.peak_one);
        ImageView peakTwo = (ImageView)view.findViewById(R.id.peak_two);
        peakOne.setImageResource(R.anim.peak_meter_1);
        peakTwo.setImageResource(R.anim.peak_meter_2);
        AnimationDrawable mPeakOneAnimation = (AnimationDrawable)peakOne.getDrawable();
        AnimationDrawable mPeakTwoAnimation = (AnimationDrawable)peakTwo.getDrawable();

        Song focused = Queue.getFocused(context);
        if(focused != null && focused.isPlaying() && album.getName().equals(focused.getAlbum()) &&
                album.getArtist().equals(focused.getArtist())) {
            peakOne.setVisibility(View.VISIBLE);
            peakTwo.setVisibility(View.VISIBLE);
            if(!mPeakOneAnimation.isRunning()) {
                mPeakOneAnimation.start();
                mPeakTwoAnimation.start();
            }
        } else {
            peakOne.setVisibility(View.GONE);
            peakTwo.setVisibility(View.GONE);
            if(mPeakOneAnimation.isRunning()) {
                mPeakOneAnimation.stop();
                mPeakTwoAnimation.stop();
            }
        }

        return view;
    }

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
        if(view == null) {
            view = convertView;
        }

        Album album = Album.fromCursor(context, getCursor());
        view = getViewForAlbum(context, album, view, position, parent, listAq);

		int pad = context.getResources().getDimensionPixelSize(R.dimen.list_top_padding);
		if(position == 0) {
			if(getCount() == 1) {
				view.setPadding(0, pad, 0, pad);
			} else {
				view.setPadding(0, pad, 0, 0);
			}
		} else if(position == getCount() - 1) {
			view.setPadding(0, 0, 0, pad);
		} else {
			view.setPadding(0, 0, 0, 0);
        }

		return view;
	}
}