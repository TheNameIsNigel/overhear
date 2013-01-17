package com.afollestad.overhear.fragments;

import com.afollestad.overhear.MusicService;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

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

/**
 * A completely self-sufficient now playing bar, displayed on the bottom of any activity that has music controls.
 * 
 * @author Aidan Follestad
 */
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
	public void onResume() {
		super.onResume();
		update();
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

	private void initialize() {
		playPause.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().startService(new Intent(getActivity(), MusicService.class)
				.setAction(MusicService.ACTION_TOGGLE_PLAYBACK));
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
				getActivity().startService(new Intent(getActivity(), MusicService.class)
				.setAction(MusicService.ACTION_SKIP));
			}
		});
		previous.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				getActivity().startService(new Intent(getActivity(), MusicService.class)
				.setAction(MusicService.ACTION_REWIND));
			}
		});
		update();
	}

	public void update() {
		if(getActivity() == null) {
			return;
		}
		Song nowPlaying = MusicUtils.getNowPlaying(getActivity());
		if(nowPlaying != null) {
			playPause.setImageResource(R.drawable.pause);
		} else {
			nowPlaying = MusicUtils.getLastPlaying(getActivity());
			playPause.setImageResource(R.drawable.play);
		}
		if(nowPlaying != null) {
			Album album = Album.getAlbum(getActivity(), nowPlaying.getAlbum(), nowPlaying.getArtist());
			//        		TODO Un-comment to re-enable scaling of images (if out of memory errors ever occur)
			//				int dimen = context.getResources().getDimensionPixelSize(R.dimen.now_playing_bar_cover);
			int dimen = -1;
			AlbumAdapter.startAlbumArtTask(getActivity(), album, playing, dimen);
		} else {
			//TODO default now playing image
		}
	}
}
