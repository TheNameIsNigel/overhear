package com.afollestad.overhear.fragments;

import com.afollestad.overhear.MusicBoundActivity;
import com.afollestad.overhear.MusicService;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.RecentsAdapter;
import com.afollestad.overhear.ui.AlbumViewer;
import com.afollestad.overhearapi.Album;

import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

public class RecentsListFragment extends ListFragment {

	private RecentsAdapter adapter;
	private final BroadcastReceiver mStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        	if(adapter != null)
    			adapter.notifyDataSetChanged();
        }
    };
	
	
	public RecentsListFragment() { }

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		adapter = new RecentsAdapter((MusicBoundActivity)getActivity());
	}

	@Override
	public void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.PLAYING_STATE_CHANGED);
        getActivity().registerReceiver(mStatusReceiver, filter);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		getActivity().unregisterReceiver(mStatusReceiver);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		int pad = getResources().getDimensionPixelSize(R.dimen.list_side_padding);
		getListView().setPadding(pad, 0, pad, 0);
		getListView().setSmoothScrollbarEnabled(true);
		getListView().setFastScrollEnabled(true);
		setListAdapter(adapter);
		setEmptyText(getString(R.string.no_recents));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
		Album album = (Album)adapter.getItem(position);
		startActivity(new Intent(getActivity(), AlbumViewer.class)
		.putExtra("album", album.getJSON().toString()));
	}
}