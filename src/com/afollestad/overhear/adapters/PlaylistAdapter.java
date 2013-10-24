package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.fragments.PlaylistSongFragment;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.silk.adapters.SilkCursorAdapter;

import java.util.ArrayList;

public class PlaylistAdapter extends SilkCursorAdapter<Playlist> {

    public PlaylistAdapter(Activity context, Cursor c) {
        super(context, R.layout.song_item, c, new CursorConverter<Playlist>() {
            @Override
            public Playlist convert(Cursor cursor) {
                return Playlist.fromCursor(cursor);
            }
        });
    }

    @Override
    public View onViewCreated(final int index, View recycled, final Playlist item) {
        ((TextView) recycled.findViewById(R.id.title)).setText(item.getName());
        View options = recycled.findViewById(R.id.options);
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(getContext(), view);
                menu.inflate(R.menu.playlist_item_popup);
                final boolean isFavorites = item.getName().equals(getContext().getString(R.string.favorites_str));
                if (isFavorites)
                    menu.getMenu().findItem(R.id.delete).setTitle(R.string.clear_str);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        final ArrayList<Integer> listSongs = item.getSongs(getContext(), null);
                        switch (menuItem.getItemId()) {
                            case R.id.playAll: {
                                PlaylistSongFragment.performClick((Activity) getContext(), listSongs.get(0), item, index);
                                return true;
                            }
                            case R.id.addToQueue: {
                                MusicUtils.addToQueue((Activity) getContext(), QueueItem.getAllFromIds(listSongs, item.getId(), QueueItem.SCOPE_PLAYLIST));
                                return true;
                            }
                            case R.id.rename: {
                                MusicUtils.createNewPlaylistDialog((Activity) getContext(), item).show();
                                return true;
                            }
                            case R.id.delete: {
                                if (isFavorites)
                                    MusicUtils.createPlaylistClearDialog((Activity) getContext(), item).show();
                                else
                                    MusicUtils.createPlaylistDeleteDialog((Activity) getContext(), item).show();
                                return true;
                            }
                        }
                        return false;
                    }
                });
                menu.show();
            }
        });

        ImageView peakOne = (ImageView) recycled.findViewById(R.id.peak_one);
        ImageView peakTwo = (ImageView) recycled.findViewById(R.id.peak_two);
        peakOne.setImageResource(R.anim.peak_meter_1);
        peakTwo.setImageResource(R.anim.peak_meter_2);
        AnimationDrawable mPeakOneAnimation = (AnimationDrawable) peakOne.getDrawable();
        AnimationDrawable mPeakTwoAnimation = (AnimationDrawable) peakTwo.getDrawable();

        QueueItem focused = null;
        boolean isPlaying = false;
        if (getContext() instanceof OverhearActivity) {
            if (((OverhearActivity) getContext()).getService() != null) {
                focused = ((OverhearActivity) getContext()).getService().getQueue().getFocused();
                isPlaying = ((OverhearActivity) getContext()).getService().isPlaying();
            }
        } else {
            if (((OverhearListActivity) getContext()).getService() != null) {
                focused = ((OverhearListActivity) getContext()).getService().getQueue().getFocused();
                isPlaying = ((OverhearListActivity) getContext()).getService().isPlaying();
            }
        }

        if (focused != null && item.getId() == focused.getPlaylistId()) {
            peakOne.setVisibility(View.VISIBLE);
            peakTwo.setVisibility(View.VISIBLE);
            if (isPlaying) {
                if (!mPeakOneAnimation.isRunning()) {
                    mPeakOneAnimation.start();
                    mPeakTwoAnimation.start();
                }
            } else {
                mPeakOneAnimation.stop();
                mPeakOneAnimation.selectDrawable(0);
                mPeakTwoAnimation.stop();
                mPeakTwoAnimation.selectDrawable(0);
            }
        } else {
            peakOne.setVisibility(View.GONE);
            peakTwo.setVisibility(View.GONE);
            if (mPeakOneAnimation.isRunning()) {
                mPeakOneAnimation.stop();
                mPeakTwoAnimation.stop();
            }
        }

        return recycled;
    }
}