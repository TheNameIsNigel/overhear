package com.afollestad.overhear.adapters;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.base.Overhear;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhear.utils.ViewUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhear.utils.WebArtUtils;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.tasks.LastfmGetArtistImage;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;

public class ArtistAdapter extends SimpleCursorAdapter {

	public ArtistAdapter(Activity context, int layout, Cursor c, String[] from, int[] to, int flags) {
		super(context, layout, c, from, to, flags);
		this.context = context;
	}

	private Activity context;

	public static void retrieveArtistArt(Activity context, Artist artist, AImageView view) {
		view.setImageBitmap(null);
		String url = WebArtUtils.getImageURL(context, artist);
		if (url == null) {
			new LastfmGetArtistImage(context, view).execute(artist);
		} else {
			view.setManager(Overhear.get(context).getManager()).setSource(url).load();
		}
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		if (view == null) {
			if (convertView != null) {
				view = convertView;
			} else {
				view = newView(context, getCursor(), parent);
			}
		}

		final Artist artist = Artist.fromCursor(getCursor());
		((TextView) view.findViewById(R.id.title)).setText(artist.getName());
		((TextView) view.findViewById(R.id.artist)).setText(context.getString(R.string.artist_details)
				.replace("{albums}", "" + artist.getAlbumCount())
				.replace("{tracks}", "" + artist.getTrackCount()));

		final AImageView image = (AImageView) view.findViewById(R.id.image);
		retrieveArtistArt(context, artist, image);

		View options = view.findViewById(R.id.options);
		options.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
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
							.putExtra("artist", artist.getName()));
							return true;
						}
						case R.id.addToQueue: {
							ArrayList<Song> content = Song.getAllFromScope(context, new String[]{
									MediaStore.Audio.Media.IS_MUSIC + " = 1 AND " +
											MediaStore.Audio.Media.ARTIST + " = '" + artist.getName().replace("'", "''") + "'",
											MediaStore.Audio.Media.ALBUM});
							MusicUtils.addToQueue(context, content, QueueItem.SCOPE_ARTIST);
							return true;
						}
						case R.id.redownloadArt: {
							new LastfmGetArtistImage(context, image).execute(artist);
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

		if (focused != null && artist.getName().equals(focused.getArtist())) {
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

		int pad = context.getResources().getDimensionPixelSize(R.dimen.list_top_padding);
		if (position == 0) {
			if (getCount() == 1) {
				ViewUtils.relativeMargins(view, pad, pad);
			} else {
				ViewUtils.relativeMargins(view, pad, 0);
			}
		} else if (position == getCount() - 1) {
			ViewUtils.relativeMargins(view, 0, pad);
		} else {
			ViewUtils.relativeMargins(view, 0, 0); 
		}

		return view;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.album_artist_item, null);
	}
}