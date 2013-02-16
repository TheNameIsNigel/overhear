package com.afollestad.overhear.adapters;

import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Genre;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

public class GenreAdapter extends CursorAdapter {
	
	public GenreAdapter(Context context, Cursor c, int flags) {
		super(context, c, flags);
	}
	
	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		Genre genre = Genre.fromCursor(cursor);
		((TextView)view).setText(genre.getName());
	}
 
	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return LayoutInflater.from(context).inflate(R.layout.genre_item, null);
	}
}