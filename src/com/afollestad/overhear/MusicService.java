package com.afollestad.overhear;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class MusicService extends Service {
	
	public MusicService() {
	}

	private MediaPlayer player;
	private final IBinder mBinder = new MusicBinder();
	
	public MediaPlayer getPlayer() {
		return player;
	}
	
	public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
	
	@Override
	public void onCreate() {
		super.onCreate();
		player = new MediaPlayer();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
}
