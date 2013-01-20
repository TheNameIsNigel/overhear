package com.afollestad.overhear.ui;

import java.util.Timer;
import java.util.TimerTask;

import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

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
		IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        registerReceiver(mStatusReceiver, filter);
		hookToPlayer();
	}
	
	public void onResume() {
		super.onResume();
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
			if(LoginHandler.getTwitterInstance(getApplicationContext(), true) == null)
				startActivityForResult(new Intent(this, LoginHandler.class), TWEET_PLAYING_LOGIN);
			else
				startActivity(new Intent(this, TweetNowPlaying.class));
			return true;
		}
		return false;
	}
	
	/**
	 * Hooks UI elements to the music service media player.
	 */
	public void hookToPlayer() {
		final MediaPlayer player = MusicService.getPlayer();
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
		SeekBar seek = (SeekBar)findViewById(R.id.seek);
		seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) { }
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) { }
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
		findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				startService(new Intent(getApplicationContext(), MusicService.class)
					.setAction(MusicService.ACTION_TOGGLE_PLAYBACK));
			}
		});
		findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
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
	}
	
	/**
	 * Loads song/album/artist info and album art
	 */
	public void load() {
		song = Queue.getFocused(this);
		album = Album.getAlbum(this, song.getAlbum(), song.getArtist());
		AlbumAdapter.startAlbumArtTask(this, album, (ImageView)findViewById(R.id.cover), -1);
		((TextView)findViewById(R.id.track)).setText(song.getTitle());
		((TextView)findViewById(R.id.artistAlbum)).setText(song.getArtist() + " - " + album.getName());
	}
	
	/**
	 * Updates the play button, seek bar, and position indicators.
	 */
	public void update() {
		MediaPlayer player = MusicService.getPlayer();
		if(player == null) {
			Toast.makeText(getApplicationContext(), "Unable to hook to the music player.", Toast.LENGTH_LONG).show();
			return;
		}
		if(player.isPlaying()) {
			((ImageButton)findViewById(R.id.play)).setImageResource(R.drawable.pause);
		} else {
			((ImageButton)findViewById(R.id.play)).setImageResource(R.drawable.play);
		}
		SeekBar seek = (SeekBar)findViewById(R.id.seek);
		TextView progress = (TextView)findViewById(R.id.progress);
		TextView remaining = (TextView)findViewById(R.id.remaining);
		int max = player.getDuration();
		int current = player.getCurrentPosition();
		seek.setProgress(current);
		seek.setMax(max);
		progress.setText(Song.getDurationString(current));
		remaining.setText("-" + Song.getDurationString(max - current));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mStatusReceiver);
	}
}
