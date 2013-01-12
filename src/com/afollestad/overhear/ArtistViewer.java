package com.afollestad.overhear;

import java.lang.ref.WeakReference;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.User;

import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.overhearapi.Song;
import com.afollestad.overhearapi.LastFM.ArtistInfo;
import com.afollestad.overhearapi.Utils;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class ArtistViewer extends MusicBoundActivity {

	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;
	public Artist artist;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_artist_viewer);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(2);
		mViewPager.setCurrentItem(1);
		try {
			artist = Artist.fromJSON(new JSONObject(getIntent().getStringExtra("artist")));
		} catch (JSONException e) {
			throw new Error(e.getMessage());
		}
		setTitle(artist.getName());
		ArtistAdapter.loadArtistPicture(this, artist, new WeakReference<ImageView>(
				(ImageView)findViewById(R.id.cover)), 180f, 180f);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_artist_viewer, menu);
		return true;
	}
	
	public class SectionsPagerAdapter extends TaggedFragmentAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch(position) {
			case 0:
				return new SongListFragment();
			case 1:
				return new AlbumListFragment();
			case 2:
				return new BioListFragment();				
			}
			return null;
		}

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (position) {
			case 0:
				return getString(R.string.songs_str).toUpperCase(Locale.getDefault());
			case 1:
				return getString(R.string.albums_str).toUpperCase(Locale.getDefault());
			case 2:
				return getString(R.string.about_str).toUpperCase(Locale.getDefault());
			}
			return null;
		}
	}

	public static class AlbumListFragment extends MusicListFragment {

		private AlbumAdapter adapter;

		public AlbumListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			String artist = ((ArtistViewer)getActivity()).artist.getName();
			adapter = new AlbumAdapter((MusicBoundActivity)getActivity(), artist);
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
	
	public static class SongListFragment extends MusicListFragment {

		private SongAdapter adapter;

		public SongListFragment() {  }

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			setRetainInstance(true);
			String artist = ((ArtistViewer)getActivity()).artist.getName();
			adapter = new SongAdapter((MusicBoundActivity)getActivity(), null, artist);
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
			setEmptyText(getString(R.string.no_albums));
		}

		@Override
		public void onListItemClick(ListView l, View v, int position, long id) {
			super.onListItemClick(l, v, position, id);
			Song song = (Song)adapter.getItem(position);
			try {
				((MusicBoundActivity)getActivity()).getMusicService().playTrack(getActivity(), song);
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

	public static class BioListFragment extends MusicFragment {

		public BioListFragment() {  }

		private Artist artist;
		private User twitterUser;
		public final static int LOGIN_HANDER_RESULT = 600;
		
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			artist = ((ArtistViewer)getActivity()).artist;
		}
		
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			super.onCreateView(inflater, container, savedInstanceState);
			View view = inflater.inflate(R.layout.bio_fragment, null);
			view.findViewById(R.id.socialUpdates).setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(twitterUser == null)
						return;
					Uri uri = Uri.parse("https://twitter.com/" + twitterUser.getScreenName() +
							"/status/" + twitterUser.getStatus().getId());
					startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
				}
			});
			return view;
		}

		private void loadLastFm() {
			final Handler mHandler = new Handler();
			new Thread(new Runnable() {
				public void run() {
					try {
						final ArtistInfo info = LastFM.getArtistInfo(artist.getName());
						mHandler.post(new Runnable() {
							public void run() {
								if(getView() != null) {
									if(info.getBioContent() == null) {
										((TextView)getView().findViewById(R.id.bioAbout)).setText(R.string.no_bio_str);
									} else {
										((TextView)getView().findViewById(R.id.bioAbout)).setText(info.getBioContent());
									}
								}
							}
						});
					} catch (Exception e) {
						e.printStackTrace();
						mHandler.post(new Runnable() {
							public void run() {
								((TextView)getView().findViewById(R.id.bioAbout)).setText(R.string.failed_load_artist_bio);
							}
						});
					}
				}
			}).start();
		}

		private void loadTwitter() {
			final Handler mHandler = new Handler();
			new Thread(new Runnable() {
				public void run() {
					try {
						Twitter twitter = LoginHandler.getTwitterInstance(getActivity(), true);
						if(twitter == null) {
							mHandler.post(new Runnable() {
								public void run() {
									Button action = (Button)getView().findViewById(R.id.bioUpdatesAction);
									action.setEnabled(true);
									action.setVisibility(View.VISIBLE);
									action.setText(R.string.login_twitter);
									action.setOnClickListener(new View.OnClickListener() {
										@Override
										public void onClick(View arg0) {
											startActivityForResult(
													new Intent(getActivity(), LoginHandler.class), 
													LOGIN_HANDER_RESULT);
										}
									});
									getView().findViewById(R.id.bioUpdateSource).setVisibility(View.GONE);
									getView().findViewById(R.id.bioUpdates).findViewById(View.GONE);
								}
							});
							return;
						}
						ResponseList<User> possibleUsers = twitter.searchUsers(artist.getName(), 0);
						boolean found = false;
						if(possibleUsers.size() > 0) {
							for(int i = 0; i < possibleUsers.size(); i++) {
								if(possibleUsers.get(i).isVerified()) {
									twitterUser = possibleUsers.get(i);
									mHandler.post(new Runnable() {
										public void run() {
											if(getView() != null) {
												getView().findViewById(R.id.bioUpdatesAction).setVisibility(View.GONE);
												TextView updates = (TextView)getView().findViewById(R.id.bioUpdates);
												updates.setText(twitterUser.getStatus().getText());
												updates.setVisibility(View.VISIBLE);
												String source = getString(R.string.social_update_source)
														.replace("{time}", Utils.getFriendlyTime(twitterUser.getStatus().getCreatedAt()))
														.replace("{user}", "@" + twitterUser.getScreenName())
														.replace("{network}", "Twitter");
												TextView sourceTxt = (TextView)getView().findViewById(R.id.bioUpdateSource);
												sourceTxt.setText(source);
												sourceTxt.setVisibility(View.VISIBLE);
											}
										}
									});
									found = true;
									break;
								}
							}
						}
						if(!found) {
							mHandler.post(new Runnable() {
								public void run() { 
									TextView updates = (TextView)getView().findViewById(R.id.bioUpdates); 
									updates.setText(R.string.no_social_profile);
									updates.setVisibility(View.VISIBLE);
									getView().findViewById(R.id.bioUpdateSource).setVisibility(View.GONE);
									getView().findViewById(R.id.bioUpdatesAction).setVisibility(View.GONE);
								}
							});
						}
					} catch(Exception e) {
						e.printStackTrace();
						mHandler.post(new Runnable() {
							public void run() {
								((Button)getView().findViewById(R.id.bioUpdatesAction)).setText(R.string.error_str);
							}
						});
					}
				}
			}).start();
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			if(requestCode == LOGIN_HANDER_RESULT && resultCode == RESULT_OK) {
				update();
			}
		}
		
		@Override
		public void onViewCreated(View view, Bundle savedInstanceState) {
			super.onViewCreated(view, savedInstanceState);
			loadLastFm();
			loadTwitter();
		}


		@Override
		public void update() {
			if(getActivity() != null && getView() != null) {
				loadTwitter();
			} else {
				twitterUser = null;
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		return false;
	}

	@Override
	public void onBound() { }

	@Override
	public void onServiceUpdate() {
		for(int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			Fragment frag = mSectionsPagerAdapter.getItem(i);
			if(frag instanceof MusicFragment) {
				((MusicFragment)frag).update();
			} else if(frag instanceof MusicListFragment) {
				((MusicListFragment)frag).update();
			}
		}
	}
}