package com.afollestad.overhear.fragments;

import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.Recents;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhearapi.Album;

public class RecentsListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    private AlbumAdapter adapter;
    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MusicService.RECENTS_UPDATED)) {
                getLoaderManager().restartLoader(0, null, RecentsListFragment.this);
            } else if (intent.getAction().equals(MusicService.PLAYING_STATE_CHANGED) && adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    };


    public RecentsListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }


    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(0, null, RecentsListFragment.this);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.RECENTS_UPDATED);
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        getActivity().registerReceiver(mStatusReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mStatusReceiver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new AlbumAdapter(getActivity(), 0, null, new String[]{}, new int[]{}, 0);
        setListAdapter(adapter);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int pad = getResources().getDimensionPixelSize(R.dimen.list_side_padding);
        getListView().setPadding(pad, 0, pad, 0);
        getListView().setSmoothScrollbarEnabled(true);
        getListView().setFastScrollEnabled(true);
        setEmptyText(getString(R.string.no_recents));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        adapter.getCursor().moveToPosition(position);
        Album album = Album.fromCursor(adapter.getCursor());
        startActivity(new Intent(getActivity(), AlbumViewer.class)
                .putExtra("album", album.getJSON().toString()));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Recents.PROVIDER_URI,
                null, null, null, Recents.SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        if (data == null)
            return;
        adapter.changeCursor(data);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        if (adapter != null)
            adapter.changeCursor(null);
    }
}