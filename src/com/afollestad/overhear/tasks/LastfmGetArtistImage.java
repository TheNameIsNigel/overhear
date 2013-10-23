package com.afollestad.overhear.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.afollestad.overhear.base.Overhear;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhear.utils.WebArtUtils;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.silk.views.image.SilkImageView;

import java.lang.ref.WeakReference;

/**
 * Handles retrieving art for artists from the cache, or last.fm if not available.
 *
 * @author Aidan Follestad
 */
public class LastfmGetArtistImage extends AsyncTask<Artist, Integer, String> {

    private final WeakReference<Activity> context;
    private final WeakReference<SilkImageView> view;

    @SuppressWarnings("rawtypes")
    public LastfmGetArtistImage(Activity context, SilkImageView view) {
        if (view != null) {
            if (view.getTag() != null)
                ((AsyncTask) view.getTag()).cancel(true);
            view.setTag(this);
        }
        this.context = new WeakReference<Activity>(context);
        this.view = new WeakReference<SilkImageView>(view);
    }

    @Override
    protected String doInBackground(Artist... arts) {
        if (arts.length == 0 || arts[0] == null || context == null || context.get() == null) {
            return null;
        }
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
        if (view == null || view.get() == null) return;
        else {
            if (view.get().getTag() != this) return;
            view.get().setImageURL(Overhear.get(context.get()).getManager(), result);
        }
        super.onPostExecute(result);
    }
}
