package com.afollestad.overhear.adapters;

import java.util.ArrayList;

import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;
import com.androidquery.AQuery;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class RecentsAdapter extends BaseAdapter {

	public RecentsAdapter(Activity context) {
		this.context = context;
	}
	
	private Activity context;
	
	@Override
	public int getCount() {
		return getRecents().size();
	}

	@Override
	public Object getItem(int position) {
		return Album.getAlbum(context, getRecents().get(position).getAlbum());
	}

	@Override
	public long getItemId(int pos) {
		return getRecents().get(pos).getId();
	}

	public ArrayList<Song> getRecents() {
		return MusicUtils.getRecents(context);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = null;
		if(convertView != null) {
			view = convertView;
		} else {
			view = LayoutInflater.from(context).inflate(R.layout.album_item, null);
		}
		
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

		Album album = (Album)getItem(position);
		((TextView)view.findViewById(R.id.title)).setText(album.getName());
		((TextView)view.findViewById(R.id.artist)).setText(album.getArtist().getName());
		final ImageView cover = (ImageView)view.findViewById(R.id.image); 

		final AQuery aq = new AQuery(view);
		if (aq.shouldDelay(position, view, parent, "")) {
            cover.setImageDrawable(null);
        } else {
            AlbumAdapter.startAlbumArtTask(context, album, cover, 0);
        }
	
		ImageView peakOne = (ImageView)view.findViewById(R.id.peak_one);
		ImageView peakTwo = (ImageView)view.findViewById(R.id.peak_two);
		peakOne.setImageResource(R.anim.peak_meter_1);
		peakTwo.setImageResource(R.anim.peak_meter_2);
		AnimationDrawable mPeakOneAnimation = (AnimationDrawable)peakOne.getDrawable();
		AnimationDrawable mPeakTwoAnimation = (AnimationDrawable)peakTwo.getDrawable();

		Song nowPlaying = MusicUtils.getNowPlaying(context);
		if(nowPlaying != null && album.getName().equals(nowPlaying.getAlbum()) &&
				album.getArtist().equals(nowPlaying.getArtist())) {
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

}
