package com.afollestad.overhear;

import java.lang.ref.WeakReference;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;

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
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_artist_viewer);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		try {
			artist = Artist.fromJSON(new JSONObject(getIntent().getStringExtra("artist")));
		} catch (JSONException e) {
			throw new Error(e.getMessage());
		}
		setTitle(artist.getName());
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
				return new AlbumListFragment();
			case 1:
				return new BioListFragment();
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
				return getString(R.string.albums_str).toUpperCase(Locale.getDefault());
			case 1:
				return getString(R.string.about_str).toUpperCase(Locale.getDefault());
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
									((TextView)getView().findViewById(R.id.bioAbout)).setText(
											Html.fromHtml(info.getBioSummary()));
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

					try {
						Twitter twitter = TwitterFactory.getSingleton();
						ResponseList<User> possibleUsers = twitter.searchUsers(artist.getName(), 0);
						if(possibleUsers.size() == 0) {
							System.out.println("No Twitter users results found!");
						}
						for(int i = 0; i < 4; i++) {
							System.out.println(possibleUsers.get(i).getScreenName() + " is a match?");
							if(possibleUsers.get(i).isVerified()) {
								final User user = possibleUsers.get(i);
								mHandler.post(new Runnable() {
									public void run() {
										if(getView() != null) {
											((TextView)getView().findViewById(R.id.bioUpdates)).setText(
													user.getStatus().getText());
											String source = getString(R.string.social_update_source)
													.replace("{time}", Utils.getFriendlyTime(user.getStatus().getCreatedAt()))
													.replace("{user}", "@" + user.getScreenName())
													.replace("{network}", "Twitter");
											((TextView)getView().findViewById(R.id.bioUpdateSource)).setText(source);
										}
									}
								});
								break;
							}
						}
					} catch(Exception e) {
						e.printStackTrace();
						mHandler.post(new Runnable() {
							public void run() {
								Crouton.makeText(getActivity(), R.string.failed_load_artist_updates, Style.ALERT);
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
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpTo(this, new Intent(this, OverviewScreen.class));
			return true;
		}
		return false;
	}
}
