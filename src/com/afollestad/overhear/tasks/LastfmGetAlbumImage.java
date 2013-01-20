package com.afollestad.overhear.tasks;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.LastFM;
import com.androidquery.AQuery;

public class LastfmGetAlbumImage extends AsyncTask<Album, Integer, String> {

    private String url = null;
    private final AQuery aq;
    private final WeakReference<Context> contextReference;
    private final WeakReference<ImageView> imageviewReference;
    private final ImageView mImageView;
   
    public LastfmGetAlbumImage(Activity context, ImageView iv) {
        contextReference = new WeakReference<Context>(context);
        imageviewReference = new WeakReference<ImageView>(iv);
        mImageView = imageviewReference.get();
        aq = new AQuery(context, iv);
    }

    @Override
    protected String doInBackground(Album... als) {
    	url = als[0].getAlbumArtUri(contextReference.get()).toString();
    	if(url == null) {
    		url = MusicUtils.getImageURL(contextReference.get(), als[0].getName() + ":" + 
            		als[0].getArtist().getName(), AlbumAdapter.ALBUM_IMAGE);
    		aq.cache(url, 0);
    	}
        if (url == null && MusicUtils.isOnline(contextReference.get())) {
            try {
                url = LastFM.getAlbumInfo(als[0].getArtist().getName(), als[0].getName()).getCoverImageURL();
                aq.cache(url, 0);
                MusicUtils.setImageURL(contextReference.get(), als[0].getName() + ":" + 
                		als[0].getArtist().getName(), url, AlbumAdapter.ALBUM_IMAGE);
                return url;
            } catch (Exception e) {
            	return als[0].getAlbumArtUri(contextReference.get()).toString();
            }
        }
        return url;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null && mImageView != null)
            new BitmapFromURL(mImageView).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, result);
        super.onPostExecute(result);
    }
}