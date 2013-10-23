package com.afollestad.overhear.base;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.service.MusicService.MusicBinder;

/**
 * The base of list activities, used for convenience (handles common functions that every
 * activity uses, reducing the amount of code and complexity among activity Java files).
 *
 * @author Aidan Follestad
 */
public abstract class OverhearListActivity extends ListActivity {

    private boolean mChangedConfig;
    private MusicService mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicBinder binder = (MusicBinder) service;
            mService = binder.getService();
            onBound();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;
        }
    };

    public MusicService getService() {
        return mService;
    }

    public abstract void onBound();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null)
            mChangedConfig = savedInstanceState.getBoolean("changed_config", false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("changed_config", isChangingConfigurations());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        bindService(new Intent(this, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mChangedConfig) {
            Overhear.get(this).bind();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (!isChangingConfigurations()) {
            Overhear.get(this).unbind();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mService != null)
            unbindService(mConnection);
    }
}
