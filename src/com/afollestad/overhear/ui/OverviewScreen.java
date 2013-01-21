package com.afollestad.overhear.ui;

import java.util.Locale;

import com.afollestad.overhear.R;
import com.afollestad.overhear.TaggedFragmentAdapter;
import com.afollestad.overhear.fragments.AlbumListFragment;
import com.afollestad.overhear.fragments.ArtistListFragment;
import com.afollestad.overhear.fragments.GenreListFragment;
import com.afollestad.overhear.fragments.RecentsListFragment;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

public class OverviewScreen extends Activity {

	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

	public final static int TWEET_PLAYING_LOGIN = 400;
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == TWEET_PLAYING_LOGIN && resultCode == Activity.RESULT_OK) {
			startActivity(new Intent(this, TweetNowPlaying.class));
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(3);
		mViewPager.setCurrentItem(2);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.overview_screen, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.tweetPlaying:
			if(LoginHandler.getTwitterInstance(getApplicationContext(), true) == null)
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
				return getString(R.string.recent_str).toUpperCase(Locale.getDefault()); 
			case 1:
				return getString(R.string.artists_str).toUpperCase(Locale.getDefault());
			case 2:
				return getString(R.string.albums_str).toUpperCase(Locale.getDefault());
			case 3:
				return getString(R.string.genres_str).toUpperCase(Locale.getDefault());
			}
			return null;
		}
	}
}