package com.afollestad.overhear.tasks;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ImageView;

import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.androidquery.AQuery;

public class LastfmGetArtistImage extends AsyncTask<Artist, Integer, String> {

    private String url = null;
    private final AQuery aq;
    private final WeakReference<Context> contextReference;
    private final WeakReference<ImageView> imageviewReference;
    private final ImageView mImageView;

    public LastfmGetArtistImage(Activity context, ImageView iv) {
        contextReference = new WeakReference<Context>(context);
        imageviewReference = new WeakReference<ImageView>(iv);
        mImageView = imageviewReference.get();
        aq = new AQuery(context, iv);
    }

    @Override
    protected String doInBackground(Artist... arts) {
        if (MusicUtils.isOnline(contextReference.get()) && arts[0] != null) {
            try {
                url = LastFM.getArtistInfo(arts[0].getName()).getBioImageURL();
                aq.cache(url, 0);
                MusicUtils.setImageURL(contextReference.get(), arts[0].getName(), url, ArtistAdapter.ARTIST_IMAGE);
                return url;
            } catch (Exception e) {
                return null;
            }
        } else {
            url = MusicUtils.getImageURL(contextReference.get(), arts[0].getName(), ArtistAdapter.ARTIST_IMAGE);
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
