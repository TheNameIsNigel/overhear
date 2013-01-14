package com.afollestad.overhear.adapters;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.R;
import com.afollestad.overhear.R.anim;
import com.afollestad.overhear.R.id;
import com.afollestad.overhear.R.layout;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Genre;
import com.afollestad.overhearapi.LoadedCallback;
import com.afollestad.overhearapi.Song;
import com.afollestad.overhearapi.Utils;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class AlbumAdapter extends BaseAdapter {

	public AlbumAdapter(MusicBoundActivity context, String artist) {
		this.context = context;
		final int memClass = ((ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
		// Use 1/8th of the available memory for this memory cache.
		final int cacheSize = 1024 * 1024 * memClass / 8;
		mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				// The cache size will be measured in bytes rather than number of items.
				return bitmap.getByteCount();
			}
		};
		mHandler = new Handler();
		this.artist = artist;
	}

	private MusicBoundActivity context;
	private Handler mHandler;
	private Album[] items;
	private LruCache<String, Bitmap> mMemoryCache;
	private String artist;

	private AnimationDrawable mPeakOneAnimation, mPeakTwoAnimation;

	@Override
	public int getCount() {
		if(items == null)
			return 0;
		return items.length;
	}

	@Override
	public Object getItem(int index) {
		return items[index];
	}

	@Override
	public long getItemId(int index) {
		return items[index].getAlbumId();
	}

	public void loadAlbums() {
		if(artist != null) {
			Album.getAlbumsForArtist(context, artist, new LoadedCallback<Album[]>() {
				@Override
				public void onLoaded(Album[] result) {
					items = result;
					notifyDataSetChanged();
				}	
			});
		} else { 
			Album.getAllAlbums(context, new LoadedCallback<Album[]>() {
				@Override
				public void onLoaded(Album[] result) {
					items = result;
					notifyDataSetChanged();
				}
			});
		}
	}

	public void loadRecents() {
		if(context.getMusicService() == null) 
			return;
		items = context.getMusicService().getRecents().toArray(new Album[0]);
		super.notifyDataSetChanged();
	}
	
	public void loadGenreAsync(final Genre genre, final LoadedCallback<Object> cb) {
		new Thread(new Runnable() {
			public void run() {
				List<Song> songs = genre.getAllSongs(context);
				final List<Album> albums = new ArrayList<Album>();
				for(Song s : songs) {
					albums.add(Album.getAlbum(context, s.getAlbum()));
				}
				mHandler.post(new Runnable() {
					public void run() {
						items = albums.toArray(new Album[0]);
						notifyDataSetChanged();
						if(cb != null)
							cb.onLoaded(null);
					}
				});
			}
		}).start();
	}

	private void loadAlbumCover(final Album album, final WeakReference<ImageView> view) {
		new Thread(new Runnable() {
			public void run() {
				final Bitmap image = album.getAlbumArt(context, 75f, 75f);
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
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout view;
		if (convertView == null) {
			view = (RelativeLayout)LayoutInflater.from(context).inflate(R.layout.album_item, null);
		} else {
			view = (RelativeLayout)convertView;
		}

		int pad = Utils.convertDpToPx(context, 15f);
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

		Album album = items[position];
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
		mPeakOneAnimation = (AnimationDrawable)peakOne.getDrawable();
		mPeakTwoAnimation = (AnimationDrawable)peakTwo.getDrawable();

		if(context.getMusicService() != null) {
			Song nowPlaying = context.getMusicService().getNowPlaying();
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

		return view;
	}
}