package com.afollestad.overhear.tasks;

import java.lang.ref.WeakReference;

import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.overhearapi.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;

public class AlbumImageLoader extends AsyncTask<Album, Integer, Bitmap> {

	private final Context context;
	private final int width;
	private final int height;
	private final WeakReference<ImageView> imageViewReference;
	private final ImageView mImageView;

	private WeakReference<Bitmap> bitmapReference;

	public AlbumImageLoader(Context context, ImageView iv, int width, int height) {
		this.context = context;
		this.width = width;
		this.height = height;
		imageViewReference = new WeakReference<ImageView>(iv);
		mImageView = imageViewReference.get();
	}

	@Override
	protected Bitmap doInBackground(Album... params) {
		Bitmap local = params[0].getAlbumArt(context, width, height);
		if(local == null) {
			try {
				String url = LastFM.getAlbumInfo(params[0].getArtist().getName(), 
						params[0].getName()).getCoverImageURL();
				local = Utils.loadImage(context, Uri.parse(url), width, height);
				//TODO save locally
			} catch(Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		bitmapReference = new WeakReference<Bitmap>(local);
		return bitmapReference.get();
	}

	@Override
	protected void onPostExecute(Bitmap result) {
		if (result != null && mImageView != null)
			mImageView.setImageBitmap(result);
		super.onPostExecute(result);
	}
}
