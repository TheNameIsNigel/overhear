package com.afollestad.overhear;

import com.afollestad.overhear.MusicService.MusicBinder;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class MusicBoundActivity extends Activity {

	MusicService mService;
	boolean mBound;
	
	public MusicService getMusicService() {
		return mService;
	}
	
	public boolean isServiceBound() {
		return mBound;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		Intent intent = new Intent(this, MusicService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		unbindService(mConnection);
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
        	MusicBinder binder = (MusicBinder)service;
            mService = binder.getService();
            mBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };
}