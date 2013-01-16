package com.afollestad.overhear.tasks;

import java.io.InputStream;
import java.lang.ref.WeakReference;

import com.afollestad.overhear.MusicUtils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.ImageView;

public class BitmapFromURL extends AsyncTask<String, Integer, Bitmap> {

    private final WeakReference<ImageView> imageViewReference;
    private final ImageView mImageView;
    private WeakReference<Bitmap> bitmapReference;

    public BitmapFromURL(ImageView iv) {
        imageViewReference = new WeakReference<ImageView>(iv);
        mImageView = imageViewReference.get();
    }

    @Override
    protected Bitmap doInBackground(String... params) {
    	if(params[0].contains("content")) {
    		InputStream input = null;
    	    try {
    	        input = mImageView.getContext().getContentResolver().openInputStream(Uri.parse(params[0]));
    	        if (input == null)
    	            return null;
    	        bitmapReference = new WeakReference<Bitmap>(BitmapFactory.decodeStream(input));
    	        input.close();
    	    } catch(Exception e) {
    	    	e.printStackTrace();
    	    }
    	} else {
    		bitmapReference = new WeakReference<Bitmap>(MusicUtils.getBitmapFromURL(params[0]));
    	}
        return bitmapReference.get();
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null && mImageView != null)
            mImageView.setImageBitmap(result);
        super.onPostExecute(result);
    }
}
