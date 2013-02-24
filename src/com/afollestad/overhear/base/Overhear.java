package com.afollestad.overhear.base;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;
import com.afollestad.aimage.ImageManager;
import com.afollestad.overhear.service.MusicService;

public class Overhear extends Application {

    private ImageManager manager;
    private int boundActivities;

    public ImageManager getManager() {
        if(manager == null)
            manager = new ImageManager(this);
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
