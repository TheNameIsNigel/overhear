package com.afollestad.overhear;

import java.util.ArrayList;
import com.afollestad.overhearapi.Song;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

public class MusicService extends Service {

	public MusicService() {
	}

	private MediaPlayer player;	
	private Song nowPlaying;
	private ArrayList<Song> recents;
	private ArrayList<Song> queue;
	private int queuePos;
	private boolean preparedPlayer;
	private final IBinder mBinder = new MusicBinder();

	public final static String PLAYING_STATE_CHANGED = "com.afollestad.overhear.PLAY_STATE_CHANGED";

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
				nextTrack();
			}
		});
		player.setDataSource(song.getData());
		player.prepare();
		preparedPlayer = true;
		player.start();
		appendRecent(song);
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
	}

	public void playAll(int position, ArrayList<Song> playlist) throws Exception {
		queue = playlist;
		queuePos = position;
        playTrack(queue.get(position));
    }
	
	public void pauseTrack() {
		if(player != null && preparedPlayer && player.isPlaying()) {
			player.pause();
			MusicUtils.setLastPlaying(getApplicationContext(), nowPlaying);
			nowPlaying = null;
		}
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
	}
	
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

	public void previousTrack() {
		queuePos--;
		if(queuePos < 0) {
			nowPlaying = null;
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			return;
		}
		Song previous = queue.get(queuePos);
		try {
			playTrack(previous);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void nextTrack() {
		queuePos++;
		if(queuePos > queue.size()) {
			nowPlaying = null;
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			return;
		}
		Song queued = queue.get(queuePos);
		if(queued == null) {
			nowPlaying = null;
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			return;
		}
		try {
			playTrack(queued);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Song getNowPlaying() {
		return nowPlaying;
	}

	public ArrayList<Song> getRecents() {
		return recents;
	}

	private void appendRecent(Song song) {
		for(int i = 0; i < recents.size(); i++) {
			if(recents.get(i).getId() == song.getId()) {
				recents.remove(i);
			}
		}
		if(recents.size() == 10) {
			recents.remove(9);
		}
		recents.add(0, song);
	}

	public void clearQueue() {
		queue.clear();
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

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
				pauseTrack();
			}
		}
	};

	
	@Override
	public void onCreate() {
		super.onCreate();
		registerReceiver(receiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
		player = new MediaPlayer();
		queue = new ArrayList<Song>();	
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
		unregisterReceiver(receiver);
		player.release();
	}
}