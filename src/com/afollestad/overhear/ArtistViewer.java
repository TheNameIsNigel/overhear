package com.afollestad.overhear;

import java.lang.ref.WeakReference;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import com.afollestad.overhear.R.string;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.overhearapi.LastFM.ArtistInfo;
import com.afollestad.overhearapi.Utils;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ArtistViewer extends FragmentActivity {

	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	public Artist artist;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_artist_viewer);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		try {
			artist = Artist.fromJSON(new JSONObject(getIntent().getStringExtra("artist")));
		} catch (JSONException e) {
			throw new Error(e.getMessage());
		}
		ArtistAdapter.loadArtistPicture(this, artist, new WeakReference<ImageView>(
				(ImageView)findViewById(R.id.cover)), 360f, 180f);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_artist_viewer, menu);
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
				return new BioListFragment();
			case 1:
				return new AlbumListFragment();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.bio_str).toUpperCase(Locale.getDefault());
			case 1:
				return getString(R.string.albums_str).toUpperCase(Locale.getDefault());
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
			String artist = ((ArtistViewer)getActivity()).artist.getName();
			adapter = new AlbumAdapter(getActivity(), artist);
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
	
	public static class BioListFragment extends Fragment {
		
		public BioListFragment() {  }

		private Artist artist;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			artist = ((ArtistViewer)getActivity()).artist;
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			return inflater.inflate(R.layout.bio_fragment, null);
		}
		
		private void load() {
			final Handler mHandler = new Handler();
			new Thread(new Runnable() {
				public void run() {
					try {
						final ArtistInfo info = LastFM.getArtistInfo(artist.getName());
						mHandler.post(new Runnable() {
							public void run() {
								if(getView() != null) {
									((TextView)getView().findViewById(R.id.bioAbout)).setText(info.getBioSummary());
								}
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
						mHandler.post(new Runnable() {
							public void run() {
								Crouton.makeText(getActivity(), R.string.failed_load_artist_bio, Style.ALERT);
							}
						});
					}
				}
			}).start();
		}
		
		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			load();
		}
	}
}
