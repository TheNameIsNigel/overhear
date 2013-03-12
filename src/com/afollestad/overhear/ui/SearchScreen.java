package com.afollestad.overhear.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.SearchAdapter;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.fragments.NowPlayingBarFragment;
import com.afollestad.overhear.fragments.SongListFragment;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;

import java.util.ArrayList;

/**
 * Allows you to search for songs, albums, artists, etc.
 * 
 * @author Aidan Follestad
 */
public class SearchScreen extends OverhearListActivity {

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    };


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
                column + " LIMIT 5");
    }

    public void search(String query) {
        adapter.clear();
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        Cursor cursor = openCursor(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, query, MediaStore.Audio.AlbumColumns.ALBUM);
        ArrayList<Album> albums = new ArrayList<Album>();
        while (cursor.moveToNext()) {
            albums.add(Album.fromCursor(cursor));
        }
        if (albums.size() > 0)
            adapter.add("Albums", albums.toArray());
        cursor.close();

        cursor = openCursor(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, query, MediaStore.Audio.ArtistColumns.ARTIST);
        ArrayList<Artist> artists = new ArrayList<Artist>();
        while (cursor.moveToNext()) {
            artists.add(Artist.fromCursor(cursor));
        }
        if (artists.size() > 0)
            adapter.add("Artists", artists.toArray());
        cursor.close();

        cursor = openCursor(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, query, MediaStore.Audio.Media.TITLE);
        ArrayList<QueueItem> songs = new ArrayList<QueueItem>();
        while (cursor.moveToNext()) {
            songs.add(QueueItem.fromCursor(cursor, -1, QueueItem.SCOPE_ALBUM));
        }
        if (songs.size() > 0)
            adapter.add("Songs", songs.toArray());
        cursor.close();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.activity_search);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        registerReceiver(mStatusReceiver, filter);
        adapter = new SearchAdapter(this);
        setListAdapter(adapter);
        getListView().setFastScrollEnabled(true);
        getListView().setSmoothScrollbarEnabled(true);
        getListView().setEmptyView(findViewById(android.R.id.empty));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        switch (adapter.getItemViewType(position)) {
            case 1:
                SongListFragment.performOnClick(this, (QueueItem) adapter.getItem(position), null, null,
                		null, QueueItem.SCOPE_SINGULAR, -1);
                break;
            case 2:
                startActivity(new Intent(this, AlbumViewer.class).putExtra("album",
                        ((Album) adapter.getItem(position)).getJSON().toString()));
                break;
            case 3:
                startActivity(new Intent(this, ArtistViewer.class).putExtra("artist",
                        ((Artist) adapter.getItem(position)).getJSON().toString()));
                break;
        }
    }

    private SearchView getSearchView(MenuItem item) {
        SearchView searchView = (SearchView)item.getActionView();
        searchView.setQueryHint(getString(R.string.search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String s) {
                lastQuery = s;
                mHandler.removeCallbacks(searchRunner);
                mHandler.postDelayed(searchRunner, 350);
                return false;
            }
        });
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        return searchView;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_screen, menu);
        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = getSearchView(searchItem);
        searchView.setFocusable(true);
        searchView.setIconified(false);
        searchView.requestFocusFromTouch();
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
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mStatusReceiver);
    }
    
    @Override
	public void onBound() {
        ((NowPlayingBarFragment)getFragmentManager().findFragmentById(R.id.nowPlaying)).update(true);
	}
}