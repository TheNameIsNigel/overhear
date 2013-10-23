package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;

import java.util.ArrayList;
import java.util.Collections;

public class SearchAdapter extends BaseAdapter {

    private final Activity context;
    private final ArrayList<Object> items;

    public SearchAdapter(Activity context) {
        this.context = context;
        this.items = new ArrayList<Object>();
    }

    public void add(String header, Object[] toadd) {
        items.add(header);
        Collections.addAll(items, toadd);
        notifyDataSetChanged();
    }

    public void clear() {
        items.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        Object item = items.get(position);
        if (item instanceof QueueItem) {
            return 1;
        } else if (item instanceof Album) {
            return 2;
        } else if (item instanceof Artist) {
            return 3;
        } else {
            return 0;
        }
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        int viewType = getItemViewType(i);

        switch (viewType) {
            default:
                if (view == null)
                    view = LayoutInflater.from(context).inflate(R.layout.list_header, null);
                ((TextView) view).setText(items.get(i).toString());
                break;
            case 1:
                view = SongAdapter.getViewForSong(context, (QueueItem) items.get(i), view, false);
                break;
            case 2:
                view = AlbumAdapter.getViewForAlbum(context, (Album) items.get(i), convertView);
                break;
            case 3:
                view = ArtistAdapter.getViewForArtist(context, (Artist) items.get(i), convertView, false);
                break;
        }

        int leftRightPad = 0;
        if (viewType == 0) {
            leftRightPad = context.getResources().getDimensionPixelSize(R.dimen.list_header_side_padding);
        } else {
            leftRightPad = context.getResources().getDimensionPixelSize(R.dimen.list_side_padding);
        }
        view.setPadding(leftRightPad, view.getPaddingTop(), leftRightPad, view.getPaddingBottom());

        return view;
    }
}
