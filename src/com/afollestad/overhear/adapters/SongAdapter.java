package com.afollestad.overhear.adapters;

import java.util.ArrayList;

import com.afollestad.overhear.MusicUtils;
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
	}
	
	public ArrayList<Song> getSongs() {
		ArrayList<Song> songs = new ArrayList<Song>();
		getCursor().moveToFirst();
		songs.add(Song.fromCursor(getCursor()));
		while(getCursor().moveToNext()) {
			songs.add(Song.fromCursor(getCursor()));
		}
		return songs;
	}
	
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
		
		Song nowPlaying = MusicUtils.getNowPlaying(context);
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
	}
}
