package com.afollestad.overhear;

import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.ImageView;

/**
 * Wraps around the now_playing_bar layout that's included in any screen with music conrols, and
 * automatically handles user interaction and notifications from the MusicService.
 * 
 * @author Aidan Follestad
 */
public class NowPlayingBar {

	private NowPlayingBar() { }
	
	private MusicBoundActivity context;
	private ImageView playing;
	private ImageView playPause;
	private ImageView previous;
	private ImageView next;
	
	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	update();
        }
    };
	
	public static NowPlayingBar get(MusicBoundActivity context) {
		NowPlayingBar bar = new NowPlayingBar();
		bar.context = context;
		bar.playing = (ImageView)context.findViewById(R.id.playing);
		bar.playPause = (ImageView)context.findViewById(R.id.play);
		bar.previous = (ImageView)context.findViewById(R.id.previous);
		bar.next = (ImageView)context.findViewById(R.id.next);
		bar.initialize();
		bar.update();
		return bar;
	}
	
	private void initialize() {
		IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
		context.registerReceiver(mStatusReceiver, filter);
		
		playPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(context.getMusicService() == null) {
					return;
				}
				if(context.getMusicService().isPlaying()) {
					context.getMusicService().pauseTrack();
				} else {
					try {
						context.getMusicService().resumeTrack();
					} catch(Exception e) {
						Crouton.makeText(context, e.getMessage(), Style.ALERT);
					}
				}
			}
		});
		playing.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//TODO open now playing screen 
			}
		});
		next.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Crouton.makeText(context, "Not implemented yet", Style.ALERT).show();
			}
		});
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Crouton.makeText(context, "Not implemented yet", Style.ALERT).show();
			}
		});
	}
	
	private void update() {
		if(context == null) {
			return;
		}
		if(context.getMusicService() != null) {
			Song song = context.getMusicService().getNowPlaying();
			if(song != null) {
				playPause.setImageResource(R.drawable.pause);
			} else {
				song = MusicService.MusicUtils.getLastPlaying(context);
				playPause.setImageResource(R.drawable.play);
			}
			if(song != null) {
				Album album = Album.getAlbum(context, song.getAlbum());
				playing.setImageBitmap(album.getAlbumArt(context, 42f, 42f));
			} else {
				//TODO default now playing image
			}
		}
	}
}