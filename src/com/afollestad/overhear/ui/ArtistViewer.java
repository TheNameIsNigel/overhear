package com.afollestad.overhear.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import com.afollestad.aimage.views.AImageView;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhear.TaggedFragmentAdapter;
import com.afollestad.overhear.Twitter;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.fragments.AlbumListFragment;
import com.afollestad.overhear.fragments.BioListFragment;
import com.afollestad.overhear.fragments.SongListFragment;
import com.afollestad.overhearapi.Artist;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class ArtistViewer extends Activity {

    SectionsPagerAdapter mSectionsPagerAdapter;
    ViewPager mViewPager;
    public Artist artist;

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
        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(2);
        mViewPager.setCurrentItem(1);
        try {
            artist = Artist.fromJSON(new JSONObject(getIntent().getStringExtra("artist")));
        } catch (JSONException e) {
            e.printStackTrace();
            //throw new Error(e.getMessage());
        }
        setTitle(artist.getName());
        if (findViewById(R.id.cover) != null) {
            ArtistAdapter.retrieveArtistArt(this, artist, (AImageView) findViewById(R.id.cover));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.artist_viewer, menu);
        return true;
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
                    args.putString("artist_name", artist.getName());
                    args.putBoolean("show_artist", false);
                    songs.setArguments(args);
                    return songs;
                }
                case 1: {
                    AlbumListFragment albums = new AlbumListFragment();
                    Bundle args = new Bundle();
                    args.putString("artist_name", artist.getName());
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.shopArtist:
                MusicUtils.browseArtist(getApplicationContext(), artist.getName());
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
}