package com.afollestad.overhear;

import java.util.Locale;

import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.adapters.GenreAdapter;
import com.afollestad.overhear.adapters.SongAdapter;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Genre;
import com.afollestad.overhearapi.Song;
import com.afollestad.overhearapi.Utils;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

public class OverviewScreen extends MusicBoundActivity {

	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	NowPlayingBar nowPlaying;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setCurrentItem(1);
	}

	@Override
	public void onPause() {
		super.onPause();
		if(isServiceBound()) {  
			getMusicService().saveRecents();
		}
	}

	public class SectionsPagerAdapter extends TaggedFragmentAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch(position) {
			case 0:
				return new RecentsListFragment();
			case 1:
				return new ArtistListFragment();
			case 2:
				return new AlbumListFragment();
			case 3:
				return new AllSongsListFragment();
			case 4:
				return new GenreListFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 5;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.recent_str).toUpperCase(Locale.getDefault());
			case 1:
				return getString(R.string.artists_str).toUpperCase(Locale.getDefault());
			case 2:
				return getString(R.string.albums_str).toUpperCase(Locale.getDefault());
			case 3:
				return getString(R.string.songs_str).toUpperCase(Locale.getDefault());
			case 4:
				return getString(R.string.genres_str).toUpperCase(Locale.getDefault());
			}
			return null;
		}
	}

	public static class RecentsListFragment extends MusicListFragment {

		private AlbumAdapter adapter;

		public RecentsListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			adapter = new AlbumAdapter((MusicBoundActivity)getActivity(), null);
			setListAdapter(adapter);
			adapter.loadRecents();
		}

		@Override
		public void onResume() {
			super.onResume();
			adapter.notifyDataSetChanged();
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			int pad = Utils.convertDpToPx(getActivity(), 20f);
			getListView().setPadding(pad, 0, pad, 0);
			getListView().setSmoothScrollbarEnabled(true);
			getListView().setFastScrollEnabled(true);
			setEmptyText(getString(R.string.no_recents));
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			Album album = (Album)adapter.getItem(position);
			startActivity(new Intent(getActivity(), AlbumViewer.class)
			.putExtra("album", album.getJSON().toString()));
		}

		@Override
		public void update() {
			if(adapter != null) {
				if(adapter.isEmpty())
					adapter.loadRecents();
				else
					adapter.notifyDataSetChanged();
			}
		}
	}

	public static class AlbumListFragment extends MusicListFragment {

		private AlbumAdapter adapter;

		public AlbumListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			adapter = new AlbumAdapter((MusicBoundActivity)getActivity(), null);
			setListAdapter(adapter);
			adapter.loadAlbums();
		}

		@Override
		public void onResume() {
			super.onResume();
			adapter.notifyDataSetChanged();
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			int pad = Utils.convertDpToPx(getActivity(), 20f);
			getListView().setPadding(pad, 0, pad, 0);
			getListView().setSmoothScrollbarEnabled(true);
			getListView().setFastScrollEnabled(true);
			setEmptyText(getString(R.string.no_albums));
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			Album album = (Album)adapter.getItem(position);
			startActivity(new Intent(getActivity(), AlbumViewer.class)
			.putExtra("album", album.getJSON().toString()));
		}

		@Override
		public void update() {
			if(adapter != null)
				adapter.notifyDataSetChanged();
		}
	}

	public static class ArtistListFragment extends MusicFragment {

		private ArtistAdapter adapter;

		public ArtistListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			adapter = new ArtistAdapter((MusicBoundActivity)getActivity());
		}

		@Override
		public void onResume() {
			super.onResume();
			adapter.notifyDataSetChanged();
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			GridView grid = (GridView)inflater.inflate(R.layout.grid_fragment, null);
			grid.setAdapter(adapter);
			grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> arg0, View view, int index, long id) {
					Artist artist = (Artist)adapter.getItem(index);
					startActivity(new Intent(getActivity(), ArtistViewer.class)
					.putExtra("artist", artist.getJSON().toString()));
				}
			});
			adapter.loadArtists();
			return grid;
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
		}

		@Override
		public void update() {
			if(adapter != null)
				adapter.notifyDataSetChanged();
		}
	}

	public static class AllSongsListFragment extends MusicListFragment {

		private SongAdapter adapter;

		public AllSongsListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			adapter = new SongAdapter((MusicBoundActivity)getActivity(), null, null);
			setListAdapter(adapter);
			adapter.loadSongs();
		}

		@Override
		public void onResume() {
			super.onResume();
			adapter.notifyDataSetChanged();
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
			Song song = (Song)adapter.getItem(position);
			try {
				((MusicBoundActivity)getActivity()).getMusicService().playTrack(song);
			} catch(Exception e) {
				e.printStackTrace();
				Crouton.makeText(getActivity(), "Failed to play " + song.getTitle(), Style.ALERT);
			}
			adapter.notifyDataSetChanged();
		}

		@Override
		public void update() {
			if(adapter != null)
				adapter.notifyDataSetChanged();
		}
	}

	public static class GenreListFragment extends MusicListFragment {

		private GenreAdapter adapter;

		public GenreListFragment() { }
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			adapter = new GenreAdapter(getActivity());
			setListAdapter(adapter);
			adapter.loadGenres();
		}

		@Override
		public void onResume() {
			super.onResume();
			adapter.notifyDataSetChanged();
		}

		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			int pad = Utils.convertDpToPx(getActivity(), 20f);
			getListView().setPadding(pad, 0, pad, 0);
			getListView().setSmoothScrollbarEnabled(true);
			getListView().setFastScrollEnabled(true);
			setEmptyText(getString(R.string.no_genres));
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			startActivity(new Intent(getActivity(), GenreViewer.class)
					.putExtra("genre", ((Genre)adapter.getItem(position)).getJSON().toString()));
		}
		
		@Override
		public void update() {
			if(adapter != null)
				adapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onBound() {
		nowPlaying = NowPlayingBar.get(this);
		updateFragments();
	}
	
	private void updateFragments() {
		for(int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			Fragment frag = getFragmentManager().findFragmentByTag("page:" + i);
			if(frag instanceof MusicFragment) {
				((MusicFragment)frag).update();
			} else if(frag instanceof MusicListFragment) {
				((MusicListFragment)frag).update();
			}
		}
	}

	
	@Override
	public void onNowPlayingUpdate() {
		updateFragments();
	}
}