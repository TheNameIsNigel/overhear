package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.CursorAdapter;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SongAdapter;
import com.afollestad.overhear.base.OverhearListFragment;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Genre;
import com.afollestad.overhearapi.Song;

public class SongListFragment extends OverhearListFragment {

	private SongAdapter adapter;
	private Genre genre;

	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (adapter != null)
				adapter.notifyDataSetChanged();
		}
	};


	public SongListFragment() {
	}

	@Override
	public Uri getLoaderUri() {
		Uri uri = null;
		if (getArguments() != null && getArguments().containsKey("genre")) {
			genre = Genre.fromJSON(getArguments().getString("genre"));
			uri = MediaStore.Audio.Genres.Members.getContentUri("external", genre.getId());
		} else {
			uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		}
		return uri;
	}

	@Override
	public String getLoaderSelection() {
		String[] scope = null;
		if (genre != null) {
			scope = new String[]{null, null};
		} else {
			scope = getScope(null);
		}
		return scope[0];
	}

	@Override
	public String getLoaderSort() {
		String[] scope = null;
		if (genre != null) {
			scope = new String[]{null, null};
		} else {
			scope = getScope(null);
		}
		return scope[1];
	}

	@Override
	public CursorAdapter getAdapter() {
		if (adapter == null) {
			adapter = new SongAdapter(getActivity(), null, 0);
		}
		return adapter;
	}

	@Override
	public BroadcastReceiver getReceiver() {
		return mStatusReceiver;
	}

	@Override
	public IntentFilter getFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(MusicService.PLAYING_STATE_CHANGED);
		return filter;
	}

	@Override
	public String getEmptyText() {
		return getString(R.string.no_songs);
	}

	@Override
	public void onItemClick(int position, Cursor cursor) {
		Song song = Song.fromCursor(adapter.getCursor());
		Album album = null;
		Artist artist = null;
		if(getArguments() != null) {
			if(getArguments().containsKey("album"))
				album = Album.fromJSON(getArguments().getString("album"));
			if(getArguments().containsKey("artist"))
				artist = Artist.fromJSON(getArguments().getString("artist"));
		}
		performOnClick(getActivity(), song, album, artist, genre, getScopeInt(), position);
	}

	@Override
	public void onInitialize() {
		if(getArguments() != null && getArguments().containsKey("genre"))
			genre = Genre.fromJSON(getArguments().getString("genre"));
	}


	public static void performOnClick(Activity context, Song song, Album album, Artist artist, Genre genre, int scope, int position) {
		Intent intent = new Intent(context, MusicService.class)
		.setAction(MusicService.ACTION_PLAY_ALL)
		.putExtra("song", song.getJSON().toString())
		.putExtra("scope", scope)
		.putExtra("position", position);
		if(album != null)
			intent.putExtra("album", album.getJSON().toString());
		if(artist != null)
			intent.putExtra("artist", artist.getJSON().toString());
		if(genre != null)
			intent.putExtra("genre", genre.getJSON().toString());
		context.startService(intent);
	}

	private String[] getScope(Song genreSong) {
		String sort = MediaStore.Audio.Media.TITLE;
		String where = MediaStore.Audio.Media.IS_MUSIC + " = 1";

		if (getArguments() != null) {
			switch(getScopeInt()) {
			case QueueItem.SCOPE_All_SONGS: {
				break;
			}
			case QueueItem.SCOPE_ARTIST: {
				Artist artist = Artist.fromJSON(getArguments().getString("artist"));
				sort = MediaStore.Audio.Media.ALBUM;
				where += " AND " + MediaStore.Audio.Media.ARTIST_ID + " = " + artist.getId();
				break;
			}
			case QueueItem.SCOPE_ALBUM: {
				Album album = Album.fromJSON(getArguments().getString("album"));
				sort = MediaStore.Audio.Media.TRACK;
				where += " AND " + MediaStore.Audio.Media.ALBUM_ID + " = " + album.getAlbumId();
				break;
			}
			case QueueItem.SCOPE_GENRE: {
				sort = null;
				break;
			}
			}
		}

		return new String[]{where, sort};
	}

	public int getScopeInt() {
		if (getArguments() != null) {
			if (getArguments().containsKey("artist")) {
				return QueueItem.SCOPE_ARTIST;
			} else if (getArguments().containsKey("album")) {
				return QueueItem.SCOPE_ALBUM;
			} else if (getArguments().containsKey("genre")) {
				return QueueItem.SCOPE_GENRE;
			} else {
				return QueueItem.SCOPE_All_SONGS;
			}
		} else {
			return QueueItem.SCOPE_All_SONGS;
		}
	}
}