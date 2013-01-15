package com.afollestad.overhear.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;

import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.overhearapi.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;

public class ArtistImageLoader extends AsyncTask<Artist, Integer, Bitmap> {

	private final Context context;
	private final int width;
	private final int height;
	private final WeakReference<ImageView> imageViewReference;
	private final ImageView mImageView;

	private WeakReference<Bitmap> bitmapReference;

	public ArtistImageLoader(Context context, ImageView iv, int width, int height) {
		this.context = context;
		this.width = width;
		this.height = height;
		imageViewReference = new WeakReference<ImageView>(iv);
		mImageView = imageViewReference.get();
	}

	private Bitmap getCachedImage(Artist artist) {
		File[] files = context.getExternalCacheDir().listFiles();
		String artistFileName = artist.getName().replace(" ", "_") + ".jpg";
		for(File fi : files) {
			if(fi.getName().equals(artistFileName)) {
				return Utils.loadImage(context, Uri.fromFile(fi), width, height);
			}
		}
		return null;
	}

	@Override
	protected Bitmap doInBackground(Artist... params) {
		Bitmap cached = getCachedImage(params[0]);
		if(cached == null) {
			try {
				String artistFileName = params[0].getName().replace(" ", "_") + ".jpg";
				String url = LastFM.getArtistInfo(params[0].getName()).getBioImageURL(); 
				cached = Utils.loadImage(context, Uri.parse(url), width, height);
				cached.compress(CompressFormat.JPEG, 100, new FileOutputStream(new File(
						context.getExternalCacheDir(), artistFileName)));
			} catch(Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		bitmapReference = new WeakReference<Bitmap>(cached);
		return bitmapReference.get();
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		if (result != null && mImageView != null)
			mImageView.setImageBitmap(result);
		super.onPostExecute(result);
	}
}
