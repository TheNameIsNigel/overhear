package com.afollestad.overhear.adapters;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.R;
import com.afollestad.overhear.tasks.AlbumImageLoader;
import com.afollestad.overhearapi.Album;
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

public class AlbumAdapter extends CursorAdapter {

	public AlbumAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		this.musicContext = (MusicBoundActivity)context;	}

	private MusicBoundActivity musicContext;
	
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.album_item, null);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		int position = cursor.getPosition();
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

		Album album = Album.fromCursor(context, cursor);
		((TextView)view.findViewById(R.id.title)).setText(album.getName());
		((TextView)view.findViewById(R.id.artist)).setText(album.getArtist().getName());
		final ImageView cover = (ImageView)view.findViewById(R.id.image); 

		int dimen = context.getResources().getDimensionPixelSize(R.dimen.album_list_cover);
		new AlbumImageLoader(context, cover, dimen, dimen).execute(album);
	
		ImageView peakOne = (ImageView)view.findViewById(R.id.peak_one);
		ImageView peakTwo = (ImageView)view.findViewById(R.id.peak_two);
		peakOne.setImageResource(R.anim.peak_meter_1);
		peakTwo.setImageResource(R.anim.peak_meter_2);
		AnimationDrawable mPeakOneAnimation = (AnimationDrawable)peakOne.getDrawable();
		AnimationDrawable mPeakTwoAnimation = (AnimationDrawable)peakTwo.getDrawable();

		if(musicContext.getMusicService() != null) {
			Song nowPlaying = musicContext.getMusicService().getNowPlaying();
			if(nowPlaying != null && album.getName().equals(nowPlaying.getAlbum())) {
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