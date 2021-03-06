package com.afollestad.overhear.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhear.ui.NowPlayingViewer;
import com.afollestad.overhear.ui.PlaylistViewer;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.silk.fragments.SilkFragment;
import com.afollestad.silk.views.image.SilkImageView;

import java.lang.ref.WeakReference;

/**
 * A completely self-sufficient now playing bar, displayed on the bottom of any activity that has music controls.
 *
 * @author Aidan Follestad
 */
public class NowPlayingBarFragment extends SilkFragment {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            update();
        }
    };
    private Album album;
    private Playlist playlist;
    private QueueItem focused;
    private WeakReference<SilkImageView> playing;
    private WeakReference<ImageView> playPause;
    private WeakReference<ImageView> previous;
    private WeakReference<ImageView> next;
    private WeakReference<TextView> track;
    private WeakReference<TextView> artist;

    public NowPlayingBarFragment() {
    }

    @Override
    public void onCreate(Bundle sis) {
        super.onCreate(sis);
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        getActivity().registerReceiver(mStatusReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (getActivity() != null) {
            try {
                getActivity().unregisterReceiver(mStatusReceiver);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected int getLayout() {
        return R.layout.now_playing_bar;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        playing = new WeakReference<SilkImageView>((SilkImageView) view.findViewById(R.id.playing));
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
        getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (focused == null) {
                    return;
                }
                startActivity(new Intent(getActivity(), NowPlayingViewer.class));
            }
        });
        getView().setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (focused == null)
                    return true;
                if (playlist != null) {
                    if (getActivity() instanceof PlaylistViewer &&
                            playlist.getId() != focused.getPlaylistId()) {
                        return false;
                    }
                    startActivity(new Intent(getActivity(), PlaylistViewer.class)
                            .putExtra("playlist", playlist.getJSON().toString()));
                } else if (album != null) {
                    if (getActivity() instanceof AlbumViewer &&
                            (!album.getName().equals(focused.getAlbum(getActivity())) ||
                                    !album.getArtist().getName().equals(focused.getArtist(getActivity())))) {
                        return false;
                    }
                    startActivity(new Intent(getActivity(), AlbumViewer.class)
                            .putExtra("album", album.getJSON().toString()));
                }
                return false;
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
        if (getActivity() == null)
            return;
        boolean isPlaying = false;
        WeakReference<QueueItem> lastPlayed = new WeakReference<QueueItem>(focused);
        focused = null;

        if (getActivity() instanceof OverhearActivity) {
            if (((OverhearActivity) getActivity()).getService() != null) {
                focused = ((OverhearActivity) getActivity()).getService().getQueue().getFocused();
                isPlaying = ((OverhearActivity) getActivity()).getService().isPlaying();
            }
        } else {
            if (((OverhearListActivity) getActivity()).getService() != null) {
                focused = ((OverhearListActivity) getActivity()).getService().getQueue().getFocused();
                isPlaying = ((OverhearListActivity) getActivity()).getService().isPlaying();
            }
        }

        if (focused != null) {
            if (isPlaying)
                playPause.get().setImageResource(R.drawable.ic_pause);
            else
                playPause.get().setImageResource(R.drawable.ic_play);
            previous.get().setEnabled(true);
            next.get().setEnabled(true);

            if (focused.getPlaylistId() > 0)
                playlist = Playlist.get(getActivity(), focused.getPlaylistId());

            boolean albumChanged = true;
            if (lastPlayed != null && lastPlayed.get() != null &&
                    lastPlayed.get().getAlbum(getActivity()).equals(focused.getAlbum(getActivity())) &&
                    lastPlayed.get().getArtist(getActivity()).equals(focused.getArtist(getActivity()))) {
                albumChanged = false;
            }

            if (albumChanged || playing.get().getDrawable() == null) {
                album = Album.getAlbum(getActivity(), focused.getAlbum(getActivity()), focused.getArtist(getActivity()));
                playing.get().setImageBitmap(null);
                AlbumAdapter.retrieveAlbumArt(getActivity(), album, playing.get());
            }

            track.get().setText(focused.getTitle(getActivity()));
            artist.get().setText(focused.getArtist(getActivity()));
            return;
        }

        playPause.get().setImageResource(R.drawable.ic_play);
        previous.get().setEnabled(false);
        next.get().setEnabled(false);
    }
}