package com.afollestad.overhear;

import java.util.Locale;

import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Utils;

import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;

public class OverviewScreen extends FragmentActivity {

	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setCurrentItem(1); //Default to albums page
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		menu.findItem(R.id.nowPlayingAction).setIcon(new BitmapDrawable(getResources(), 
				Album.getAllAlbums(this).get(7).getAlbumArt(this, 56f, 56f)));
		return true;
	}

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch(position) {
			case 0:
				return new AllSongsListFragment();
			case 1:
				return new AlbumListFragment();
			case 2:
				return new ArtistListFragment();
			case 3:
				return new GenreListFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 4;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.songs_str).toUpperCase(Locale.getDefault());
			case 1:
				return getString(R.string.albums_str).toUpperCase(Locale.getDefault());
			case 2:
				return getString(R.string.artists_str).toUpperCase(Locale.getDefault());
			case 3:
				return getString(R.string.genres_str).toUpperCase(Locale.getDefault());
			}
			return null;
		}
	}

	public static class AlbumListFragment extends ListFragment {
		
		private AlbumAdapter adapter;
		
		public AlbumListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			adapter = new AlbumAdapter(getActivity(), null);
			setListAdapter(adapter);
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
	}
	
	public static class ArtistListFragment extends Fragment {
		
		private ArtistAdapter adapter;
		
		public ArtistListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			adapter = new ArtistAdapter(getActivity());
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			View toreturn = inflater.inflate(R.layout.grid_fragment, null);
			((GridView)toreturn.findViewById(R.id.gridView)).setAdapter(adapter);
			adapter.notifyDataSetChanged();
			return toreturn;
		}
		
		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
		}
	}
	
	public static class AllSongsListFragment extends ListFragment {
		
		private SongAdapter adapter;
		
		public AllSongsListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			adapter = new SongAdapter(getActivity(), null);
			setListAdapter(adapter);
			adapter.notifyDataSetChanged();
		}
		
		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			int pad = Utils.convertDpToPx(getActivity(), 15f);
			getListView().setPadding(pad, 0, pad, 0);
			getListView().setSmoothScrollbarEnabled(true);
			getListView().setFastScrollEnabled(true);
			setEmptyText(getString(R.string.no_songs));
		}
	}
	
	public static class GenreListFragment extends ListFragment {
		
		private GenreAdapter adapter;
		
		public GenreListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			adapter = new GenreAdapter(getActivity());
			setListAdapter(adapter);
			adapter.notifyDataSetChanged();
		}
		
		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			int pad = Utils.convertDpToPx(getActivity(), 15f);
			getListView().setPadding(pad, 0, pad, 0);
			getListView().setSmoothScrollbarEnabled(true);
			getListView().setFastScrollEnabled(true);
			setEmptyText(getString(R.string.no_genres));
		}
	}
}