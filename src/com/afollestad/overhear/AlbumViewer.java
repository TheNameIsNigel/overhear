package com.afollestad.overhear;

import java.lang.ref.WeakReference;

import org.json.JSONException;
import org.json.JSONObject;

import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

import android.os.Bundle;
import android.os.Handler;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AlbumViewer extends MusicBoundActivity {

	private SongAdapter adapter;
	private Album album;
	private Artist artist;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_album_viewer);
		load();
		ListView list = (ListView)findViewById(R.id.songList);
		list.setAdapter(adapter);
		list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View view, int index, long id) {
				Song song = (Song)adapter.getItem(index);
				try {
					getMusicService().playTrack(getApplicationContext(), song);
				} catch(Exception e) {
					e.printStackTrace();
					Crouton.makeText(AlbumViewer.this, "Failed to play " + song.getTitle(), Style.ALERT);
				}
				adapter.notifyDataSetChanged();
			}
		});
		adapter.loadSongs();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		adapter.notifyDataSetChanged();
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
		adapter = new SongAdapter(this, album.getName(), null);
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
							Crouton.makeText(AlbumViewer.this, R.string.failed_load_artist_bio, Style.ALERT);
						}
					});
				}
			}
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_album_viewer, menu);
		return true;
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
		adapter.notifyDataSetChanged();
	}
}
