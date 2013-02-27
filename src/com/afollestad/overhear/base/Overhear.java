package com.afollestad.overhear.base;

import java.io.File;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;

import com.afollestad.aimage.ImageManager;
import com.afollestad.overhear.service.MusicService;

public class Overhear extends Application {

	/**
	 * This isn't used directly by any code in the app.
	 */
	public Overhear() { }
	
    private ImageManager manager;
    private int boundActivities;

    public ImageManager getManager() {
        if(manager == null) {
        	File cacheDir = new File(Environment.getExternalStorageDirectory(), "Overhear");
        	cacheDir.mkdirs();
            manager = new ImageManager(this, cacheDir);
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
