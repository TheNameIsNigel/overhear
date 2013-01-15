package com.afollestad.overhear.adapters;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.R;
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

public class SongAdapter extends CursorAdapter {

	public SongAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		this.musicContext = (MusicBoundActivity)context;
	}

	private MusicBoundActivity musicContext;

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.song_item, null);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Song song = Song.fromCursor(cursor);
		((TextView)view.findViewById(R.id.title)).setText(song.getTitle());
		((TextView)view.findViewById(R.id.duration)).setText(song.getDurationString());

		ImageView peakOne = (ImageView)view.findViewById(R.id.peak_one);
		ImageView peakTwo = (ImageView)view.findViewById(R.id.peak_two);
		peakOne.setImageResource(R.anim.peak_meter_1);
		peakTwo.setImageResource(R.anim.peak_meter_2);
		AnimationDrawable mPeakOneAnimation = (AnimationDrawable)peakOne.getDrawable();
		AnimationDrawable mPeakTwoAnimation = (AnimationDrawable)peakTwo.getDrawable();
		
		if(musicContext.getMusicService() != null) {
			Song nowPlaying = musicContext.getMusicService().getNowPlaying();
			if(nowPlaying != null && song.getId() == nowPlaying.getId()) {
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
}
