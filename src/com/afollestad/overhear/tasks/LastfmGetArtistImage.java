package com.afollestad.overhear.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.App;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.WebArtUtils;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;

import java.lang.ref.WeakReference;

public class LastfmGetArtistImage extends AsyncTask<Artist, Integer, String> {

    private WeakReference<Activity> context;
    private WeakReference<AImageView> view;

    public LastfmGetArtistImage(Activity context, AImageView view) {
        this.context = new WeakReference<Activity>(context);
        this.view = new WeakReference<AImageView>(view);
    }

    @Override
    protected String doInBackground(Artist... arts) {
        String url = null;
        if (MusicUtils.isOnline(context.get()) && arts[0] != null) {
            try {
                Log.i("Overhear", "Getting artist information from LastFM for: " + arts[0].getName());
                url = LastFM.getArtistInfo(arts[0].getName()).getBioImageURL();
                WebArtUtils.setImageURL(context.get(), arts[0], url);
                return url;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    @Override
    protected void onPostExecute(String result) {
        if(view == null || view.get() == null) {
            return;
        } else if(result != null) {
            view.get().setManager(((App)context.get().getApplication()).getManager()).setSource(result).load();
        }
        super.onPostExecute(result);
    }
}
