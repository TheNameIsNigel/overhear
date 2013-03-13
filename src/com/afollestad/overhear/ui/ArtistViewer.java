package com.afollestad.overhear.ui;

import android.app.*;
import android.app.ActionBar.Tab;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.TaggedFragmentAdapter;
import com.afollestad.overhear.fragments.AlbumListFragment;
import com.afollestad.overhear.fragments.BioListFragment;
import com.afollestad.overhear.fragments.NowPlayingBarFragment;
import com.afollestad.overhear.fragments.SongListFragment;
import com.afollestad.overhear.utils.Twitter;
import com.afollestad.overhearapi.Artist;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

/**
 * Displays songs, albums, and bios of artists.
 * 
 * @author Aidan Follestad
 */
public class ArtistViewer extends OverhearActivity {

	public Artist artist;
	private SectionsPagerAdapter mSectionsPagerAdapter;
	private ViewPager mViewPager;
	

	public final static int TWEET_PLAYING_LOGIN = 400;
	

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == TWEET_PLAYING_LOGIN && resultCode == Activity.RESULT_OK) {
			startActivity(new Intent(this, TweetNowPlaying.class));
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_artist_viewer);
		
		try {
			artist = Artist.fromJSON(new JSONObject(getIntent().getStringExtra("artist")));
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if (findViewById(R.id.cover) != null) {
			ArtistAdapter.retrieveArtistArt(this, artist, (AImageView) findViewById(R.id.cover), false);
		}
		
		setTitle(artist.getName());
		setupTabs();
	}

	private void setupTabs() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
		
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            	getActionBar().setSelectedNavigationItem(position);
            }
        });

        ActionBar.TabListener mTabListener = new ActionBar.TabListener() {
			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft) {
				
			}
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft) {
				mViewPager.setCurrentItem(tab.getPosition());
			}
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft) {
				
			}
		};
        
		for(int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(mTabListener));
		}
		actionBar.setSelectedNavigationItem(1);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.artist_viewer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			startActivity(new Intent(this, OverviewScreen.class)
			.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
			finish();
			return true;
		case R.id.tweetPlaying:
			if (Twitter.getTwitterInstance(getApplicationContext(), true) == null)
				startActivityForResult(new Intent(this, LoginHandler.class), TWEET_PLAYING_LOGIN);
			else
				startActivity(new Intent(this, TweetNowPlaying.class));
			return true;
		case R.id.search:
			startActivity(new Intent(this, SearchScreen.class));
			return true;
		}
		return false;
	}

	@Override
	public void onBound() {
		((NowPlayingBarFragment)getFragmentManager().findFragmentById(R.id.nowPlaying)).update();
	}


	public class SectionsPagerAdapter extends TaggedFragmentAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: {
                    SongListFragment songs = new SongListFragment();
                    Bundle args = new Bundle();
                    args.putString("artist", artist.getJSON().toString());
                    songs.setArguments(args);
                    return songs;
                }
                case 1: {
                    AlbumListFragment albums = new AlbumListFragment();
                    Bundle args = new Bundle();
                    args.putString("artist", artist.getJSON().toString());
                    albums.setArguments(args);
                    return albums;
                }
                case 2: {
                    return new BioListFragment();
                }
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
}