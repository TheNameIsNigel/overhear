package com.afollestad.overhear.adapters;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.overhearapi.Song;
import com.afollestad.overhearapi.Utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Handler;
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
	
	public static void loadArtistPicture(final Context context, final Artist artist, 
			final WeakReference<ImageView> view, final float widthDp, final float heightDp) {
			
		final Handler mHandler = new Handler();
		new Thread(new Runnable() {
			public void run() {
				
				File[] files = context.getExternalCacheDir().listFiles();
				boolean found = false;
				String artistFileName = artist.getName().replace(" ", "_") + ".jpg";
				
				for(File fi : files) {
					if(fi.getName().equals(artistFileName)) {
						final Bitmap loaded = Utils.loadImage(context, Uri.fromFile(fi), widthDp, heightDp);
						mHandler.post(new Runnable() {
							public void run() {
								if(view != null && view.get() != null) {
									view.get().setImageBitmap(loaded);
								}
							}
						});
						found = true;
						break;
					}
				}
				if(found) {
					return;
				}
				
				try {
					String url = LastFM.getArtistInfo(artist.getName()).getBioImageURL(); 
					final Bitmap loaded = Utils.loadImage(context, Uri.parse(url), widthDp, heightDp);
					loaded.compress(CompressFormat.JPEG, 100, new FileOutputStream(new File(
							context.getExternalCacheDir(), artistFileName)));
					mHandler.post(new Runnable() {
						public void run() {
							if(view != null && view.get() != null) {
								view.get().setImageBitmap(loaded);
							}
						}
					});
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Artist artist = Artist.fromCursor(cursor);
		((TextView)view.findViewById(R.id.title)).setText(artist.getName());
		final ImageView cover = (ImageView)view.findViewById(R.id.image);
		
		cover.setImageBitmap(null);
		loadArtistPicture(context, artist, new WeakReference<ImageView>(cover), 
				180f, 180f);
		
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