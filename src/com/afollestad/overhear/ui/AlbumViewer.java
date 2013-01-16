package com.afollestad.overhear.ui;

import org.json.JSONException;
import org.json.JSONObject;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.fragments.NowPlayingBarFragment;
import com.afollestad.overhear.fragments.SongListFragment;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;

import android.os.Bundle;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class AlbumViewer extends MusicBoundActivity {

	private Album album;
	private Artist artist;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_album_viewer);
		load();
		
		if (savedInstanceState == null) {
	        // First-time init; create fragment to embed in activity.
	        FragmentTransaction ft = getFragmentManager().beginTransaction();
	        Fragment newFragment = new SongListFragment();
	        Bundle args = new Bundle();
	        args.putInt("album_id", album.getAlbumId());
	        newFragment.setArguments(args);
	        ft.add(R.id.songList, newFragment);
	        ft.commit();
	    }
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.album_viewer, menu);
		return true;
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if(intent.hasExtra("album")) {
			setIntent(intent);
			load();
		}
	}
	
	private void load() {
		try {
			album = Album.fromJSON(this, new JSONObject(getIntent().getStringExtra("album")));
			artist = album.getArtist();
		} catch (JSONException e) {
			throw new java.lang.Error(e.getMessage());
		}
		((TextView)findViewById(R.id.artistName)).setText(artist.getName());
		setTitle(album.getName());
		
		findViewById(R.id.artistCover).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(new Intent(AlbumViewer.this, ArtistViewer.class)
						.putExtra("artist", artist.getJSON().toString()));
			}
		});
		
		int dimen = getResources().getDimensionPixelSize(R.dimen.split_view_image);
		ArtistAdapter.startArtistArtTask(this, artist, (ImageView)findViewById(R.id.artistCover), dimen);
		AlbumAdapter.startAlbumArtTask(this, album, (ImageView)findViewById(R.id.albumCover), dimen);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		case R.id.shopArtist:
			MusicUtils.browseArtist(getApplicationContext(), artist.getName());
			return true;
		}
		return false;
	}

	@Override
	public void onBound() {
		NowPlayingBarFragment frag = (NowPlayingBarFragment)getFragmentManager().findFragmentById(R.id.nowPlaying);
		frag.update();
	}
}
