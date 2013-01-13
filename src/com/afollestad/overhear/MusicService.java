package com.afollestad.overhear;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.afollestad.overhearapi.Album;
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
	private ArrayList<Album> recents;
	private boolean preparedPlayer;
	private final IBinder mBinder = new MusicBinder();
	private MusicActivityCallback mCallback;
	
	public void setCallback(MusicActivityCallback cb) {
		mCallback = cb;
	}
	
	public void playTrack(Song song) throws Exception {
		nowPlaying = song;
		MusicUtils.setLastPlaying(getApplicationContext(), song);
		if(player != null) {
			player.stop();
			player.release();
		}
		player = new MediaPlayer();
		player.setDataSource(song.getData());
		player.prepare();
		preparedPlayer = true;
		player.start();
		appendRecent(Album.getAlbum(getApplicationContext(), song.getAlbum()));
		if(mCallback != null)
			mCallback.onServiceUpdate();
	}
	
	public void pauseTrack() {
		if(player != null && preparedPlayer && player.isPlaying()) {
			player.pause();
			MusicUtils.setLastPlaying(getApplicationContext(), nowPlaying);
			nowPlaying = null;
		}
		if(mCallback != null)
			mCallback.onServiceUpdate();
	}
	
	public void resumeTrack() throws Exception {
		Song last = MusicUtils.getLastPlaying(getApplicationContext());
		if(preparedPlayer) {
			player.start();
			nowPlaying = last;
			if(mCallback != null)
				mCallback.onServiceUpdate();
		} else if(last != null) {
			playTrack(last);
		}
	}
	
	public Song getNowPlaying() {
		return nowPlaying;
	}
	
	public MediaPlayer getPlayer() {
		return player;
	}
	
	public ArrayList<Album> getRecents() {
		return recents;
	}
	
	public void appendRecent(Album album) {
		for(int i = 0; i < recents.size(); i++) {
			if(recents.get(i).getAlbumId() == album.getAlbumId()) {
				recents.remove(i);
			}
		}
		if(recents.size() == 10) {
			recents.remove(9);
		}
		recents.add(0, album);
	}
	
	public void saveRecents() {
		MusicUtils.setRecents(getApplicationContext(), recents);
	}
	
	public boolean isPlaying() {
		if(player != null && preparedPlayer) {
			return player.isPlaying();
		} else {
			return false;
		}
	}
	
	public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
		
	@Override
	public void onCreate() {
		super.onCreate();
		recents = MusicUtils.getRecents(getApplicationContext());
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

		public static void setRecents(Context context, ArrayList<Album> recents) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			try {
				JSONArray json = new JSONArray();
				for(Album album : recents) {
					json.put(album.getJSON());
				}
				prefs.edit().putString("recents", json.toString()).commit();
			} catch(Exception e) {
				throw new Error(e.getMessage());
			}
		}
		
		public static ArrayList<Album> getRecents(Context context) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			ArrayList<Album> recents = new ArrayList<Album>();
			if(!prefs.contains("recents")) {
				return recents;
			}
			try {
				JSONArray json = new JSONArray(prefs.getString("recents", null));
				for(int i = 0; i < json.length(); i++) {
					recents.add(Album.fromJSON(context, json.getJSONObject(i)));
				}
			} catch(Exception e) {
				throw new Error(e.getMessage());
			}
			return recents;
		}
	}
}
