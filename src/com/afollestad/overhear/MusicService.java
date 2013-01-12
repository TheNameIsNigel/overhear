package com.afollestad.overhear;

import com.afollestad.overhearapi.Song;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class MusicService extends Service {
	
	public MusicService() {
	}
	
	public static interface MusicActivityCallback {
		public abstract void onServiceUpdate();
	}

	private MediaPlayer player;
	private boolean preparedPlayer;
	private final IBinder mBinder = new MusicBinder();
	private MusicActivityCallback mCallback;
		
	public void playTrack(Context context, Song song) throws Exception {
		MusicUtils.setNowPlaying(context, song);
		MusicUtils.setLastPlaying(context, null);
		if(player != null) {
			player.stop();
			player.release();
		}
		player = new MediaPlayer();
		player.setDataSource(song.getData());
		player.prepare();
		preparedPlayer = true;
		player.start();
		if(mCallback != null)
			mCallback.onServiceUpdate();
	}
	
	public void pauseTrack(Context context) {
		Song playing = MusicUtils.getNowPlaying(context);
		if(player != null && preparedPlayer && player.isPlaying()) {
			player.pause();
			MusicUtils.setNowPlaying(context, null);
			MusicUtils.setLastPlaying(context, playing);
		}
		if(mCallback != null)
			mCallback.onServiceUpdate();
	}
	
	public void resumeTrack(Context context) throws Exception {
		if(preparedPlayer) {
			player.start();
			Song last = MusicUtils.getLastPlaying(context);
			MusicUtils.setNowPlaying(context, last);
			MusicUtils.setLastPlaying(context, null);
		} else {
			Song last = MusicUtils.getLastPlaying(context);
			if(last != null)
				playTrack(context, last);
		}
		if(mCallback != null)
			mCallback.onServiceUpdate();
	}
	
	public MediaPlayer getPlayer() {
		return player;
	}
	
	public boolean isPlaying() {
		if(player != null && preparedPlayer) {
			return player.isPlaying();
		} else {
			return false;
		}
	}
	
	public class MusicBinder extends Binder {
        MusicService getService(MusicActivityCallback cb) {
        	mCallback = cb;
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
		player.release();
	}
}
