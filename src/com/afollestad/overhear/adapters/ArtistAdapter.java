package com.afollestad.overhear.adapters;

import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.tasks.ArtistOrAlbumImage;
import com.afollestad.overhear.tasks.LastfmGetArtistImage;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;
import com.androidquery.AQuery;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ArtistAdapter extends SimpleCursorAdapter {

	public ArtistAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		this.context = (Activity)context;
	}

	private Activity context;
	public final static String ARTIST_IMAGE = "artist_image";
	
	public static void startArtistArtTask(Activity context, Artist artist, ImageView cover, int width) {
		if (MusicUtils.getImageURL(context, artist.getName(), ARTIST_IMAGE) == null) {
            new LastfmGetArtistImage(context, cover).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR, artist);
        } else {
        	if(width == 0) {
        		width = context.getResources().getDimensionPixelSize(R.dimen.gridview_image);
        	}
//            if(height == 0) {
//                height  = context.getResources().getDimensionPixelSize(R.dimen.gridview_image);
//            }
            new ArtistOrAlbumImage(context, cover, ARTIST_IMAGE, width, -1).executeOnExecutor(
                    AsyncTask.THREAD_POOL_EXECUTOR, artist.getName());
        }
	}
	
	@Override
    public View getView(final int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		if(view == null) {
			if(convertView != null) {
				view = convertView;
			} else {
				view = newView(context, getCursor(), parent);
			}
		}
		
		Artist artist = Artist.fromCursor(getCursor());
		((TextView)view.findViewById(R.id.title)).setText(artist.getName());
		final ImageView cover = (ImageView)view.findViewById(R.id.image);
			
		final AQuery aq = new AQuery(view);
		if (aq.shouldDelay(position, view, parent, "")) {
            cover.setImageDrawable(null);
        } else {
            startArtistArtTask(context, artist, cover, 0);
        }
		
		ImageView peakOne = (ImageView)view.findViewById(R.id.peak_one);
		ImageView peakTwo = (ImageView)view.findViewById(R.id.peak_two);
		peakOne.setImageResource(R.anim.peak_meter_1);
		peakTwo.setImageResource(R.anim.peak_meter_2);
		AnimationDrawable mPeakOneAnimation = (AnimationDrawable)peakOne.getDrawable();
		AnimationDrawable mPeakTwoAnimation = (AnimationDrawable)peakTwo.getDrawable();
		
		Song focused = Queue.getFocused(context);
		if(focused != null && focused.isPlaying() && artist.getName().equals(focused.getArtist())) {
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
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.grid_view_item, null);
	}
}