package com.afollestad.overhear.base;

import android.app.Application;
import com.afollestad.aimage.ImageManager;

public class App extends Application {

    private ImageManager manager;

    public ImageManager getManager() {
        if(manager == null)
            manager = new ImageManager(this);
        return manager;
    }
}
