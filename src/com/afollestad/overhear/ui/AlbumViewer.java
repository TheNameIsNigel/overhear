package com.afollestad.overhear.ui;

import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.NowPlayingBar;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.fragments.SongListFragment;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import android.os.Bundle;
import android.os.Handler;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class AlbumViewer extends MusicBoundActivity {

	private Album album;
	private Artist artist;
	NowPlayingBar nowPlaying;
	
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
		
		ArtistAdapter.loadArtistPicture(getApplicationContext(), artist,
				new WeakReference<ImageView>((ImageView)findViewById(R.id.artistCover)),
				160f, 160f);
		
		final Handler mHandler = new Handler();
		new Thread(new Runnable() {
			public void run() {
				try {
					final Bitmap albumCover = album.getAlbumArt(getApplicationContext(), 160f, 160f);
					mHandler.post(new Runnable() {
						public void run() { 
							((ImageView)findViewById(R.id.albumCover)).setImageBitmap(albumCover);
							invalidateOptionsMenu();
						}
					});
				} catch(Exception e) {
					e.printStackTrace();
					mHandler.post(new Runnable() {
						public void run() {
							Crouton.makeText(AlbumViewer.this, R.string.failed_load_artist_bio, Style.ALERT).show();
						}
					});
				}
			}
		}).start();
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
	public void onBound() { 
		nowPlaying = NowPlayingBar.get(this);
	}
}
