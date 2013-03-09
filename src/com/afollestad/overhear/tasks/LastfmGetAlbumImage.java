package com.afollestad.overhear.tasks;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.Overhear;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhear.utils.WebArtUtils;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.LastFM;

import java.io.FileNotFoundException;
import java.lang.ref.WeakReference;

/**
 * Handles retrieving art for albums from the cache, or last.fm if not available. 
 * 
 * @author Aidan Follestad
 */
public class LastfmGetAlbumImage extends AsyncTask<Album, Integer, String> {

    private WeakReference<Context> context;
    private WeakReference<AImageView> view;
    private WeakReference<Application> app;
    private boolean forceDownload;

    public LastfmGetAlbumImage(Context context, Application app, AImageView view, boolean forceDownload) {
        if(view != null) {
            if(view.getTag() != null)
                ((AsyncTask)view.getTag()).cancel(true);
            view.setTag(this);
        }
        this.context = new WeakReference<Context>(context);
        this.view = new WeakReference<AImageView>(view);
        this.app = new WeakReference<Application>(app);
        this.forceDownload = forceDownload;
    }

    @Override
    protected String doInBackground(Album... als) {
    	if(als.length == 0 || als[0] == null || als[0].getAlbumId() == -1 || context == null || context.get() == null) {
    		return null;
    	}
        String url = WebArtUtils.getImageURL(context.get(), als[0]);
        if (url == null && !forceDownload) {
            url = als[0].getAlbumArtUri(context.get()).toString();
            try {
                context.get().getContentResolver().openInputStream(Uri.parse(url));
            } catch (FileNotFoundException e) {
                url = null;
            }
        }
        if ((url == null || url.trim().isEmpty()) || (forceDownload || url.equals("flag:force_download"))) {
            if (MusicUtils.isOnline(context.get())) {
                try {
                    Log.i("Overhear", "Getting album information from LastFM for: " + als[0].getName() + " by " + als[0].getArtist());
                    url = LastFM.getAlbumInfo(als[0].getArtist().getName(), als[0].getName()).getCoverImageURL();
                    if (url != null && !url.trim().isEmpty())
                        WebArtUtils.setImageURL(context.get(), als[0], url);
                    return url;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (forceDownload) {
                WebArtUtils.setImageURL(context.get(), als[0], "flag:force_download");
                return "flag:force_download";
            }
        }
        return url;
    }

    @Override
    protected void onPostExecute(String result) {
        if (view == null || view.get() == null) {
            return;
        } else {
            if(view.get().getTag() != this) {
                return;
            }
            if(result != null && result.equals("flag:force_download")) {
            	Toast.makeText(context.get(), R.string.download_queued, Toast.LENGTH_SHORT).show();
                //Load the fallback image
                view.get().setManager(((Overhear)app.get()).getManager()).setSource(null).load();
            } else {
            	view.get().setManager(((Overhear)app.get()).getManager()).setSource(result).load();
            }
        }
        super.onPostExecute(result);
    }
}