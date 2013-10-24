package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.database.Cursor;
import android.view.View;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Genre;
import com.afollestad.silk.adapters.SilkCursorAdapter;

public class GenreAdapter extends SilkCursorAdapter<Genre> {

    public GenreAdapter(Activity context, Cursor c) {
        super(context, R.layout.genre_item, c, new CursorConverter<Genre>() {
            @Override
            public Genre convert(Cursor cursor) {
                return Genre.fromCursor(cursor);
            }
        });
    }

    @Override
    public View onViewCreated(int index, View recycled, Genre item) {
        ((TextView) recycled).setText(item.getName());
        return recycled;
    }
}