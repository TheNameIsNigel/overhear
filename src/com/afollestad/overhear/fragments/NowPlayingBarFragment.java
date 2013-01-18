package com.afollestad.overhear.fragments;

import java.lang.ref.WeakReference;

import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.QueueUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;
import com.androidquery.AQuery;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

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

	private WeakReference<View> viewPlaying;
	private WeakReference<ImageView> playing;
	private WeakReference<ImageView> playPause;
	private WeakReference<ImageView> previous;
	private WeakReference<ImageView> next;
	private WeakReference<TextView> track;
	private WeakReference<TextView> artist;
	private WeakReference<Song> lastPlayed;

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
		viewPlaying = new WeakReference<View>(view.findViewById(R.id.viewPlaying));
		playing = new WeakReference<ImageView>((ImageView)view.findViewById(R.id.playing));
		playPause = new WeakReference<ImageView>((ImageView)view.findViewById(R.id.play));
		previous = new WeakReference<ImageView>((ImageView)view.findViewById(R.id.previous));
		next = new WeakReference<ImageView>((ImageView)view.findViewById(R.id.next));
		track = new WeakReference<TextView>((TextView)view.findViewById(R.id.playingTrack));
		artist = new WeakReference<TextView>((TextView)view.findViewById(R.id.playingArtist));
		initialize();
	}

	private void initialize() {
		playPause.get().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				getActivity().startService(new Intent(getActivity(), MusicService.class)
				.setAction(MusicService.ACTION_TOGGLE_PLAYBACK));
			}
		});
		viewPlaying.get().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				//TODO open now playing screen 
			}
		});
		next.get().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				getActivity().startService(new Intent(getActivity(), MusicService.class)
				.setAction(MusicService.ACTION_SKIP));
			}
		});
		previous.get().setOnClickListener(new View.OnClickListener() {
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
			playPause.get().setImageResource(R.drawable.pause);
		} else {
			nowPlaying = QueueUtils.poll(getActivity());
			playPause.get().setImageResource(R.drawable.play);
		}
		if(nowPlaying != null) {
			previous.get().setEnabled(true);
			next.get().setEnabled(true);

			if(lastPlayed == null || lastPlayed.get() == null || 
					(!lastPlayed.get().getAlbum().equals(nowPlaying.getAlbum()) ||
					!lastPlayed.get().getArtist().equals(nowPlaying.getArtist()))) {
				
				Album album = Album.getAlbum(getActivity(), nowPlaying.getAlbum(), nowPlaying.getArtist());
				AQuery aq = new AQuery(getActivity());
				Bitmap art = aq.getCachedImage(MusicUtils.getImageURL(getActivity(), 
						album.getName() + ":" + album.getArtist().getName(), AlbumAdapter.ALBUM_IMAGE));
				playing.get().setImageBitmap(art);
			}

			track.get().setText(nowPlaying.getTitle());
			artist.get().setText(nowPlaying.getArtist());
			lastPlayed = new WeakReference<Song>(nowPlaying);
		} else {
			lastPlayed = null;
			previous.get().setEnabled(false);
			next.get().setEnabled(false);
			//TODO default now playing image
		}
	}
}
