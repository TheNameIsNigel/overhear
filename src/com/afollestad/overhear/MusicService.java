package com.afollestad.overhear;

import org.json.JSONObject;

import com.afollestad.overhearapi.Song;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

public class MusicService extends Service {
	
	public MusicService() {
	}
	
	public static interface MusicActivityCallback {
		public abstract void onServiceUpdate();
	}

	private MediaPlayer player;
	private Song nowPlaying;
	private boolean preparedPlayer;
	private final IBinder mBinder = new MusicBinder();
	private MusicActivityCallback mCallback;
		
	public void playTrack(Context context, Song song) throws Exception {
		nowPlaying = song;
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
		if(player != null && preparedPlayer && player.isPlaying()) {
			player.pause();
			MusicUtils.setLastPlaying(context, nowPlaying);
			nowPlaying = null;
		}
		if(mCallback != null)
			mCallback.onServiceUpdate();
	}
	
	public void resumeTrack(Context context) throws Exception {
		if(preparedPlayer) {
			player.start();
			Song last = MusicUtils.getLastPlaying(context);
			nowPlaying = last;
			MusicUtils.setLastPlaying(context, null);
		} else {
			Song last = MusicUtils.getLastPlaying(context);
			if(last != null)
				playTrack(context, last);
		}
		if(mCallback != null)
			mCallback.onServiceUpdate();
	}
	
	public Song getNowPlaying() {
		return nowPlaying;
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
	
	
	public static class MusicUtils {
				
		public static void setLastPlaying(Context context, Song song) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if(song == null) {
				prefs.edit().remove("last_playing").commit();
			} else { 
				prefs.edit().putString("last_playing", song.getJSON().toString()).commit();
			}
		}
		
		public static Song getLastPlaying(Context context) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			if(!prefs.contains("last_playing")) {
				return null;
			}
			try {
				return Song.fromJSON(new JSONObject(prefs.getString("last_playing", null)));
			} catch(Exception e) {
				throw new Error(e.getMessage());
			}
		}
	}
}
