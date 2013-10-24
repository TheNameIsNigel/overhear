package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.Overhear;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.tasks.LastfmGetArtistImage;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhear.utils.WebArtUtils;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.silk.adapters.SilkCursorAdapter;
import com.afollestad.silk.views.image.SilkImageView;

import java.util.ArrayList;

public class ArtistAdapter extends SilkCursorAdapter<Artist> {

    public ArtistAdapter(Activity context, Cursor c) {
        super(context, R.layout.artist_item, c, new CursorConverter<Artist>() {
            @Override
            public Artist convert(Cursor cursor) {
                return Artist.fromCursor(cursor);
            }
        });
    }

    public static void retrieveArtistArt(Activity context, Artist artist, SilkImageView view, boolean fitView) {
        view.setImageBitmap(null);
        String url = WebArtUtils.getImageURL(context, artist);
        if (url == null) {
            new LastfmGetArtistImage(context, view).execute(artist);
        } else {
            view.setFitView(fitView).setImageURL(Overhear.get(context).getManager(), url);
        }
    }

    public static void showPopup(final Activity context, final Artist artist, final View view) {
        PopupMenu menu = new PopupMenu(context, view);
        menu.inflate(R.menu.artist_item_popup);
        menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                switch (menuItem.getItemId()) {
                    case R.id.addToPlaylist: {
                        AlertDialog diag = MusicUtils.createPlaylistChooseDialog(context, null, null, artist);
                        diag.show();
                        return true;
                    }
                    case R.id.playAll: {
                        context.startService(new Intent(context, MusicService.class)
                                .setAction(MusicService.ACTION_PLAY_ALL)
                                .putExtra("scope", QueueItem.SCOPE_ARTIST)
                                .putExtra("artist", artist.getJSON().toString()));
                        return true;
                    }
                    case R.id.addToQueue: {
                        ArrayList<QueueItem> content = QueueItem.getAll(context,
                                MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " +
                                        MediaStore.Audio.Media.ARTIST + " = '" + artist.getName().replace("'", "''") + "'",
                                MediaStore.Audio.Media.ALBUM_KEY + ", " + MediaStore.Audio.Media.TRACK, -1, QueueItem.SCOPE_ARTIST);
                        MusicUtils.addToQueue(context, content);
                        return true;
                    }
                    case R.id.redownloadArt: {
                        new LastfmGetArtistImage(context, (SilkImageView) view.findViewById(R.id.image)).execute(artist);
                        return true;
                    }
                }
                return false;
            }
        });
        menu.show();
    }

    public static View getViewForArtistInfo(final Activity context, final LastFM.ArtistInfo info) {
        View view = LayoutInflater.from(context).inflate(R.layout.artist_item_info, null);
        ((TextView) view.findViewById(R.id.title)).setText(info.getName());
        final SilkImageView image = (SilkImageView) view.findViewById(R.id.image);
        image.setFitView(true).setImageURL(Overhear.get(context).getManager(), info.getBioImageURL());
        return view;
    }

    public static View getViewForArtist(final Activity context, final Artist artist, View view, boolean grid, int scrollState) {
        if (view == null) {
            if (grid)
                view = LayoutInflater.from(context).inflate(R.layout.artist_item, null);
            else
                view = LayoutInflater.from(context).inflate(R.layout.artist_item_nongrid, null);
        }

        ((TextView) view.findViewById(R.id.title)).setText(artist.getName());
        ((TextView) view.findViewById(R.id.stats)).setText(context.getString(R.string.artist_details)
                .replace("{albums}", "" + artist.getAlbumCount())
                .replace("{tracks}", "" + artist.getTrackCount()));

        final SilkImageView image = (SilkImageView) view.findViewById(R.id.image);
        if (scrollState > -1 && scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE)
            retrieveArtistArt(context, artist, image, true);
        else image.setImageBitmap(null);

        View options = view.findViewById(R.id.options);
        if (options != null) {
            options.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopup(context, artist, view);
                }
            });
        }

        ImageView peakOne = (ImageView) view.findViewById(R.id.peak_one);
        ImageView peakTwo = (ImageView) view.findViewById(R.id.peak_two);
        peakOne.setImageResource(R.anim.peak_meter_1);
        peakTwo.setImageResource(R.anim.peak_meter_2);
        AnimationDrawable mPeakOneAnimation = (AnimationDrawable) peakOne.getDrawable();
        AnimationDrawable mPeakTwoAnimation = (AnimationDrawable) peakTwo.getDrawable();

        QueueItem focused = null;
        boolean isPlaying = false;
        if (context instanceof OverhearActivity) {
            if (((OverhearActivity) context).getService() != null) {
                focused = ((OverhearActivity) context).getService().getQueue().getFocused();
                isPlaying = ((OverhearActivity) context).getService().isPlaying();
            }
        } else {
            if (((OverhearListActivity) context).getService() != null) {
                focused = ((OverhearListActivity) context).getService().getQueue().getFocused();
                isPlaying = ((OverhearListActivity) context).getService().isPlaying();
            }
        }

        if (focused != null && artist.getName().equals(focused.getArtist(context))) {
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
    public View onViewCreated(int index, View recycled, Artist item) {
        return getViewForArtist((Activity) getContext(), item, recycled, true, getScrollState());
    }
}