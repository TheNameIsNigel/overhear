package com.afollestad.overhear.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.fragments.SongListFragment;
import com.afollestad.overhear.utils.Twitter;
import com.afollestad.overhearapi.Genre;

/**
 * Displays songs of a genre.
 * 
 * @author Aidan Follestad
 */
public class GenreViewer extends OverhearActivity {

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
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_genre_viewer);

		if(savedInstanceState == null) {
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			Fragment newFragment = new SongListFragment();
			Bundle args = new Bundle();
			args.putString("genre", getIntent().getStringExtra("genre"));
			newFragment.setArguments(args);
			ft.add(R.id.songList, newFragment);
			ft.commit();
		}

		setTitle(Genre.fromJSON(getIntent().getStringExtra("genre")).getName());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.genre_viewer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			startActivity(new Intent(this, OverviewScreen.class)
			.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
			finish();
			return true;
		case R.id.tweetPlaying:
			if(Twitter.getTwitterInstance(getApplicationContext(), true) == null)
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
	}
}
