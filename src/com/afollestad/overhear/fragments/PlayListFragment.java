package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.PlaylistAdapter;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.ui.PlaylistViewer;
import com.afollestad.overhearapi.Playlist;

public class PlayListFragment extends ListFragment implements LoaderCallbacks<Cursor> {

    private PlaylistAdapter adapter;

    private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (adapter != null)
                adapter.notifyDataSetChanged();
        }
    };


    public PlayListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        getActivity().registerReceiver(mStatusReceiver, filter);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mStatusReceiver);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int pad = getResources().getDimensionPixelSize(R.dimen.list_side_padding);
        getListView().setPadding(pad, 0, pad, 0);
        getListView().setSmoothScrollbarEnabled(true);
        getListView().setFastScrollEnabled(true);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                view.findViewById(R.id.options).performClick();
                return true;
            }
        });
        setEmptyText(getString(R.string.no_playlists));
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        adapter.getCursor().moveToPosition(position);
        Playlist list = Playlist.fromCursor(adapter.getCursor());
        performOnClick(getActivity(), list);
    }

    public static void performOnClick(Activity context, Playlist list) {
        context.startService(new Intent(context, PlaylistViewer.class)
                .putExtra("playlist", list.getJSON().toString()));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        if (data == null)
            return;
        adapter = new PlaylistAdapter(getActivity(), data, 0);
        setListAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        if (adapter != null)
            adapter.changeCursor(null);
    }
}