package com.afollestad.overhear.base;

import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.service.MusicService.MusicBinder;

import android.app.ListActivity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

/**
 * @author Aidan Follestad
 */
public abstract class OverhearListActivity extends ListActivity {

    private boolean mChangedConfig;
    private MusicService mService;

    public MusicService getService() {
    	return mService;
    }
    
    public abstract void onBound();
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null)
            mChangedConfig = savedInstanceState.getBoolean("changed_config", false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("changed_config", isChangingConfigurations());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mChangedConfig) {
        	Overhear.get(this).bind();
        	bindService(new Intent(this, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(!isChangingConfigurations()) {
            Overhear.get(this).unbind();
            //TODO make sure the connection is maintained on orientation change
            if(mService != null)
            	unbindService(mConnection);
        }
    }
    
    
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
        	MusicBinder binder = (MusicBinder)service;
            mService = binder.getService();
            onBound();
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        	mService = null;
        }
    };
}
