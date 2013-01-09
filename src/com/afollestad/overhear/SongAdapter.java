package com.afollestad.overhear;

import com.afollestad.overhearapi.Song;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SongAdapter extends BaseAdapter {

	public SongAdapter(Context context, String album) {
		this.context = context;
		this.album = album;
	}

	private Context context;
	private Song[] items;
	private String album;

	@Override
	public int getCount() {
		if(items == null)
			return 0;
		else
			return items.length;
	}

	@Override
	public Object getItem(int index) {
		if(items == null)
			return null;
		return items[index];
	}

	@Override
	public long getItemId(int index) {
		return items[index].getId();
	}
	
	@Override
	public void notifyDataSetChanged() {
		items = Song.getAllSongs(context, album).toArray(new Song[0]);
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		RelativeLayout view;
		if (convertView == null) {
			view = (RelativeLayout)LayoutInflater.from(context).inflate(R.layout.song_item, null);
		} else {
			view = (RelativeLayout)convertView;
		}
		Song song = items[position];
		((TextView)view.findViewById(R.id.title)).setText(song.getTitle());
		return view;
	}
}
