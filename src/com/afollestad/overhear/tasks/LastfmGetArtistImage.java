package com.afollestad.overhear.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.androidquery.AQuery;

import java.lang.ref.WeakReference;

public class LastfmGetArtistImage extends AsyncTask<Artist, Integer, String> {

    private WeakReference<Activity> context;
    private int viewId;
    private WeakReference<AQuery> aq;

    public LastfmGetArtistImage(Activity context, int viewId, AQuery aq) {
        this.context = new WeakReference<Activity>(context);
        this.viewId = viewId;
        this.aq = new WeakReference<AQuery>(aq);
    }

    @Override
    protected String doInBackground(Artist... arts) {
        String url = null;
        if (MusicUtils.isOnline(context.get()) && arts[0] != null) {
            try {
                Log.i("Overhear", "Getting artist information from LastFM for: " + arts[0].getName());
                url = LastFM.getArtistInfo(arts[0].getName()).getBioImageURL();
                MusicUtils.setImageURL(context.get(), arts[0].getName(), url, ArtistAdapter.ARTIST_IMAGE);
                return url;
            } catch (Exception e) {
                return null;
            }
        } else {
            url = MusicUtils.getImageURL(context.get(), arts[0].getName(), ArtistAdapter.ARTIST_IMAGE);
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
