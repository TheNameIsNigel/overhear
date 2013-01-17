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
    private final WeakReference<Context> contextReference;
    private final AQuery aquery;
    private final int albumart;
    private String url;
    private String key;

    public ArtistOrAlbumImage(Context context, ImageView iv, String key, int dimen) {
    	contextReference = new WeakReference<Context>(context);
    	aquery = new AQuery(context);
    	if(iv != null) {
    		imageViewReference = new WeakReference<ImageView>(iv);
    	} else {
    		imageViewReference = null;
    	}
    	this.key = key;
    	albumart = dimen;
    }

    @Override
    protected Bitmap doInBackground(String... args) {
    	url = MusicUtils.getImageURL(contextReference.get(), args[0], key);
        Bitmap toreturn = aquery.getCachedImage(url);
        if(albumart == -1) {
        	return toreturn;
        } else {
        	return MusicUtils.getResizedBitmap(toreturn, albumart, albumart);
        }
    }

    @Override
    protected void onPostExecute(Bitmap result) {
    	if(result != null) {
    		if (imageViewReference != null && imageViewReference.get() != null)
        		imageViewReference.get().setImageBitmap(result);
    	}
        super.onPostExecute(result);
    }
}
