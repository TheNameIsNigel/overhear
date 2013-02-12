package com.afollestad.overhear.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.*;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.service.MusicService.MusicBinder;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

import java.util.Timer;
import java.util.TimerTask;

public class NowPlayingViewer extends Activity {

	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			load();
		}
	};

	private Song song;
	private Album album;
	private Timer timer;
	private AnimationDrawable seekThumb;
	private Handler mHandler = new Handler();
	private MusicService mService;

	private View.OnTouchListener disappearListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			fadeIn(findViewById(R.id.progress));
			fadeIn(findViewById(R.id.remaining));
			resetSeekBarThumb((SeekBar)findViewById(R.id.seek));
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
		if(requestCode == TWEET_PLAYING_LOGIN && resultCode == Activity.RESULT_OK) {
			startActivity(new Intent(this, TweetNowPlaying.class));
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_now_playing);
		seekThumb = (AnimationDrawable)getResources().getDrawable(R.drawable.seekbar_thumb_fade_out);
		((SeekBar)findViewById(R.id.seek)).setThumb(seekThumb);
		IntentFilter filter = new IntentFilter();
		filter.addAction(MusicService.PLAYING_STATE_CHANGED);
		registerReceiver(mStatusReceiver, filter);
	}

	public void onResume() {
		super.onResume();
		bindService(new Intent(this, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
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
		load();
	}

	public void onPause() {
		super.onPause();
		if(mService != null)
			unbindService(mConnection);
		timer.cancel();
		timer.purge();
		timer = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_now_playing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.shopArtist:
			MusicUtils.browseArtist(getApplicationContext(), song.getArtist());
			return true;
		case R.id.tweetPlaying:
			if(Twitter.getTwitterInstance(getApplicationContext(), true) == null)
				startActivityForResult(new Intent(this, LoginHandler.class), TWEET_PLAYING_LOGIN);
			else
				startActivity(new Intent(this, TweetNowPlaying.class));
			return true;
        case R.id.sleepTimer:
            showSleepTimerDialog();
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

	private void fadeIn(final View v) {
		if(v.getAlpha() == 1) {
			Animation a = new AlphaAnimation(0.00f, 1.00f);
			a.setDuration(300);
			a.setAnimationListener(new AnimationListener() {
				public void onAnimationStart(Animation animation) { }
				public void onAnimationRepeat(Animation animation) { }
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
		if(v.getAlpha() == 0) {
			Animation a = new AlphaAnimation(1.00f, 0.00f);
			a.setDuration(700);
			a.setAnimationListener(new AnimationListener() {
				public void onAnimationStart(Animation animation) { }
				public void onAnimationRepeat(Animation animation) { }
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
		seekThumb = (AnimationDrawable)getResources().getDrawable(R.drawable.seekbar_thumb_fade_out);
		bar.setThumb(seekThumb);
		bar.invalidate();
	}


	/**
	 * Hooks UI elements to the music service media player.
	 */
	public void hookToPlayer() {
		if(mService == null) {
			return;
		}
		final MediaPlayer player = mService.getPlayer(false);
		if(player == null) {
			Toast.makeText(getApplicationContext(), "Unable to hook to the music player.", Toast.LENGTH_LONG).show();
			return;
		}
		player.setOnSeekCompleteListener(new OnSeekCompleteListener() {
			@Override
			public void onSeekComplete(MediaPlayer arg0) {
				update();
			}
		});
		final SeekBar seek = (SeekBar)findViewById(R.id.seek);
		seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
            }
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
            }
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if(fromUser) {
					player.seekTo(progress);
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
				if(album != null) {
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
	public void load() {
		song = Queue.getFocused(this);
		album = Album.getAlbum(this, song.getAlbum(), song.getArtist());
        AlbumAdapter.retrieveAlbumArt(this, album, (AImageView)findViewById(R.id.cover));
		((TextView)findViewById(R.id.track)).setText(song.getTitle());
		((TextView)findViewById(R.id.artistAlbum)).setText(song.getArtist() + " - " + album.getName());

        findViewById(R.id.cover).setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onBasicTouch(View v, MotionEvent event) {
                disappearListener.onTouch(v, event);
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
            public void onSwipeTop() {
            }
            @Override
            public void onSwipeBottom() {
            }
        });
	}

	/**
	 * Updates the play button, seek bar, and position indicators.
	 */
	public void update() {
		if(mService == null) {
			return;
		}

		MediaPlayer player = mService.getPlayer(true);
        SeekBar seek = (SeekBar)findViewById(R.id.seek);
        TextView progress = (TextView)findViewById(R.id.progress);
        TextView remaining = (TextView)findViewById(R.id.remaining);

		if(player != null && mService.isPlayerInitialized()) {
            if(player.isPlaying()) {
			    ((ImageButton)findViewById(R.id.play)).setImageResource(R.drawable.pause);
            } else {
                ((ImageButton)findViewById(R.id.play)).setImageResource(R.drawable.play);
            }
            int max = player.getDuration();
            int current = player.getCurrentPosition();
            seek.setProgress(current);
            seek.setMax(max);
            progress.setText(" " + Song.getDurationString(current));
            remaining.setText("-" + Song.getDurationString(max - current));
		} else {
			((ImageButton)findViewById(R.id.play)).setImageResource(R.drawable.play);
            seek.setProgress(0);
			seek.setMax(100);
			progress.setText(" 0:00");
			String duration = "-0:00";
			if(song != null) {
				duration = "-" + song.getDurationString();
			}
			remaining.setText(duration);
		}
	}

    public void showSleepTimerDialog() {
        if(SleepTimer.isScheduled(this)) {
            startActivity(new Intent(this, SleepTimerViewer.class));
            return;
        }
        final Dialog diag = new Dialog(this);
        diag.setContentView(R.layout.sleep_timer_dialog);
        diag.setCancelable(true);
        diag.setTitle(R.string.sleep_timer_str);
        final TextView display = (TextView)diag.findViewById(R.id.text);
        display.setText(getString(R.string.sleep_in_one));
        final SeekBar seek = (SeekBar)diag.findViewById(R.id.seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int value, boolean fromUser) {
                if(fromUser) {
                    value += 1;
                    if(value == 1) {
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


	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, IBinder service) {
			MusicBinder binder = (MusicBinder)service;
			mService = binder.getService();
			hookToPlayer();
		}
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			mService = null;
		}
	};
}