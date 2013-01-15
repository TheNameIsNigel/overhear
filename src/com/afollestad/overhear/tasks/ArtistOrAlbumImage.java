package com.afollestad.overhear.tasks;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.afollestad.overhear.MusicUtils;
import com.androidquery.AQuery;

public class ArtistOrAlbumImage extends AsyncTask<String, Integer, Bitmap> {

    private final WeakReference<ImageView> imageViewReference;
    private final Context mContext;
    private final AQuery aquery;
    private final ImageView mImageView;
    private final int albumart;
    private final WeakReference<Context> contextReference;
    private String url;
    private String key;
    private WeakReference<Bitmap> bitmapReference;

    public ArtistOrAlbumImage(ImageView iv, String key, int dimen) {
        contextReference = new WeakReference<Context>(iv.getContext());
        mContext = contextReference.get();
        imageViewReference = new WeakReference<ImageView>(iv);
        mImageView = imageViewReference.get();
        aquery = new AQuery(mContext);
        albumart = dimen;
        this.key = key;
    }

    @Override
    protected Bitmap doInBackground(String... args) {
    	url = MusicUtils.getImageURL(mContext, args[0], key);
        bitmapReference = new WeakReference<Bitmap>(aquery.getCachedImage(url));
        if(albumart == -1) {
        	return bitmapReference.get();
        } else {
        	return MusicUtils.getResizedBitmap(bitmapReference.get(), albumart, albumart);
        }
    }

    @Override
    protected void onPostExecute(Bitmap result) {
    	if (result != null && mImageView != null)
    		mImageView.setImageBitmap(result);
        super.onPostExecute(result);
    }
}
