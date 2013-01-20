package com.afollestad.overhear.service;

import java.util.ArrayList;

import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.Recents;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;
import com.androidquery.AQuery;

import android.app.Notification;
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
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class MusicService extends Service {

	public MusicService() {
	}

	private boolean preparedPlayer;
	private final IBinder mBinder = new MusicBinder();
	private boolean hasAudioFocus;
	private boolean wasPlayingBeforeLoss;
	private Toast toast;

	private Notification status;
	private static MediaPlayer player;	
	private AudioManager audioManager;
	private RemoteControlClient mRemoteControlClient;
	
	public AudioManager getAudioManager() {
		if(audioManager == null) {
			audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		}
		return audioManager;
	}
	public static MediaPlayer getPlayer() {
		return player;
	}
	
	private AudioManager.OnAudioFocusChangeListener afl = new AudioManager.OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			switch(focusChange) {
			case AudioManager.AUDIOFOCUS_LOSS:
				hasAudioFocus = false;
				getAudioManager().unregisterRemoteControlClient(mRemoteControlClient);
				getAudioManager().unregisterMediaButtonEventReceiver(new ComponentName(getApplicationContext(), MediaButtonIntentReceiver.class));
				wasPlayingBeforeLoss = isPlaying(); 
				pauseTrack();
				break;
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				hasAudioFocus = false;
				player.setVolume(0.2f, 0.2f);
				break;
			case AudioManager.AUDIOFOCUS_GAIN:
				hasAudioFocus = true;
				player.setVolume(1.0f, 1.0f);
				if(wasPlayingBeforeLoss)
					resumeTrack();
				break;
			}
		}
	};

	public final static String PLAYING_STATE_CHANGED = "com.afollestad.overhear.PLAY_STATE_CHANGED";
	public final static String RECENTS_UPDATED = "com.afollestad.overhear.RECENTS_UPDATED";

	public static final String ACTION_TOGGLE_PLAYBACK = "com.afollestad.overhear.action.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY = "com.afollestad.overhear.action.PLAY";
	public static final String ACTION_PLAY_ALL = "com.afollestad.overhear.action.PLAY_ALL";
	public static final String ACTION_PAUSE = "com.afollestad.overhear.action.PAUSE";
	public static final String ACTION_STOP = "com.afollestad.overhear.action.STOP";
	public static final String ACTION_SKIP = "com.afollestad.overhear.action.SKIP";
	public static final String ACTION_REWIND = "com.afollestad.overhear.action.REWIND";
	
	
	private boolean requestAudioFocus() {
		if(hasAudioFocus) {
			return true;
		}
		int result = getAudioManager().requestAudioFocus(afl, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		boolean hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
		Log.i("OVERHEAR SERVICE", "requestAudioFocus() = " + hasFocus);
		return hasFocus;
	}

	private boolean initializeRemoteControl() {
		boolean focused = requestAudioFocus();
		if(focused) {
			Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
			ComponentName component = new ComponentName(getApplicationContext(), MediaButtonIntentReceiver.class);
			getAudioManager().registerMediaButtonEventReceiver(component);
			intent.setComponent(component);
			mRemoteControlClient = new RemoteControlClient(PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0));
			mRemoteControlClient.setTransportControlFlags(
					RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
					RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
					RemoteControlClient.FLAG_KEY_MEDIA_NEXT |
					RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS);
			getAudioManager().registerRemoteControlClient(mRemoteControlClient);
		}
		return focused;
	}

	private void updateRemoteControl(int state) {
		Song nowPlaying = Queue.getFocused(this);
		MetadataEditor metadataEditor = mRemoteControlClient
				.editMetadata(true)
				.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, nowPlaying.getArtist())
				.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, nowPlaying.getTitle())
				.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, nowPlaying.getDuration());
		Album album = Album.getAlbum(getApplicationContext(), nowPlaying.getAlbum(), nowPlaying.getArtist());
		try {
			AQuery aq = new AQuery(this);
	        Bitmap art = aq.getCachedImage(MusicUtils.getImageURL(this, 
	        		album.getName() + ":" + album.getArtist().getName(), AlbumAdapter.ALBUM_IMAGE));
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
				if(!nextTrack()) {
					getAudioManager().abandonAudioFocus(afl);
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

	private void initializeNotification(Song nowPlaying) {
		AQuery aq = new AQuery(this);
        Bitmap art = aq.getCachedImage(MusicUtils.getImageURL(this, nowPlaying.getAlbum() + ":" + 
        		nowPlaying.getArtist(), AlbumAdapter.ALBUM_IMAGE));
        status = NotificationViewCreator.createNotification(getApplicationContext(), nowPlaying, art, isPlaying());
        startForeground(100, status);
	}
	
	
	private void playTrack(Song song) {
		Log.i("OVERHEAR SERVICE", "playTrack(" + song.getData() + ")");
		if(!initializeRemoteControl()) {
			if(toast != null)
				toast.cancel(); 
			toast = Toast.makeText(getApplicationContext(), R.string.no_audio_focus, Toast.LENGTH_SHORT);
			toast.show();
			return;
		}
		Queue.setFocused(this, song, true);
		initializeMediaPlayer(song.getData());
		player.start();
		initializeNotification(song);
		Recents.add(getApplicationContext(), song);
		sendBroadcast(new Intent(RECENTS_UPDATED));
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
		updateRemoteControl(RemoteControlClient.PLAYSTATE_PLAYING);
	}

	private void playAll(Song song, int currentPosition, String[] scope) {
		Log.i("OVERHEAR SERVICE", "playTrack(" + song.getData() + ")");
		ArrayList<Song> queue = Queue.getQueue(this);
		if(queue == null || queue.size() == 0 || 
				(!queue.get(0).getArtist().equals(song.getArtist()) || !queue.get(0).getAlbum().equals(song.getAlbum()))) {
			queue = Song.getAllFromScope(getApplicationContext(), scope);
			Queue.setQueue(this, queue);
		}
		playTrack(queue.get(currentPosition));
	}

	private void resumeTrack() {
		Log.i("OVERHEAR SERVICE", "resumeTrack()");
		boolean focused = requestAudioFocus();
		if(!focused) {
			return;
		}
		Song last = Queue.getFocused(this);
		if(player != null && preparedPlayer && last != null) {
			if(!initializeRemoteControl()) {
				return;
			}
			Queue.setFocused(this, last, true);
			player.start();
			initializeNotification(last);
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		} else if(last != null) {
			Log.i("OVERHEAR SERVICE", "No paused state found");
			playTrack(last);
		} else {
			Log.i("OVERHEAR SERVICE", "No song to resume");
		}
	}
	
	private void pauseTrack() {
		Log.i("OVERHEAR SERVICE", "pauseTrack()");
		Song focused = Queue.getFocused(this);
		if(mRemoteControlClient != null)
			mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
		Queue.setFocused(this, focused, false);
		if(player != null && preparedPlayer && player.isPlaying()) {
			player.pause();
			initializeNotification(focused);
		} else {
			stopTrack();
		}
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
	}

	private void stopTrack() {
		Log.i("OVERHEAR SERVICE", "stopTrack()");
		if(player != null && preparedPlayer && player.isPlaying()) {
			player.stop();
			player.release();
		}
		if(mRemoteControlClient != null)
			mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
		getAudioManager().abandonAudioFocus(afl);
		Queue.clearPlaying(this);
		stopForeground(true);
		stopSelf();
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
	}

	private boolean nextTrack() {
		Log.i("OVERHEAR SERVICE", "nextTrack()"); 
		if(Queue.increment(this, true)) {
			playTrack(Queue.getFocused(this));
		} else {
			stopTrack();
			return false;
		}
		return true;
	}

	private void previousTrack() {
		Log.i("OVERHEAR SERVICE", "previousTrack()");
		if(Queue.decrement(this, true)) {
			playTrack(Queue.getFocused(this));
		} else {
			stopTrack();
			return;
		}
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
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
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
			playAll(Song.fromJSON(intent.getStringExtra("song")), 
					intent.getIntExtra("position", 0),
					intent.getStringArrayExtra("scope"));
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
		player.release();
		unregisterReceiver(receiver);
		getAudioManager().unregisterRemoteControlClient(mRemoteControlClient);
		getAudioManager().unregisterMediaButtonEventReceiver(new ComponentName(getApplicationContext(), MediaButtonIntentReceiver.class));
	}
}