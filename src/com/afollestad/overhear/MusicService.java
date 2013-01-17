package com.afollestad.overhear;

import java.util.ArrayList;

import com.afollestad.overhear.tasks.ArtistOrAlbumImage;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.media.RemoteControlClient.MetadataEditor;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class MusicService extends Service {

	public MusicService() {
	}

	private ArrayList<Song> recents;
	private ArrayList<Song> queue;
	private int queuePos;
	private boolean preparedPlayer;
	private final IBinder mBinder = new MusicBinder();
	private boolean hasAudioFocus;

	private MediaPlayer player;	
	private AudioManager mAudioManager;
	private RemoteControlClient mRemoteControlClient;
	
	private AudioManager.OnAudioFocusChangeListener afl = new AudioManager.OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			switch(focusChange) {
			case AudioManager.AUDIOFOCUS_LOSS:
				hasAudioFocus = false;
				mAudioManager.unregisterRemoteControlClient(mRemoteControlClient);
				pauseTrack();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				hasAudioFocus = false;
				player.setVolume(0.2f, 0.2f);
				break;
			case AudioManager.AUDIOFOCUS_GAIN:
				hasAudioFocus = true;
				player.setVolume(1.0f, 1.0f);
				resumeTrack();
				break;
			}
		}
	};

	public final static String PLAYING_STATE_CHANGED = "com.afollestad.overhear.PLAY_STATE_CHANGED";
	public static final String ACTION_TOGGLE_PLAYBACK = "com.afollestad.overhear.action.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY = "com.afollestad.overhear.action.PLAY";
	public static final String ACTION_PLAY_ALL = "com.afollestad.overhear.action.PLAY_ALL";
	public static final String ACTION_PAUSE = "com.afollestad.overhear.action.PAUSE";
	public static final String ACTION_STOP = "com.afollestad.overhear.action.STOP";
	public static final String ACTION_SKIP = "com.afollestad.overhear.action.SKIP";
	public static final String ACTION_REWIND = "com.afollestad.overhear.action.REWIND";


	private boolean requestAudioFocus() {
		if(mAudioManager == null) {
			mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		}
		if(hasAudioFocus) {
			return true;
		}
		int result = mAudioManager.requestAudioFocus(afl, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		boolean hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
		Log.i("OVERHEAR SERVICE", "requestAudioFocus() = " + hasFocus);
		return hasFocus;
	}

	private boolean initializeRemoteControl() {
		boolean focused = requestAudioFocus();
		if(focused) {
			Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			intent.setComponent(new ComponentName(getApplicationContext(), MediaButtonIntentReceiver.class));
			mRemoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0));
			mRemoteControlClient.setTransportControlFlags(
					RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
					RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
					RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
					RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS);
			mAudioManager.registerRemoteControlClient(mRemoteControlClient);
		}
		return focused;
	}

	private void updateRemoteControl(int state) {
		Song nowPlaying = MusicUtils.getNowPlaying(getApplicationContext());
		MetadataEditor metadataEditor = mRemoteControlClient
				.editMetadata(true)
				.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, nowPlaying.getArtist())
				.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, nowPlaying.getTitle())
				.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, nowPlaying.getDuration());
		Album album = Album.getAlbum(getApplicationContext(), nowPlaying.getAlbum());
		try {
			Bitmap art = new ArtistOrAlbumImage(getApplicationContext(), null, null, -1).executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR, album.getName() + ":" + album.getArtist().getName()).get();
			metadataEditor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, art);
		} catch(Exception e) {
			e.printStackTrace();
		}
		metadataEditor.apply();
		mRemoteControlClient.setPlaybackState(state);
	}

	private void initializeMediaPlayer(String source) {
		if(player != null) {
			if(player.isPlaying())
				player.stop();
			player.release();
		}
		player = new MediaPlayer();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				if(!nextTrack() && mAudioManager != null) {
					mAudioManager.abandonAudioFocus(afl);
				}
			}
		});
		player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				switch(what) {
				case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
					mp.release();
					Toast.makeText(getApplicationContext(), "Media server died", Toast.LENGTH_LONG).show();
					break;
				default:
					if(extra == MediaPlayer.MEDIA_ERROR_IO) {
						Toast.makeText(getApplicationContext(), "Media player IO error", Toast.LENGTH_LONG).show();
					} else if(extra == MediaPlayer.MEDIA_ERROR_MALFORMED || extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
						Toast.makeText(getApplicationContext(), "Media player malformed or supported error", Toast.LENGTH_LONG).show();
					} else if(extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
						Toast.makeText(getApplicationContext(), "Media player timed out", Toast.LENGTH_LONG).show();
					}
					break;
				}
				return true;
			}
		});
		try {
			player.setDataSource(source);
			player.prepare();
			preparedPlayer = true;
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void playTrack(Song song) {
		playTrack(song, false, false);
	}

	private void playTrack(Song song, boolean isFromQueue, boolean forward) {
		Log.i("OVERHEAR SERVICE", "playTrack(" + song.getData() + ")");
		if(!initializeRemoteControl()) {
			return;
		}
		if(isFromQueue) {
			if(forward)
				queuePos++;
			else
				queuePos--;
		}
		MusicUtils.setNowPlaying(getApplicationContext(), song);
		MusicUtils.setLastPlaying(getApplicationContext(), null);
		initializeMediaPlayer(song.getData());
		player.start();
		appendRecent(song);
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
		updateRemoteControl(RemoteControlClient.PLAYSTATE_PLAYING);
	}

	private void playAll(Song song) {
		Log.i("OVERHEAR SERVICE", "playTrack(" + song.getData() + ")");
		if(queue == null || queue.size() == 0 || !queue.get(0).getArtist().equals(song.getArtist()) ||
				!queue.get(0).getAlbum().equals(song.getAlbum())) {
			queue = Song.getAllFromAlbum(getApplicationContext(), song.getAlbum(), song.getArtist());
		}
		for(int i = 0; i < queue.size(); i++) {
			if(queue.get(i).getId() == song.getId()) {
				queuePos = i;
				break;
			}
		}
		Log.i("OVERHEAR SERVICE", "Queue size: " + queue.size());
		playTrack(queue.get(queuePos));
	}

	private void pauseTrack() {
		Log.i("OVERHEAR SERVICE", "pauseTrack()");
		Song nowPlaying = MusicUtils.getNowPlaying(getApplicationContext());
		if(player != null && preparedPlayer && player.isPlaying()) {
			player.pause();
		} else {
			stopTrack();
			return;
		}
		MusicUtils.setNowPlaying(getApplicationContext(), null);
		MusicUtils.setLastPlaying(getApplicationContext(), nowPlaying);
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
		mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
		mAudioManager.abandonAudioFocus(afl);
	}

	private void stopTrack() {
		Log.i("OVERHEAR SERVICE", "stopTrack()");
		Song nowPlaying = MusicUtils.getNowPlaying(getApplicationContext());
		if(player != null && preparedPlayer && player.isPlaying()) {
			player.stop();
			player.release();
		}
		MusicUtils.setNowPlaying(getApplicationContext(), null);
		MusicUtils.setLastPlaying(getApplicationContext(), nowPlaying);
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
		mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
		mAudioManager.abandonAudioFocus(afl);
		stopSelf();
	}

	private void resumeTrack() {
		Log.i("OVERHEAR SERVICE", "resumeTrack()");
		boolean focused = requestAudioFocus();
		if(!focused) {
			return;
		}
		Song last = MusicUtils.getLastPlaying(getApplicationContext());
		if(player != null && preparedPlayer) {
			if(!initializeRemoteControl()) {
				return;
			}
			player.start();
			MusicUtils.setNowPlaying(getApplicationContext(), last);
			MusicUtils.setLastPlaying(getApplicationContext(), null);
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);			
		} else if(last != null) {
			Log.i("OVERHEAR SERVICE", "No paused state found");
			playTrack(last);
		}
	}

	private boolean nextTrack() {
		Log.i("OVERHEAR SERVICE", "nextTrack()");
		mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_SKIPPING_FORWARDS);
		Song nowPlaying = MusicUtils.getNowPlaying(getApplicationContext()); 
		if((queuePos + 1) > (queue.size() - 1)) {
			MusicUtils.setNowPlaying(getApplicationContext(), null);
			MusicUtils.setLastPlaying(getApplicationContext(), nowPlaying);
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			return false;
		}
		Song queued = queue.get(queuePos + 1);
		if(queued == null) {
			MusicUtils.setNowPlaying(getApplicationContext(), null);
			MusicUtils.setLastPlaying(getApplicationContext(), nowPlaying);
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			return false;
		}
		playTrack(queued, true, true);
		return true;
	}

	private void previousTrack() {
		Log.i("OVERHEAR SERVICE", "previousTrack()");
		mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_SKIPPING_BACKWARDS);
		Song nowPlaying = MusicUtils.getNowPlaying(getApplicationContext());
		if((queuePos - 1) < 0) {
			MusicUtils.setNowPlaying(getApplicationContext(), null);
			MusicUtils.setLastPlaying(getApplicationContext(), nowPlaying);
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			return;
		}
		Song previous = queue.get(queuePos - 1);
		playTrack(previous, true, false);
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
		if(intent == null || intent.getAction() == null) {
			return START_STICKY;
		}
		String action = intent.getAction();
		if(action.equals(ACTION_PLAY)) {
			if(intent.hasExtra("song")) {
				playTrack(Song.fromJSON(intent.getStringExtra("song")));
			} else {
				resumeTrack();
			}
		} else if(action.equals(ACTION_PLAY_ALL)) {
			playAll(Song.fromJSON(intent.getStringExtra("song")));
		} else if(action.equals(ACTION_PAUSE)) {
			pauseTrack();
		} else if(action.equals(ACTION_SKIP)) {
			nextTrack();
		} else if(action.equals(ACTION_REWIND)) {
			previousTrack();
		} else if(action.equals(ACTION_STOP)) {
			stopTrack();
		} else if(action.equals(ACTION_TOGGLE_PLAYBACK)) {
			if(isPlaying()) {
				pauseTrack();
			} else {
				resumeTrack();
			}
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiver);
		player.release();
	}
}