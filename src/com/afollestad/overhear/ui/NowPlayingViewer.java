package com.afollestad.overhear.ui;

import java.util.Timer;
import java.util.TimerTask;

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
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class NowPlayingViewer extends Activity {
	
	private Timer timer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_now_playing);
		hookToPlayer();
	}
	
	public void onResume() {
		super.onResume();
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
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_now_playing, menu);
		return true;
	}
	
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
	}
	
	public void load() {
		Song focused = Queue.getFocused(this);
		Album album = Album.getAlbum(this, focused.getAlbum(), focused.getArtist());
		AlbumAdapter.startAlbumArtTask(this, album, (ImageView)findViewById(R.id.cover), -1);
		((TextView)findViewById(R.id.track)).setText(focused.getTitle());
		((TextView)findViewById(R.id.artistAlbum)).setText(focused.getArtist() + " - " + album.getName());
		
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
}
