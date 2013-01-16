package com.afollestad.overhear.fragments;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.MusicService;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class NowPlayingBarFragment extends Fragment {

	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	update();
        }
    };
    
    private ImageView playing;
	private ImageView playPause;
	private ImageView previous;
	private ImageView next;
	
	@Override
	public void onCreate(Bundle sis) {
		super.onCreate(sis);
		IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
		getActivity().registerReceiver(mStatusReceiver, filter);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(mStatusReceiver);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		return inflater.inflate(R.layout.now_playing_bar, null);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		playing = (ImageView)view.findViewById(R.id.playing);
		playPause = (ImageView)view.findViewById(R.id.play);
		previous = (ImageView)view.findViewById(R.id.previous);
		next = (ImageView)view.findViewById(R.id.next);
		initialize();
	}
	
	private MusicService getMusicService() {
		if(getActivity() == null) {
			return null;
		} else if(!(getActivity() instanceof MusicBoundActivity)) {
			return null;
		}
		return ((MusicBoundActivity)getActivity()).getMusicService();
	}
	
	private void initialize() {
		playPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(getMusicService() == null) {
					return;
				}
				if(getMusicService().isPlaying()) {
					getMusicService().pauseTrack();
				} else {
					try {
						getMusicService().resumeTrack();
					} catch(Exception e) {
						Crouton.makeText(getActivity(), e.getMessage(), Style.ALERT);
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
				Crouton.makeText(getActivity(), "Not implemented yet", Style.ALERT).show();
			}
		});
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				Crouton.makeText(getActivity(), "Not implemented yet", Style.ALERT).show();
			}
		});
		update();
	}
	
	public void update() {
		if(getActivity() == null) {
			return;
		}
		if(getMusicService() != null) {
			Song song = getMusicService().getNowPlaying();
			if(song != null) {
				playPause.setImageResource(R.drawable.pause);
			} else {
				song = MusicUtils.getLastPlaying(getActivity());
				playPause.setImageResource(R.drawable.play);
			}
			if(song != null) {
				Album album = Album.getAlbum(getActivity(), song.getAlbum());
//        		TODO Un-comment to re-enable scaling of images (if out of memory errors ever occur)
//				int dimen = context.getResources().getDimensionPixelSize(R.dimen.now_playing_bar_cover);
				int dimen = -1;
				AlbumAdapter.startAlbumArtTask(getActivity(), album, playing, dimen);
			} else {
				//TODO default now playing image
			}
		}
	}
}
