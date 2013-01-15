package com.afollestad.overhear.fragments;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.MusicService;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SongAdapter;
import com.afollestad.overhearapi.Song;
import com.afollestad.overhearapi.Utils;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ListView;

public class SongListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

	private SongAdapter adapter;
	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(adapter != null)
				adapter.notifyDataSetChanged();
		}
	};


	public SongListFragment() {  }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
		getActivity().unregisterReceiver(mStatusReceiver);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		int pad = Utils.convertDpToPx(getActivity(), 20f);
		getListView().setPadding(pad, 0, pad, 0);
		getListView().setSmoothScrollbarEnabled(true);
		getListView().setFastScrollEnabled(true);
		setEmptyText(getString(R.string.no_songs));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		adapter.getCursor().moveToPosition(position);
		Song song = Song.fromCursor(adapter.getCursor());
		try {
			((MusicBoundActivity)getActivity()).getMusicService().playTrack(song);
		} catch(Exception e) {
			e.printStackTrace();
			Crouton.makeText(getActivity(), "Failed to play " + song.getTitle(), Style.ALERT).show();
		}
		adapter.notifyDataSetChanged();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String where = MediaStore.Audio.Media.IS_MUSIC + " = 1";
		String sort = MediaStore.Audio.Media.TITLE;
		if(getArguments() != null) {
			sort = MediaStore.Audio.Media.TRACK;
			if(getArguments().containsKey("artist_id")) {
				where += " AND " + MediaStore.Audio.Media.ARTIST_ID + " = " + getArguments().getInt("artist_id"); 			
			} else if(getArguments().containsKey("album_id")) {
				where += " AND " + MediaStore.Audio.Media.ALBUM_ID + " = " + getArguments().getInt("album_id");
			} else if(getArguments().containsKey("artist_name")) {
				where += " AND " + MediaStore.Audio.Media.ARTIST + " = " + getArguments().getString("artist_name").replace("'", "''");
			}
		}
		return new CursorLoader(getActivity(), 
				uri, 
				null, 
				where, 
				null, 
				sort);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
		if(data == null)
			return;
		adapter = new SongAdapter(getActivity(), data, 0);
		setListAdapter(adapter);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		if (adapter != null)
			adapter.changeCursor(null);
	}
}