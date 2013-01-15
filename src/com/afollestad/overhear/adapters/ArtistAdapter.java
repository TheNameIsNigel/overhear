package com.afollestad.overhear.adapters;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.R;
import com.afollestad.overhear.tasks.ArtistImageLoader;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class ArtistAdapter extends CursorAdapter {

	public ArtistAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		this.musicContext = (MusicBoundActivity)context;
	}

	private MusicBoundActivity musicContext;
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Artist artist = Artist.fromCursor(cursor);
		((TextView)view.findViewById(R.id.title)).setText(artist.getName());
		final ImageView cover = (ImageView)view.findViewById(R.id.image);
			
		int dimen = context.getResources().getDimensionPixelSize(R.dimen.gridview_image);
		new ArtistImageLoader(context, cover, dimen, dimen).execute(artist);
		
		ImageView peakOne = (ImageView)view.findViewById(R.id.peak_one);
		ImageView peakTwo = (ImageView)view.findViewById(R.id.peak_two);
		peakOne.setImageResource(R.anim.peak_meter_1);
		peakTwo.setImageResource(R.anim.peak_meter_2);
		AnimationDrawable mPeakOneAnimation = (AnimationDrawable)peakOne.getDrawable();
		AnimationDrawable mPeakTwoAnimation = (AnimationDrawable)peakTwo.getDrawable();
		
		if(musicContext.getMusicService() != null) {
			Song nowPlaying = musicContext.getMusicService().getNowPlaying();
			if(nowPlaying != null && artist.getName().equals(nowPlaying.getArtist())) {
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
		} else {
			peakOne.setVisibility(View.GONE);
			peakTwo.setVisibility(View.GONE);
		}
	}
 
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.grid_view_item, null);
	}
}