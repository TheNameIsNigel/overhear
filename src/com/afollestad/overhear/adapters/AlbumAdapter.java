package com.afollestad.overhear.adapters;

import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.tasks.ArtistOrAlbumImage;
import com.afollestad.overhear.tasks.LastfmGetAlbumImage;
import com.afollestad.overhearapi.Album;
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

public class AlbumAdapter extends SimpleCursorAdapter {

	public AlbumAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		this.context = (Activity)context;
	}

	private Activity context;
	public final static String ALBUM_IMAGE = "album_image";

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.album_item, null);
	}

	public static void startAlbumArtTask(Activity context, Album album, ImageView cover, int dimen) {
		if (MusicUtils.getImageURL(context, album.getName() + ":" + album.getArtist().getName(), ALBUM_IMAGE) == null) {
			new LastfmGetAlbumImage(context, cover).executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR, album);
		} else {
			if(dimen == 0) {
				dimen = context.getResources().getDimensionPixelSize(R.dimen.album_list_cover);
			}
			//dimen = -1;
			new ArtistOrAlbumImage(context, cover, ALBUM_IMAGE, dimen).executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR, album.getName() + ":" + album.getArtist().getName());
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

		Album album = Album.fromCursor(context, getCursor());
		((TextView)view.findViewById(R.id.title)).setText(album.getName());
		((TextView)view.findViewById(R.id.artist)).setText(album.getArtist().getName());
		final ImageView cover = (ImageView)view.findViewById(R.id.image); 

		final AQuery aq = new AQuery(view);
		if (aq.shouldDelay(position, view, parent, "")) {
			cover.setImageDrawable(null);
		} else {
			startAlbumArtTask(context, album, cover, 0);
		}

		ImageView peakOne = (ImageView)view.findViewById(R.id.peak_one);
		ImageView peakTwo = (ImageView)view.findViewById(R.id.peak_two);
		peakOne.setImageResource(R.anim.peak_meter_1);
		peakTwo.setImageResource(R.anim.peak_meter_2);
		AnimationDrawable mPeakOneAnimation = (AnimationDrawable)peakOne.getDrawable();
		AnimationDrawable mPeakTwoAnimation = (AnimationDrawable)peakTwo.getDrawable();

		Song focused = Queue.getFocused(context);
		if(focused != null && album.getName().equals(focused.getAlbum()) &&
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
}