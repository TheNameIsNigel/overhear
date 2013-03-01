package com.afollestad.overhear.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.tasks.LastfmGetAlbumImage;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhear.utils.SleepTimer;
import com.afollestad.overhear.utils.Twitter;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.overhearapi.Song;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Shows details about the currently playing song with large album art.
 * 
 * @author Aidan Follestad
 */
public class NowPlayingViewer extends OverhearActivity {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            load(intent.getBooleanExtra("album_changed", true));
        }
    };

    private QueueItem songItem;
    private Song song;
    private Album album;
    private Playlist playlist;
    private Timer timer;
    private AnimationDrawable seekThumb;
    private Handler mHandler = new Handler();

    private View.OnTouchListener disappearListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            fadeIn(findViewById(R.id.progress), true);
            fadeIn(findViewById(R.id.remaining), true);
            resetSeekBarThumb((SeekBar) findViewById(R.id.seek));
            mHandler.removeCallbacks(disappearRunner);
            mHandler.postDelayed(disappearRunner, 3000);
            return false;
        }
    };
    private Runnable disappearRunner = new Runnable() {
        @Override
        public void run() {
            fadeOut(findViewById(R.id.progress));
            fadeOut(findViewById(R.id.remaining));
            seekThumb.start();
        }
    };


    public final static int TWEET_PLAYING_LOGIN = 400;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TWEET_PLAYING_LOGIN && resultCode == Activity.RESULT_OK) {
            startActivity(new Intent(this, TweetNowPlaying.class));
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_now_playing);
        seekThumb = (AnimationDrawable) getResources().getDrawable(R.drawable.seekbar_thumb_fade_out);
        ((SeekBar) findViewById(R.id.seek)).setThumb(seekThumb);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        registerReceiver(mStatusReceiver, filter);
    }

    public void onResume() {
        super.onResume();
        this.invalidateOptionsMenu();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        }, 250, 250);
    }

    public void onPause() {
        super.onPause();
        timer.cancel();
        timer.purge();
        timer = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_now_playing, menu);
        boolean favorited = MusicUtils.isFavorited(this, songItem);
        menu.findItem(R.id.favorite).setIcon(favorited ? R.drawable.favorited : R.drawable.unfavorited);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                startActivity(new Intent(this, OverviewScreen.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
                return true;
            }
            case R.id.shopArtist: {
                MusicUtils.browseArtist(getApplicationContext(), song.getArtist());
                return true;
            }
            case R.id.addToPlaylist: {
                AlertDialog diag = MusicUtils.createPlaylistChooseDialog(this, song, null, null);
                diag.show();
                return true;
            }
            case R.id.tweetPlaying: {
                if (Twitter.getTwitterInstance(getApplicationContext(), true) == null)
                    startActivityForResult(new Intent(this, LoginHandler.class), TWEET_PLAYING_LOGIN);
                else
                    startActivity(new Intent(this, TweetNowPlaying.class));
                return true;
            }
            case R.id.sleepTimer: {
                showSleepTimerDialog();
                return true;
            }
            case R.id.redownloadArt: {
            	Toast.makeText(getApplicationContext(), R.string.redownloading_art, Toast.LENGTH_SHORT).show();
            	new LastfmGetAlbumImage(this, getApplication(), (AImageView) findViewById(R.id.cover), true).execute(album);
            	return true;
        	}
            case R.id.favorite: {
            	MusicUtils.toggleFavorited(getApplicationContext(), songItem, song);
				this.invalidateOptionsMenu();
				return true;
            }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mStatusReceiver);
    }


    private void resetFade(final View v) {
        v.setAlpha(1.0f);
        v.setVisibility(View.VISIBLE);
        v.clearAnimation();
    }

    private void fadeIn(final View v, boolean instant) {
        if (v.getAlpha() < 1 && !instant) {
            Animation a = new AlphaAnimation(0.00f, 1.00f);
            a.setDuration(200);
            a.setAnimationListener(new AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    v.setVisibility(View.VISIBLE);
                    v.setAlpha(1);
                }
            });
            v.startAnimation(a);
        } else {
            v.setVisibility(View.VISIBLE);
            v.setAlpha(1);
        }
    }

    private void fadeOut(final View v) {
        if (v.getAlpha() > 0) {
            Animation a = new AlphaAnimation(1.00f, 0.00f);
            a.setDuration(600);
            a.setAnimationListener(new AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    v.setVisibility(View.INVISIBLE);
                    v.setAlpha(0);
                }
            });
            v.startAnimation(a);
        } else {
            v.setVisibility(View.INVISIBLE);
            v.setAlpha(0);
        }
    }

    private void resetSeekBarThumb(SeekBar bar) {
        seekThumb = (AnimationDrawable) getResources().getDrawable(R.drawable.seekbar_thumb_fade_out);
        bar.setThumb(seekThumb);
        bar.invalidate();
    }


    /**
     * Hooks UI elements to the music service media player.
     */
    public void hookToPlayer() {
        if (getService() == null) {
            return;
        }
        final MediaPlayer player = getService().getPlayer(false);
        if (player == null) {
            Toast.makeText(getApplicationContext(), "Unable to hook to the music player.", Toast.LENGTH_LONG).show();
            return;
        }
        player.setOnSeekCompleteListener(new OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer arg0) {
                update();
            }
        });
        final SeekBar seek = (SeekBar) findViewById(R.id.seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (getService() != null && getService().isPlayerInitialized() && getService().isPlaying()) {
                        player.seekTo(progress);
                    } else {
                        startService(new Intent(getApplicationContext(), MusicService.class)
                                .setAction(MusicService.ACTION_TOGGLE_PLAYBACK));
                    }
                }
            }
        });

        findViewById(R.id.previous).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                startService(new Intent(getApplicationContext(), MusicService.class)
                        .setAction(MusicService.ACTION_REWIND));
            }
        });
        findViewById(R.id.previous).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                startService(new Intent(getApplicationContext(), MusicService.class)
                        .setAction(MusicService.ACTION_REWIND).putExtra("override", true));
                return true;
            }
        });
        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                startService(new Intent(getApplicationContext(), MusicService.class)
                        .setAction(MusicService.ACTION_TOGGLE_PLAYBACK));
            }
        });
        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(new Intent(getApplicationContext(), MusicService.class)
                        .setAction(MusicService.ACTION_SKIP));
            }
        });
        findViewById(R.id.meta).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(playlist != null) {
                    startActivity(new Intent(getApplicationContext(), PlaylistViewer.class)
                            .putExtra("playlist", playlist.getJSON().toString()));
                } else {
                    startActivity(new Intent(getApplicationContext(), AlbumViewer.class)
                            .putExtra("album", album.getJSON().toString()));
                }
            }
        });
        seek.setOnDragListener(new View.OnDragListener() {
            @Override
            public boolean onDrag(View v, DragEvent event) {
                resetFade(findViewById(R.id.progress));
                resetFade(findViewById(R.id.remaining));
                resetSeekBarThumb(seek);
                return false;
            }
        });
        seek.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                disappearListener.onTouch(v, event);
                return false;
            }
        });
        disappearListener.onTouch(null, null);
    }

    /**
     * Loads song/album/artist info and album art
     */
    public void load(boolean albumChanged) {
        songItem = getService().getQueue().getFocusedItem();
        if(songItem == null)
        	return;
        song = songItem.getSong(this);
        this.invalidateOptionsMenu();
        
        album = Album.getAlbum(this, song.getAlbum(), song.getArtist());
        if(song.getPlaylistId() > -1) {
            playlist = Playlist.get(this, song.getPlaylistId());
        } else {
            playlist = null;
        }
        
        AImageView cover = (AImageView) findViewById(R.id.cover);
        if (album != null && (albumChanged || cover.getDrawable() == null)) {
            AlbumAdapter.retrieveAlbumArt(this, album, cover);
        }
        ((TextView) findViewById(R.id.track)).setText(song.getTitle());
        ((TextView) findViewById(R.id.artistAlbum)).setText(
        		(song != null ? song.getArtist() : getString(R.string.unknown_str)) + " - " + 
        		(album != null ? album.getName() : getString(R.string.unknown_str))
        );

        findViewById(R.id.cover).setOnTouchListener(new OnSwipeTouchListener(this) {
			@Override
			public void onSwipeTop() {
				Toast.makeText(getApplicationContext(), R.string.redownloading_art, Toast.LENGTH_SHORT).show();
                new LastfmGetAlbumImage(getApplicationContext(), getApplication(), (AImageView) findViewById(R.id.cover), true).execute(album);
			}
			@Override
			public void onSwipeRight() {
				findViewById(R.id.previous).performClick();
			}
			@Override
			public void onSwipeLeft() {
				findViewById(R.id.next).performClick();
			}
			@Override
			public void onSwipeBottom() {
				findViewById(R.id.meta).performClick();
			}
			@Override
			public void onBasicTouch(View v, MotionEvent event) {
				disappearListener.onTouch(v, event);
			}
			@Override
			public void onDoubleTap() {
				findViewById(R.id.play).performClick();
			}
		});
        
        findViewById(R.id.cover).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });
    }
    
    /**
     * Updates the play button, seek bar, and position indicators.
     */
    public void update() {
        if (getService() == null) {
            return;
        }

        MediaPlayer player = getService().getPlayer(true);
        SeekBar seek = (SeekBar) findViewById(R.id.seek);
        TextView progress = (TextView) findViewById(R.id.progress);
        TextView remaining = (TextView) findViewById(R.id.remaining);

        if (player != null && getService().isPlayerInitialized()) {
            if (player.isPlaying()) {
                ((ImageButton) findViewById(R.id.play)).setImageResource(R.drawable.pause);
            } else {
                ((ImageButton) findViewById(R.id.play)).setImageResource(R.drawable.play);
            }
            int max = player.getDuration();
            int current = player.getCurrentPosition();
            seek.setProgress(current);
            seek.setMax(max);
            progress.setText(" " + Song.getDurationString(current));
            remaining.setText("-" + Song.getDurationString(max - current));
        } else {
            ((ImageButton) findViewById(R.id.play)).setImageResource(R.drawable.play);
            seek.setProgress(0);
            seek.setMax(100);
            progress.setText(" 0:00");
            String duration = "-0:00";
            if (song != null) {
                duration = "-" + song.getDurationString();
            }
            remaining.setText(duration);
        }
    }

    public void showSleepTimerDialog() {
        if (SleepTimer.isScheduled(this)) {
            startActivity(new Intent(this, SleepTimerViewer.class));
            return;
        }
        final Dialog diag = new Dialog(this);
        diag.setContentView(R.layout.sleep_timer_dialog);
        diag.setCancelable(true);
        diag.setTitle(R.string.sleep_timer_str);
        final TextView display = (TextView) diag.findViewById(R.id.text);
        display.setText(getString(R.string.sleep_in_one));
        final SeekBar seek = (SeekBar) diag.findViewById(R.id.seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if (fromUser) {
                    value += 1;
                    if (value == 1) {
                        display.setText(getString(R.string.sleep_in_one));
                    } else {
                        display.setText(getString(R.string.sleep_in_x).replace("{x}", Integer.toString(value)));
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        diag.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                diag.dismiss();
                SleepTimer.schedule(getApplicationContext(), seek.getProgress() + 1);
                startActivity(new Intent(getApplicationContext(), SleepTimerViewer.class));
            }
        });
        diag.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                diag.dismiss();
            }
        });
        seek.setMax(59);
        diag.show();
    }

	@Override
	public void onBound() {
		load(true);
		hookToPlayer();
	}
}