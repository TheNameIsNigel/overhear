package com.afollestad.overhear.ui;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SearchView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SearchAdapter;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;

import java.util.ArrayList;

public class SearchScreen extends ListActivity {

    protected SearchAdapter adapter;
    private Handler mHandler = new Handler();
    private String lastQuery;
    private Runnable searchRunner = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    search(lastQuery);
                }
            });
        }
    };

    private Cursor openCursor(Uri uri, String query, String column) {
        query = query.replace("'", "\\'").replace("%", "\\%").replace("_", "\\_");
        return getContentResolver().query(
                uri,
                null,
                column + " LIKE ('%" + query + "%') ESCAPE '\\'",
                null,
                column + " LIMIT 10");
    }

    public void search(String query) {
        adapter.clear();

        Cursor cursor = openCursor(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, query, MediaStore.Audio.AlbumColumns.ALBUM);
        ArrayList<Album> albums = new ArrayList<Album>();
        while (cursor.moveToNext()) {
            albums.add(Album.fromCursor(getApplicationContext(), cursor));
        }
        if(albums.size() > 0)
            adapter.add("Albums", albums.toArray());
        cursor.close();

        cursor = openCursor(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, query, MediaStore.Audio.ArtistColumns.ARTIST);
        ArrayList<Artist> artists = new ArrayList<Artist>();
        while (cursor.moveToNext()) {
            artists.add(Artist.fromCursor(cursor));
        }
        if(artists.size() > 0)
            adapter.add("Artists", artists.toArray());
        cursor.close();

        cursor = openCursor(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, query, MediaStore.Audio.Media.TITLE);
        ArrayList<Song> songs = new ArrayList<Song>();
        while (cursor.moveToNext()) {
            songs.add(Song.fromCursor(cursor));
        }
        if(songs.size() > 0)
            adapter.add("Songs", songs.toArray());
        cursor.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_search);
        adapter = new SearchAdapter(this);
        ((ListView) findViewById(android.R.id.list)).setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_screen, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                lastQuery = s;
                mHandler.removeCallbacks(searchRunner);
                mHandler.postDelayed(searchRunner, 250);
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }
}