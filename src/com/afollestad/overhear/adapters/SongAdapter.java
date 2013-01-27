package com.afollestad.overhear.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Song;

import java.util.ArrayList;

public class SongAdapter extends CursorAdapter {

	public SongAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
	}

    private boolean showArtist = true;

    public void setShowArtist(boolean show) {
        showArtist = show;
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

    public static View getViewForSong(Context context, Song song, View view, boolean showArtist) {
        if(view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.song_item, null);
        }
        ((TextView)view.findViewById(R.id.title)).setText(song.getTitle());
        TextView artist = (TextView)view.findViewById(R.id.artist);
        if(showArtist) {
            artist.setVisibility(View.VISIBLE);
            artist.setText(song.getArtist());
        } else {
            artist.setVisibility(View.GONE);
        }

        ImageView peakOne = (ImageView)view.findViewById(R.id.peak_one);
        ImageView peakTwo = (ImageView)view.findViewById(R.id.peak_two);
        peakOne.setImageResource(R.anim.peak_meter_1);
        peakTwo.setImageResource(R.anim.peak_meter_2);
        AnimationDrawable mPeakOneAnimation = (AnimationDrawable)peakOne.getDrawable();
        AnimationDrawable mPeakTwoAnimation = (AnimationDrawable)peakTwo.getDrawable();

        Song focused = Queue.getFocused(context);
        if(focused != null && focused.isPlaying() && song.getId() == focused.getId()) {
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
	public void bindView(View view, Context context, Cursor cursor) {
		Song song = Song.fromCursor(cursor);
		getViewForSong(context, song, view, showArtist);
	}
}
