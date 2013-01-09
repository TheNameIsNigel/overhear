package com.afollestad.overhear;

import java.lang.ref.WeakReference;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;

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

public class ArtistAdapter extends BaseAdapter {

	public ArtistAdapter(Context context) {
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
	}

	private Context context;
	private Handler mHandler;
	private Artist[] items;
	private LruCache<String, Bitmap> mMemoryCache;

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
		return items[index].getId();
	}

	@Override
	public void notifyDataSetChanged() {
		items = Artist.getAllArtists(context).toArray(new Artist[0]);
		super.notifyDataSetChanged();
	}

	public void loadArtistPicture(final Artist artist, final WeakReference<ImageView> view) {
		new Thread(new Runnable() {
			public void run() {
				Bitmap image = null;
				try {
					image = LastFM.getArtistInfo(artist.getName()).getBioImage(context, 180f, 180f, true);
				} catch(Exception e) {
					e.printStackTrace();
				}
				if(image != null) {
					mMemoryCache.put(artist.getId() + "", image);
					final Bitmap fImage = image;
					mHandler.post(new Runnable() {
						public void run() {
							if(view != null && view.get() != null)
								view.get().setImageBitmap(fImage);
						}
					});
				}
			}
		}).start();
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout view;
		if (convertView == null) {
			view = (RelativeLayout)LayoutInflater.from(context).inflate(R.layout.grid_view_item, null);
		} else {
			view = (RelativeLayout)convertView;
		}
				
		Artist artist = items[position];
		((TextView)view.findViewById(R.id.title)).setText(artist.getName());
		final ImageView cover = (ImageView)view.findViewById(R.id.image); 

		if(mMemoryCache.get(artist.getId() + "") != null) {
			final Bitmap image = mMemoryCache.get(artist.getId() + "");
			cover.setImageBitmap(image);	
		} else {
			cover.setImageBitmap(null);
			loadArtistPicture(artist, new WeakReference<ImageView>((ImageView)view.findViewById(R.id.image)));
		}
		return view;
	}
}