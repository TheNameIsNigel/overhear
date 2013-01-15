package com.afollestad.overhear;

import java.util.ArrayList;

import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class MusicService extends Service {
	
	public MusicService() {
	}

	private MediaPlayer player;	
	private Song nowPlaying;
	private ArrayList<Album> recents;
	private boolean preparedPlayer;
	private final IBinder mBinder = new MusicBinder();
	
	public final static String PLAYING_STATE_CHANGED = "com.afollestad.overhear.PLAY_STATE_CHANGED";
	
	/**
	 * Requests that the service plays a song.
	 */
	public void playTrack(Song song) throws Exception {
		nowPlaying = song;
		MusicUtils.setLastPlaying(getApplicationContext(), song);
		if(player != null) {
			player.stop();
			player.release();
		}
		player = new MediaPlayer();
		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				//TODO verify that this works
				nowPlaying = null;
				sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			}
		});
		player.setDataSource(song.getData());
		player.prepare();
		preparedPlayer = true;
		player.start();
		appendRecent(Album.getAlbum(getApplicationContext(), song.getAlbum()));
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
	}
	
	/**
	 * Pauses the currently playing song.
	 */
	public void pauseTrack() {
		if(player != null && preparedPlayer && player.isPlaying()) {
			player.pause();
			MusicUtils.setLastPlaying(getApplicationContext(), nowPlaying);
			nowPlaying = null;
		}
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
	}
	
	/**
	 * Resumes the last playing song, or restart it if it's position was lost.
	 * @throws Exception
	 */
	public void resumeTrack() throws Exception {
		Song last = MusicUtils.getLastPlaying(getApplicationContext());
		if(preparedPlayer) {
			player.start();
			nowPlaying = last;
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
		} else if(last != null) {
			playTrack(last);
		}
	}
	
	/**
	 * Gets the currently playing song, if any.
	 */
	public Song getNowPlaying() {
		return nowPlaying;
	}
	
	/**
	 * Gets the recently played history.
	 * @return
	 */
	public ArrayList<Album> getRecents() {
		return recents;
	}
	
	/**
	 * Adds a song to the recent history.
	 */
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
	
	/**
	 * Saves the recent history to the local preferences for loading later.
	 */
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
		player = new MediaPlayer();
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
		
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		recents = MusicUtils.getRecents(getApplicationContext());
		return START_STICKY;
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		player.release();
	}
}