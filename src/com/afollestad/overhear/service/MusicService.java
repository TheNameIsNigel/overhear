package com.afollestad.overhear.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.*;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.os.Binder;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import com.afollestad.aimage.ImageListener;
import com.afollestad.overhear.*;
import com.afollestad.overhear.base.Overhear;
import com.afollestad.overhear.queue.Queue;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.tasks.LastfmGetAlbumImage;
import com.afollestad.overhear.utils.Recents;
import com.afollestad.overhear.utils.SleepTimer;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Genre;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.overhearapi.Song;

import java.util.ArrayList;
import java.util.Calendar;

public class MusicService extends Service {

	public MusicService() {
	}

	private final IBinder mBinder = new MusicBinder();
	private boolean hasAudioFocus;
	private boolean wasPlayingBeforeLoss;
	private Toast toast;
	private boolean initialized;
	private Queue queue;
	private Song lastFocused;

	public Queue getQueue() {
		if(queue == null)
			queue = new Queue(this);
		return queue;
	}

	private static MediaPlayer player;
	private AudioManager audioManager;
	private RemoteControlClient mRemoteControlClient;

	public AudioManager getAudioManager() {
		if (audioManager == null) {
			audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		}
		return audioManager;
	}

	public MediaPlayer getPlayer(boolean nullIfNotInitialized) {
		if (player == null && !nullIfNotInitialized) {
			player = new MediaPlayer();
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
				@Override
				public void onCompletion(MediaPlayer mp) {
					if (!nextTrack()) {
						getAudioManager().abandonAudioFocus(afl);
					}
				}
			});
			player.setOnErrorListener(new MediaPlayer.OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					switch (what) {
					case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
						player.release();
						player = null;
						initialized = false;
						Toast.makeText(getApplicationContext(), "Media server died", Toast.LENGTH_LONG).show();
						break;
					default:
						if (extra == MediaPlayer.MEDIA_ERROR_IO) {
							Toast.makeText(getApplicationContext(), "Media player I/O error", Toast.LENGTH_LONG).show();
						} else if (extra == MediaPlayer.MEDIA_ERROR_MALFORMED || extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
							Toast.makeText(getApplicationContext(), "Media player malformed or supported error", Toast.LENGTH_LONG).show();
						} else if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
							Toast.makeText(getApplicationContext(), "Media player timed out", Toast.LENGTH_LONG).show();
						} else {
							Toast.makeText(getApplicationContext(), "Unknown media player error", Toast.LENGTH_LONG).show();
						}
						break;
					}
					return true;
				}
			});
		}
		return player;
	}

	private AudioManager.OnAudioFocusChangeListener afl = new AudioManager.OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			switch (focusChange) {
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
				if (wasPlayingBeforeLoss)
					resumeTrack();
				break;
			}
		}
	};

	public final static String PLAYING_STATE_CHANGED = "com.afollestad.overhear.PLAY_STATE_CHANGED";
	public final static String RECENTS_UPDATED = "com.afollestad.overhear.RECENTS_UPDATED";
	public final static String PLAYLIST_UPDATED = "com.afollestad.overhear.PLAYLIST_UPDATED";

	public static final String ACTION_SLEEP_TIMER = "com.afollestad.overhear.action.SLEEP_TIMER";
	public static final String ACTION_TOGGLE_PLAYBACK = "com.afollestad.overhear.action.TOGGLE_PLAYBACK";
	public static final String ACTION_PLAY_ALL = "com.afollestad.overhear.action.PLAY_ALL";
	public static final String ACTION_PAUSE = "com.afollestad.overhear.action.PAUSE";
	public static final String ACTION_STOP = "com.afollestad.overhear.action.STOP";
	public static final String ACTION_SKIP = "com.afollestad.overhear.action.SKIP";
	public static final String ACTION_REWIND = "com.afollestad.overhear.action.REWIND";
	public static final String ACTION_CLEAR_NOTIFICATION = "com.afollestad.overhear.action.CLEAR_NOTIFICATION";


	private boolean requestAudioFocus() {
		if (hasAudioFocus) {
			return true;
		}
		int result = getAudioManager().requestAudioFocus(afl, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
		boolean hasFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED);
		Log.i("OVERHEAR SERVICE", "requestAudioFocus() = " + hasFocus);
		return hasFocus;
	}

	private boolean initializeRemoteControl() {
		boolean focused = requestAudioFocus();
		if (focused) {
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

	private void updateRemoteControl(final int state, Song nowPlaying) {
		mRemoteControlClient
		.editMetadata(false)
		.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, nowPlaying.getArtist())
		.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, nowPlaying.getTitle())
		.putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, nowPlaying.getDuration()).apply();
		mRemoteControlClient.setPlaybackState(state);

		Album album = Album.getAlbum(getApplicationContext(), nowPlaying.getAlbum(), nowPlaying.getArtist());
		LastfmGetAlbumImage task = new LastfmGetAlbumImage(this, getApplication(), null, false);
		task.execute(album);
		try {
			String url = task.get();
			Overhear.get(this).getManager().get(url, new ImageListener() {
				@Override
				public void onImageReceived(final String source, final Bitmap bitmap) {
					mRemoteControlClient
					.editMetadata(false)
					.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, bitmap)
					.apply();
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeMediaPlayer(String source) {
		MediaPlayer player = getPlayer(false);
		try {
			player.reset();
			player.setDataSource(source);
			player.prepare();
			player.start();
			initialized = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeNotification(final Song nowPlaying) {
		Notification status = NotificationViewCreator.createNotification(getApplicationContext(), nowPlaying, null, isPlaying());
		startForeground(100, status);

		Album album = Album.getAlbum(this, nowPlaying.getAlbum(), nowPlaying.getArtist());
		LastfmGetAlbumImage task = new LastfmGetAlbumImage(this, getApplication(), null, false);
		task.execute(album);
		try {
			String url = task.get();
			Overhear.get(this).getManager().get(url, new ImageListener() {
				@Override
				public void onImageReceived(final String source, final Bitmap bitmap) {
					Notification update = NotificationViewCreator.createNotification(
							getApplicationContext(), nowPlaying, bitmap, isPlaying());
					NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
					nm.notify(100, update);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void playTrack(QueueItem item, boolean moveQueue) {
		Log.i("OVERHEAR SERVICE", "playTrack(" + item.getSongId() + ")");
		
		final Song itemSong = item.getSong(this);
		if (!initializeRemoteControl()) {
			if (toast != null)
				toast.cancel();
			toast = Toast.makeText(getApplicationContext(), R.string.no_audio_focus, Toast.LENGTH_SHORT);
			toast.show();
			return;
		}

		if(moveQueue)
			queue.move(queue.find(item));

		initializeMediaPlayer(item.getData());
		initializeNotification(itemSong);
		updateRemoteControl(RemoteControlClient.PLAYSTATE_PLAYING, itemSong);
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED)
				.putExtra("album_changed", lastFocused == null || 
					(!lastFocused.getAlbum().equals(itemSong.getAlbum()) ||
					!lastFocused.getArtist().equals(itemSong.getArtist()))));
		lastFocused = itemSong;
		Recents.add(this, itemSong);
	}

	private void playAll(Song song, int scope, int queuePos, Album album, Artist artist, Playlist list, Genre genre) {
		QueueItem item = null;
		if(song != null) {
			item = new QueueItem(song, scope);
		}

		if (item == null || !queue.contains(item)) {
			// The queue doesn't contain the song being played, load it's scope into the queue now
			ArrayList<Song> queue = null;
			switch(scope) {
			case QueueItem.SCOPE_SINGULAR:
				queue = new ArrayList<Song>();
				queue.add(song);
				break;
			case QueueItem.SCOPE_All_SONGS: {
				queue = Song.getAllFromScope(this, new String[] { null, 
						MediaStore.Audio.Media.TITLE });
				if(item == null)
					item = new QueueItem(queue.get(0), QueueItem.SCOPE_All_SONGS);
				break;
			}
			case QueueItem.SCOPE_ALBUM: {
				queue = Song.getAllFromScope(this, new String[] {
						MediaStore.Audio.Media.ALBUM + " = '" + album.getName().replace("'", "''") + "' AND " +
								MediaStore.Audio.Media.ARTIST + " = '" + album.getArtist().getName().replace("'", "''") + "'",
								MediaStore.Audio.Media.TRACK
				});
				if(item == null)
					item = new QueueItem(queue.get(0), QueueItem.SCOPE_ALBUM);
				break;
			}
			case QueueItem.SCOPE_ARTIST: {
				queue = Song.getAllFromScope(this, new String[] {
						MediaStore.Audio.Media.ARTIST + " = '" + artist.getName().replace("'", "''") + "'",
						MediaStore.Audio.Media.ALBUM
				});
				if(item == null)
					item = new QueueItem(queue.get(0), QueueItem.SCOPE_ARTIST);
				break;
			}
			case QueueItem.SCOPE_PLAYLIST: {
				queue = list.getSongs(this, null);
				if(item == null)
					item = new QueueItem(queue.get(0), QueueItem.SCOPE_PLAYLIST);
				break;
			}
			case QueueItem.SCOPE_GENRE: {
				queue = genre.getSongs(this);
				if(item == null)
					item = new QueueItem(queue.get(0), QueueItem.SCOPE_GENRE);
				break;
			}
			}

			Log.i("OVERHEAR SERVICE", "playAll(" + (item != null ? item.getSongId() : "null") + ")");
			this.queue.set(queue, scope);
			if(queuePos > -1) {
				this.queue.move(queuePos);
			} else {
				this.queue.move(0);
			}	
		} else {
			Log.i("OVERHEAR SERVICE", "playAll(" + (item != null ? item.getSongId() : "null") + ")");
			this.queue.move(this.queue.find(item));
		}

		playTrack(this.queue.getFocusedItem(), false);
	}

	private void resumeTrack() {
		Log.i("OVERHEAR SERVICE", "resumeTrack()");
		QueueItem last = queue.getFocusedItem();
		if (player != null && last != null && queue.getPosition() > -1 && initialized) {
			if (!initializeRemoteControl()) {
				return;
			}
			try {
				player.start();
			} catch (IllegalStateException e) {
				e.printStackTrace();
				playTrack(last, true);
				return;
			}
			initializeNotification(last.getSong(this));
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
			mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
		} else if(last != null) {
			Log.i("OVERHEAR SERVICE", "No pause state found, restarting focused song");
			playTrack(last, true);
		} else {
			Log.i("OVERHEAR SERVICE", "No song to resume");
		}
	}

	private void pauseTrack() {
		Log.i("OVERHEAR SERVICE", "pauseTrack()");
		if (mRemoteControlClient != null)
			mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
		if (player != null && player.isPlaying()) {
			player.pause();
			initializeNotification(queue.getFocused());
		} else {
			stopTrack();
		}
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
	}

	private void stopTrack() {
		Log.i("OVERHEAR SERVICE", "stopTrack()");
		if (player != null && player.isPlaying()) {
			player.stop();
			player.release();
			player = null;
			initialized = false;
		}
		if (mRemoteControlClient != null)
			mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
		getAudioManager().abandonAudioFocus(afl);
		stopForeground(true);
		stopSelf();
		sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
	}

	private boolean nextTrack() {
		Log.i("OVERHEAR SERVICE", "nextTrack()");
		if (queue.increment()) {
			playTrack(queue.getFocusedItem(), true);
		} else {
			stopTrack();
			return false;

		}
		return true;
	}

	private void previousOrRewind(boolean override) {
		Log.i("OVERHEAR SERVICE", "previousTrack()");
		if (player != null && player.getCurrentPosition() > 3000 && !override) {
			player.seekTo(0);
			sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
		} else {
			if (queue.decrement()) {
				playTrack(queue.getFocusedItem(), true);
			} else {
				stopTrack();
				return;
			}
		}
	}

	public boolean isPlaying() {
		return player != null && initialized && player.isPlaying();
	}

	public boolean isPlayerInitialized() {
		return initialized;
	}


	public class MusicBinder extends Binder {
		public MusicService getService() {
			return MusicService.this;
		}
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
				pauseTrack();
			}
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		queue = new Queue(this);
		registerReceiver(receiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		if (intent == null || intent.getAction() == null) {
			return START_STICKY;
		}
		String action = intent.getAction();
		if (action.equals(ACTION_PLAY_ALL)) {
			Song song = Song.fromJSON(intent.getStringExtra("song"));
			Album album = null;
			Artist artist = null;
			Playlist list = null;
			Genre genre = null;
			if(intent.hasExtra("album"))
				album = Album.fromJSON(intent.getStringExtra("album"));
			if(intent.hasExtra("artist"))
				artist = Artist.fromJSON(intent.getStringExtra("artist"));
			if(intent.hasExtra("playlist"))
				list = Playlist.fromJSON(intent.getStringExtra("playlist"));
			if(intent.hasExtra("genre"))
				genre = Genre.fromJSON(intent.getStringExtra("genre"));
			playAll(song, intent.getIntExtra("scope", 0), intent.getIntExtra("position", 0), album, artist, list, genre);
		} else if (action.equals(ACTION_PAUSE)) {
			pauseTrack();
		} else if (action.equals(ACTION_SLEEP_TIMER)) {
			long scheduledTime = SleepTimer.getScheduledTime(this).getTimeInMillis();
			if (Calendar.getInstance().getTimeInMillis() < scheduledTime) {
				// Verify that now is for sure the sleep timer scheduled time (or after it).
				return START_STICKY;
			}
			pauseTrack();
		} else if (action.equals(ACTION_SKIP)) {
			nextTrack();
		} else if (action.equals(ACTION_REWIND)) {
			previousOrRewind(intent.getBooleanExtra("override", false));
		} else if (action.equals(ACTION_STOP)) {
			stopTrack();
		} else if (action.equals(ACTION_TOGGLE_PLAYBACK)) {
			if (isPlaying())
				pauseTrack();
			else
				resumeTrack();
		} else if(action.equals(ACTION_CLEAR_NOTIFICATION)) {
			if(!isPlaying())
				stopForeground(true);
		}
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		if(queue != null)
			queue.persist(this);
		if (player != null && player.isPlaying()) {
			player.stop();
			player.release();
			player = null;
		}
		initialized = false;
		unregisterReceiver(receiver);
		getAudioManager().unregisterRemoteControlClient(mRemoteControlClient);
		getAudioManager().unregisterMediaButtonEventReceiver(new ComponentName(getApplicationContext(), MediaButtonIntentReceiver.class));
		super.onDestroy();
	}
}