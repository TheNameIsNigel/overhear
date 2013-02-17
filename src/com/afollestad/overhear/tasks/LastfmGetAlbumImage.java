package com.afollestad.overhear.tasks;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.App;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.WebArtUtils;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.LastFM;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

public class LastfmGetAlbumImage extends AsyncTask<Album, Integer, String> {

    private WeakReference<Context> context;
    private WeakReference<AImageView> view;
    private WeakReference<Application> app;
   
    public LastfmGetAlbumImage(Context context, Application app, AImageView view) {
        this.context = new WeakReference<Context>(context);
        this.view = new WeakReference<AImageView>(view);
        this.app = new WeakReference<Application>(app);
        if(view != null) {
            if(view.getTag() != null) {
                //TODO this may be causing issues (with cancelling tasks that should not be cancelled).
                ((LastfmGetAlbumImage)view.getTag()).cancel(true);
            }
            view.setTag(this);
        }
    }

    @Override
    protected String doInBackground(Album... als) {
        String url = WebArtUtils.getImageURL(context.get(), als[0]);
        if(url == null) {
            url = als[0].getAlbumArtUri(context.get()).toString();
            try {
                context.get().getContentResolver().openInputStream(Uri.parse(url));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                url = null;
            }
        }
        if ((url == null || url.trim().isEmpty()) && MusicUtils.isOnline(context.get())) {
            try {
                Log.i("Overhear", "Getting album information from LastFM for: " + als[0].getName() + " by " + als[0].getArtist());
                url = LastFM.getAlbumInfo(als[0].getArtist().getName(), als[0].getName()).getCoverImageURL();
                WebArtUtils.setImageURL(context.get(), als[0], url);
                return url;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    @Override
    protected void onPostExecute(String result) {
        if(view == null && view.get() == null) {
            return;
        } else if(view != null && view.get() != null) {
            if(view.get().getTag() != null && view.get().getTag() != this) {
                return;
            } else if(view != null && view.get() != null && result != null) {
                view.get().setManager(((App)app.get()).getManager()).setSource(result).load();
            }
        }
        super.onPostExecute(result);
    }
}