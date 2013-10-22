package com.afollestad.overhear.base;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;
import com.afollestad.overhear.R;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.silk.images.SilkImageManager;

import java.io.File;

/**
 * Holds variables that need to be persisted when activities are killed.
 *
 * @author Aidan Follestad
 */
public class Overhear extends Application {

    public final static int DATABASE_VERSION = 3;
    private SilkImageManager manager;
    private int boundActivities;
    private Object LOCK = new Object();

    /**
     * This isn't used directly by any code in the app.
     */
    public Overhear() {
    }

    public static Overhear get(Activity context) {
        return (Overhear) context.getApplication();
    }

    public static Overhear get(Service context) {
        return (Overhear) context.getApplication();
    }

    public SilkImageManager getManager() {
        if (manager == null) {
            File cacheDir = getExternalCacheDir();
            cacheDir.mkdirs();
            manager = new SilkImageManager(this)
                    .setCacheDirectory(cacheDir)
                    .setFallbackImage(R.drawable.default_song_album);
        }
        return manager;
    }

    public void bind() {
        synchronized (LOCK) {
            boundActivities++;
        }
    }

    public void unbind() {
        synchronized (LOCK) {
            boundActivities--;
            if (boundActivities == 0) {
                sendBroadcast(new Intent(MusicService.ACTION_CLEAR_NOTIFICATION));
            }
        }
    }
}
