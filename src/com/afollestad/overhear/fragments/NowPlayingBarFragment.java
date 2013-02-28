package com.afollestad.overhear.fragments;

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
import android.widget.TextView;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhear.ui.NowPlayingViewer;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

import java.lang.ref.WeakReference;

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

    public NowPlayingBarFragment() {
    }
    
    private Album album;
    private Song focused;

    private WeakReference<View> viewPlaying;
    private WeakReference<AImageView> playing;
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
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        update();
    }

    @Override
    public void onDestroy() {
        if(getActivity() != null) {
            try {
                getActivity().unregisterReceiver(mStatusReceiver);
            } catch(Exception e) {
                //Supress
            }
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.now_playing_bar, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        playing = new WeakReference<AImageView>((AImageView) view.findViewById(R.id.playing));
        viewPlaying = new WeakReference<View>(view.findViewById(R.id.viewPlaying));
        playPause = new WeakReference<ImageView>((ImageView) view.findViewById(R.id.play));
        previous = new WeakReference<ImageView>((ImageView) view.findViewById(R.id.previous));
        next = new WeakReference<ImageView>((ImageView) view.findViewById(R.id.next));
        track = new WeakReference<TextView>((TextView) view.findViewById(R.id.playingTrack));
        artist = new WeakReference<TextView>((TextView) view.findViewById(R.id.playingArtist));
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
                if (focused == null) {
                    return;
                }
                startActivity(new Intent(getActivity(), NowPlayingViewer.class));
            }
        });
        if (getArguments() == null || !getArguments().getBoolean("disable_long_click", false)) {
            viewPlaying.get().setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (focused == null) {
                        return true;
                    }
                    startActivity(new Intent(getActivity(), AlbumViewer.class)
                            .putExtra("album", album.getJSON().toString()));
                    return false;
                }
            });
        }
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
        previous.get().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                getActivity().startService(new Intent(getActivity(), MusicService.class)
                        .setAction(MusicService.ACTION_REWIND).putExtra("override", true));
                return true;
            }
        });
    }

    public void update() {
        if (getActivity() == null) {
            return;
        }
        
        boolean isPlaying = false; 
        if(getActivity() instanceof OverhearActivity) {
        	focused = ((OverhearActivity)getActivity()).getService().getQueue().getFocused();
        	isPlaying = ((OverhearActivity)getActivity()).getService().isPlaying();
        } else {
        	focused = ((OverhearListActivity)getActivity()).getService().getQueue().getFocused();
        	isPlaying = ((OverhearListActivity)getActivity()).getService().isPlaying();
        }
        
        if (focused != null && isPlaying) {
            playPause.get().setImageResource(R.drawable.pause);
        } else {
            playPause.get().setImageResource(R.drawable.play);
        }
        if (focused != null) {
            previous.get().setEnabled(true);
            next.get().setEnabled(true);

            if (lastPlayed == null || lastPlayed.get() == null ||
                    (!lastPlayed.get().getAlbum().equals(focused.getAlbum()) ||
                            !lastPlayed.get().getArtist().equals(focused.getArtist())) ||
                    playing.get().getDrawable() == null) {
                album = Album.getAlbum(getActivity(), focused.getAlbum(), focused.getArtist());
                if(album != null) {
                    AlbumAdapter.retrieveAlbumArt(getActivity(), album, playing.get());
                }
            }

            track.get().setText(focused.getTitle());
            artist.get().setText(focused.getArtist());
            lastPlayed = new WeakReference<Song>(focused);
        } else {
            lastPlayed = null;
            previous.get().setEnabled(false);
            next.get().setEnabled(false);
        }
    }
}