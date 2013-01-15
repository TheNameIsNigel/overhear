package com.afollestad.overhear.adapters;

import java.lang.ref.WeakReference;
import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;
import com.afollestad.overhearapi.Utils;

import android.app.ActivityManager;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AlbumAdapter extends CursorAdapter {

	public AlbumAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
		this.musicContext = (MusicBoundActivity)context;
		final int memClass = ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		final int cacheSize = 1024 * 1024 * memClass / 8;
		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getByteCount();
			}
		};
		mHandler = new Handler();
	}

	private MusicBoundActivity musicContext;
	private Handler mHandler;
	private LruCache<String, Bitmap> mMemoryCache;

	private void loadAlbumCover(final Album album, final WeakReference<ImageView> view) {
		new Thread(new Runnable() {
			public void run() {
				final Bitmap image = album.getAlbumArt(musicContext, 75f, 75f);
				if(image != null)
					mMemoryCache.put(album.getAlbumId() + "", image);
				mHandler.post(new Runnable() {
					public void run() {
						if(view.get() != null)
							view.get().setImageBitmap(image);
					}
				});
			}
		}).start();
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.album_item, null);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		int pad = Utils.convertDpToPx(context, 15f);
		int position = cursor.getPosition();
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

		if(mMemoryCache.get(album.getAlbumId() + "") != null) {
			final Bitmap image = mMemoryCache.get(album.getAlbumId() + "");
			cover.setImageBitmap(image);
		} else {
			cover.setImageBitmap(null);
			loadAlbumCover(album, new WeakReference<ImageView>((ImageView)view.findViewById(R.id.image)));
		}

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