package com.afollestad.overhear.ui;

import java.util.Locale;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.R;
import com.afollestad.overhear.TaggedFragmentAdapter;
import com.afollestad.overhear.fragments.AlbumListFragment;
import com.afollestad.overhear.fragments.ArtistListFragment;
import com.afollestad.overhear.fragments.GenreListFragment;
import com.afollestad.overhear.fragments.NowPlayingBarFragment;
import com.afollestad.overhear.fragments.SongListFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class OverviewScreen extends MusicBoundActivity {

	SectionsPagerAdapter mSectionsPagerAdapter;
	ViewPager mViewPager;

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
				return new ArtistListFragment();
			case 1:
				return new AlbumListFragment();
			case 2:
				return new SongListFragment();
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
				return getString(R.string.artists_str).toUpperCase(Locale.getDefault());
			case 1:
				return getString(R.string.albums_str).toUpperCase(Locale.getDefault());
			case 2:
				return getString(R.string.songs_str).toUpperCase(Locale.getDefault());
			case 3:
				return getString(R.string.genres_str).toUpperCase(Locale.getDefault());
			}
			return null;
		}
	}

	@Override
	public void onBound() {
		for(int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			Fragment frag = getFragmentManager().findFragmentByTag("page:" + i);
			if(frag instanceof NowPlayingBarFragment) {
				((NowPlayingBarFragment)frag).update();
			}
		}
	}
}