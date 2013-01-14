package com.afollestad.overhear.adapters;

import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Genre;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class GenreAdapter extends BaseAdapter {

	public GenreAdapter(Context context) {
		this.context = context;
	}

	private Context context;
	private Genre[] items;

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
	
	public void loadGenres() {
		items = Genre.getAllGenres(context).toArray(new Genre[0]);
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view;
		if (convertView == null) {
			view = (TextView)LayoutInflater.from(context).inflate(R.layout.genre_item, null);
		} else {
			view = (TextView)convertView;
		}
		Genre genre = items[position];
		view.setText(genre.getName());
		return view;
	}
}
