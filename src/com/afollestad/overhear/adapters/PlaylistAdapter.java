package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.fragments.PlaylistSongFragment;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.overhearapi.Song;

import java.util.ArrayList;

public class PlaylistAdapter extends CursorAdapter {

    public PlaylistAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        this.activity = (Activity)context;
    }

    private Activity activity;

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.song_item, null);
    }

    public static View getViewForPlaylist(final Activity context, final Playlist playlist, View view, final int position) {
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.song_item, null);
        }

        ((TextView) view.findViewById(R.id.title)).setText(playlist.getName());

        View options = view.findViewById(R.id.options);
        options.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PopupMenu menu = new PopupMenu(context, view);
                menu.inflate(R.menu.playlist_item_popup);
                final boolean isFavorites = playlist.getName().equals(context.getString(R.string.favorites_str)); 
                if(isFavorites)
                	menu.getMenu().findItem(R.id.delete).setTitle(R.string.clear_str);
                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        final ArrayList<Song> listSongs = playlist.getSongs(context, null);
                        switch (menuItem.getItemId()) {
                            case R.id.playAll: {
                                PlaylistSongFragment.performClick(context, listSongs.get(0), playlist, position);
                                return true;
                            }
                            case R.id.addToQueue: {
                            	MusicUtils.addToQueue(context, listSongs, QueueItem.SCOPE_PLAYLIST);
                                return true;
                            }
                            case R.id.rename: {
                                MusicUtils.createNewPlaylistDialog(context, playlist).show();
                                return true;
                            }
                            case R.id.delete: {
                            	if(isFavorites)
                            		playlist.clear(context);
                            	else
                            		MusicUtils.createPlaylistDeleteDialog(context, playlist).show();
                                return true;
                            }
                        }
                        return false;
                    }
                });
                menu.show();
            }
        });

        ImageView peakOne = (ImageView) view.findViewById(R.id.peak_one);
        ImageView peakTwo = (ImageView) view.findViewById(R.id.peak_two);
        peakOne.setImageResource(R.anim.peak_meter_1);
        peakTwo.setImageResource(R.anim.peak_meter_2);
        AnimationDrawable mPeakOneAnimation = (AnimationDrawable) peakOne.getDrawable();
        AnimationDrawable mPeakTwoAnimation = (AnimationDrawable) peakTwo.getDrawable();

        Song focused = null;
        boolean isPlaying = false; 
        if(context instanceof OverhearActivity) {
			if(((OverhearActivity)context).getService() != null) {
				focused = ((OverhearActivity)context).getService().getQueue().getFocused();
				isPlaying = ((OverhearActivity)context).getService().isPlaying();
			}
		} else {
			if(((OverhearListActivity)context).getService() != null) {
				focused = ((OverhearListActivity)context).getService().getQueue().getFocused();
				isPlaying = ((OverhearListActivity)context).getService().isPlaying();
			}
		}
        
        if (focused != null && playlist.getId() == focused.getPlaylistId()) {
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

        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Playlist playlist = Playlist.fromCursor(cursor);
        getViewForPlaylist(activity, playlist, view, cursor.getPosition());
    }
}