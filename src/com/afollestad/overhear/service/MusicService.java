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
import com.afollestad.aimage.Dimension;
import com.afollestad.aimage.ImageListener;
import com.afollestad.overhear.*;
import com.afollestad.overhear.base.Overhear;
import com.afollestad.overhear.tasks.LastfmGetAlbumImage;
import com.afollestad.overhear.utils.Queue;
import com.afollestad.overhear.utils.Recents;
import com.afollestad.overhear.utils.SleepTimer;
import com.afollestad.overhearapi.Album;
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
    private Song lastPlaying;

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
    public static final String ACTION_PLAY = "com.afollestad.overhear.action.PLAY";
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

    private void updateRemoteControl(final int state) {
        Song nowPlaying = Queue.getFocused(this);
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
            Overhear.get(this).getManager().get(url, null, new ImageListener() {
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
            Overhear.get(this).getManager().get(url, new Dimension(this, 130f), new ImageListener() {
                @Override
                public void onImageReceived(final String source, final Bitmap bitmap) {
                    Notification update = NotificationViewCreator.createNotification(getApplicationContext(), nowPlaying, bitmap, isPlaying());
                    NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(100, update);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playTrack(Song song) {
        Log.i("OVERHEAR SERVICE", "playTrack(\"" + song.getData() + "\")");
        if (!initializeRemoteControl()) {
            if (toast != null)
                toast.cancel();
            toast = Toast.makeText(getApplicationContext(), R.string.no_audio_focus, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        Queue.setFocused(this, song, true);
        Recents.add(this, song);
        initializeMediaPlayer(song.getData());
        initializeNotification(song);

        sendBroadcast(new Intent(PLAYING_STATE_CHANGED)
                .putExtra("album_changed", lastPlaying == null || !lastPlaying.getAlbum().equals(song.getAlbum())));
        updateRemoteControl(RemoteControlClient.PLAYSTATE_PLAYING);
        lastPlaying = song;
    }

    private void playAll(Song song, String[] scope, int queuePos, Playlist list) {
        Log.i("OVERHEAR SERVICE", "playAll(\"" + (song != null ? song.getData() : "null") + "\")");
        ArrayList<Song> queue = null;
        int skipPos = Queue.canQueueSkip(this, song);
        if (skipPos == -1) {
            if(list != null)
                queue = list.getSongs(this);
            else
                queue = Song.getAllFromScope(getApplicationContext(), scope);
            queue = Queue.setQueue(this, queue);
        } else {
            queue = Queue.getQueue(this);
            queuePos = skipPos;
        }
        if(queuePos == -1) {
        	queuePos = 0;
        	for(int i = 0; i < queue.size(); i++) {
        		if(queue.get(i).getId() == song.getId()) {
        			queuePos = i;
        		}
        		break;
        	}
        }
        playTrack(queue.get(queuePos));
    }

    private void resumeTrack() {
        Log.i("OVERHEAR SERVICE", "resumeTrack()");
        Song last = Queue.getFocused(this);
        if (player != null && last != null && initialized) {
            if (!initializeRemoteControl()) {
                return;
            }
            Queue.setFocused(this, last, true);
            try {
                player.start();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                playTrack(last);
                return;
            }
            initializeNotification(last);
            sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
        } else if (last != null) {
            Log.i("OVERHEAR SERVICE", "No paused state found");
            playTrack(last);
        } else {
            Log.i("OVERHEAR SERVICE", "No song to resume");
        }
    }

    private void pauseTrack() {
        Log.i("OVERHEAR SERVICE", "pauseTrack()");
        Song focused = Queue.getFocused(this);
        if (mRemoteControlClient != null)
            mRemoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
        Queue.setFocused(this, focused, false);
        if (player != null && player.isPlaying()) {
            player.pause();
            initializeNotification(focused);
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
        Queue.clearPlaying(this);
        stopForeground(true);
        stopSelf();
        sendBroadcast(new Intent(PLAYING_STATE_CHANGED));
    }

    private boolean nextTrack() {
        Log.i("OVERHEAR SERVICE", "nextTrack()");
        if (Queue.increment(this, true)) {
            playTrack(Queue.getFocused(this));
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
            if (Queue.decrement(this, true)) {
                playTrack(Queue.getFocused(this));
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
        if (action.equals(ACTION_PLAY)) {
            if (intent.hasExtra("song")) {
                playTrack(Song.fromJSON(intent.getStringExtra("song")));
            } else {
                resumeTrack();
            }
        } else if (action.equals(ACTION_PLAY_ALL)) {
            String[] scope = null;
            Song song = null;
            if (intent.hasExtra("album_id")) {
                scope = new String[]{
                        MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " +
                                MediaStore.Audio.Media.ALBUM_ID + " = " + intent.getIntExtra("album_id", 0),
                        MediaStore.Audio.Media.TRACK
                };
            } else if(intent.hasExtra("artist")) {
            	scope = new String[]{
                        MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " +
                                MediaStore.Audio.Media.ARTIST + " = '" + intent.getStringExtra("artist").replace("'", "''") + "'",
                        MediaStore.Audio.Media.ALBUM
            	};
            } else {
                song = Song.fromJSON(intent.getStringExtra("song"));
                scope = intent.getStringArrayExtra("scope");
                if (scope == null) {
                    scope = new String[]{
                            MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " +
                                    MediaStore.Audio.Media.ALBUM + " = '" + song.getAlbum().replace("'", "''") + "' AND " +
                                    MediaStore.Audio.Media.ARTIST + " = '" + song.getArtist().replace("'", "''") + "'",
                            MediaStore.Audio.Media.TRACK
                    };
                }
            }
            Playlist list = null;
            if(intent.hasExtra("playlist"))
                list = Playlist.fromJSON(intent.getStringExtra("playlist"));
            playAll(song, scope, intent.getIntExtra("position", 0), list);
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
            Song focused = Queue.getFocused(this);
            if(focused != null && !focused.isPlaying()) {
                stopForeground(true);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i("MusicService", "onDestroy()");
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