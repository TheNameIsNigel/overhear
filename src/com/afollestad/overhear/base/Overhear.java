package com.afollestad.overhear.base;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;
import com.afollestad.aimage.ImageManager;
import com.afollestad.overhear.R;
import com.afollestad.overhear.service.MusicService;

import java.io.File;

/**
 * Holds variables that need to be persisted when activities are killed. 
 * 
 * @author Aidan Follestad
 */
public class Overhear extends Application {

	/**
	 * This isn't used directly by any code in the app.
	 */
	public Overhear() { }
	
	public final static int DATABASE_VERSION = 3;
	
    private ImageManager manager;
    private int boundActivities;

    public ImageManager getManager() {
        if(manager == null) {
        	File cacheDir = getExternalCacheDir();
        	cacheDir.mkdirs();
            manager = new ImageManager(this, cacheDir, R.drawable.default_song_album);
        }
        return manager;
    }

    public void bind() {
        boundActivities++;
    }

    public void unbind() {
        boundActivities--;
        if(boundActivities == 0) {
            startService(new Intent(this, MusicService.class).setAction(MusicService.ACTION_CLEAR_NOTIFICATION));
        }
    }

    public static Overhear get(Activity context) {
        return (Overhear)context.getApplication();
    }

    public static Overhear get(Service context) {
        return (Overhear)context.getApplication();
    }
}
