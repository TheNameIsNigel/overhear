package com.afollestad.overhear.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.LastFM;
import com.androidquery.AQuery;

import java.lang.ref.WeakReference;

public class LastfmGetAlbumImage extends AsyncTask<Album, Integer, String> {

    private WeakReference<Activity> context;
    private int viewId;
    private WeakReference<AQuery> aq;
   
    public LastfmGetAlbumImage(Activity context, int viewId, AQuery aq) {
        this.context = new WeakReference<Activity>(context);
        this.viewId = viewId;
        this.aq = new WeakReference<AQuery>(aq);
    }

    @Override
    protected String doInBackground(Album... als) {
        String url = MusicUtils.getImageURL(context.get(), als[0].getName() + ":" +
            	als[0].getArtist().getName(), AlbumAdapter.ALBUM_IMAGE);
        if (url == null && MusicUtils.isOnline(context.get())) {
            try {
                Log.i("Overhear", "Getting album information from LastFM for: " + als[0].getName() + " by " + als[0].getArtist());
                url = LastFM.getAlbumInfo(als[0].getArtist().getName(), als[0].getName()).getCoverImageURL();
                MusicUtils.setImageURL(context.get(), als[0].getName() + ":" +
                        als[0].getArtist().getName(), url, AlbumAdapter.ALBUM_IMAGE);
                return url;
            } catch (Exception e) {
            	return als[0].getAlbumArtUri(context.get()).toString();
            }
        }
        if(url == null) {
        	url = als[0].getAlbumArtUri(context.get()).toString();
        }
        return url;
    }

    @Override
    protected void onPostExecute(String result) {
        if(viewId > 0 && aq != null && aq.get() != null)
            aq.get().id(viewId).image(result, true, true);
        super.onPostExecute(result);
    }
}