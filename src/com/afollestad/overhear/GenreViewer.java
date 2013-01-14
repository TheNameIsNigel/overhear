package com.afollestad.overhear;

import org.json.JSONObject;

import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Genre;
import com.afollestad.overhearapi.LoadedCallback;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public class GenreViewer extends MusicBoundActivity {

	AlbumAdapter adapter;
	NowPlayingBar nowPlaying;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.activity_genre_viewer);
		Genre genre = null;
		try {
			genre = Genre.fromJSON(new JSONObject(getIntent().getStringExtra("genre")));
		} catch(Exception e) {
			throw new Error(e.getMessage());
		}
		setTitle(genre.getName());
		adapter = new AlbumAdapter(this, null);
		getListView().setAdapter(adapter);
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int index, long id) {
				Album album = (Album)adapter.getItem(index);
				startActivity(new Intent(GenreViewer.this, AlbumViewer.class)
					.putExtra("album", album.getJSON().toString()));
			}
		});
		adapter.loadGenreAsync(genre, new LoadedCallback<Object>() {
			@Override
			public void onLoaded(Object result) {
				setListShown(true);
			}
		});
	}
	
	public void setListShown(boolean shown) {
		findViewById(android.R.id.list).setVisibility(shown ? View.VISIBLE : View.GONE);
		findViewById(android.R.id.progress).setVisibility(shown ? View.GONE : View.VISIBLE);
	}
	
	public ListView getListView() {
		return (ListView)findViewById(android.R.id.list);
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

	@Override
	public void onNowPlayingUpdate() {
		adapter.notifyDataSetChanged();
	}
}
