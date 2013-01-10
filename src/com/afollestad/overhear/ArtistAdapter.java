package com.afollestad.overhear;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.overhearapi.Utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Handler;
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
	}

	private Context context;
	private Artist[] items;

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
	public View getView(final int position, View convertView, ViewGroup parent) {
		RelativeLayout view;
		if (convertView == null) {
			view = (RelativeLayout)LayoutInflater.from(context).inflate(R.layout.grid_view_item, null);
		} else {
			view = (RelativeLayout)convertView;
		}
				
		Artist artist = items[position];
		((TextView)view.findViewById(R.id.title)).setText(artist.getName());
		final ImageView cover = (ImageView)view.findViewById(R.id.image);
		cover.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Artist artist = items[position];
				context.startActivity(new Intent(context, ArtistViewer.class)
						.putExtra("artist", artist.getJSON().toString()));
			}
		});

		cover.setImageBitmap(null);
		loadArtistPicture(context, artist, new WeakReference<ImageView>((ImageView)view.findViewById(R.id.image)), 
				180f, 180f);
		
		return view;
	}
}