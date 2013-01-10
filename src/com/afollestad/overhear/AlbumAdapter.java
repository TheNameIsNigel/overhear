package com.afollestad.overhear;

import java.lang.ref.WeakReference;
import com.afollestad.overhearapi.Album;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
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

	public AlbumAdapter(Context context, String artist) {
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

	private Context context;
	private Handler mHandler;
	private Album[] items;
	private LruCache<String, Bitmap> mMemoryCache;
	private String artist;

	@Override
	public int getCount() {
		if(items == null)
			return 0;
		else
			return items.length;
	}

	@Override
	public Object getItem(int index) {
		if(items == null)
			return null;
		return items[index];
	}

	@Override
	public long getItemId(int index) {
		return items[index].getAlbumId();
	}

	@Override
	public void notifyDataSetChanged() {
		if(artist != null)
			items = Album.getAlbumsForArtist(context, artist).toArray(new Album[0]);
		else 
			items = Album.getAllAlbums(context).toArray(new Album[0]);
		super.notifyDataSetChanged();
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
		
		Album album = items[position];
		((TextView)view.findViewById(R.id.title)).setText(album.getName());
		((TextView)view.findViewById(R.id.artist)).setText(album.getArtist(context).getName());
		final ImageView cover = (ImageView)view.findViewById(R.id.image); 
		
		if(mMemoryCache.get(album.getAlbumId() + "") != null) {
			final Bitmap image = mMemoryCache.get(album.getAlbumId() + "");
			cover.setImageBitmap(image);
			
		} else {
			cover.setImageBitmap(null);
			loadAlbumCover(album, new WeakReference<ImageView>((ImageView)view.findViewById(R.id.image)));
		}
		return view;
	}
}