package com.afollestad.overhear.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.silk.adapters.SilkAdapter;
import com.afollestad.silk.caching.SilkComparable;

public class SearchAdapter extends SilkAdapter<SilkComparable> {

    public SearchAdapter(Activity context) {
        super(context);
    }

    @Override
    protected int getLayout(int index, int type) {
        switch (type) {
            default:
                return R.layout.list_header;
            case 1:
                return R.layout.song_item;
            case 2:
                return R.layout.album_item;
            case 3:
                return R.layout.artist_item_nongrid;
        }
    }

    @Override
    public View onViewCreated(int index, View recycled, SilkComparable item) {
        int viewType = getItemViewType(index);
        switch (viewType) {
            default:
                ((TextView) recycled).setText(item.toString());
                break;
            case 1:
                recycled = SongAdapter.getViewForSong((Activity) getContext(), (QueueItem) item, recycled, false);
                break;
            case 2:
                recycled = AlbumAdapter.getViewForAlbum((Activity) getContext(), (Album) item, recycled, getScrollState());
                break;
            case 3:
                recycled = ArtistAdapter.getViewForArtist((Activity) getContext(), (Artist) item, recycled, false, getScrollState());
                break;
        }
        int leftRightPad;
        if (viewType == 0) {
            leftRightPad = getContext().getResources().getDimensionPixelSize(R.dimen.list_header_side_padding);
        } else leftRightPad = getContext().getResources().getDimensionPixelSize(R.dimen.list_side_padding);
        recycled.setPadding(leftRightPad, recycled.getPaddingTop(), leftRightPad, recycled.getPaddingBottom());
        return recycled;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public int getItemViewType(int position) {
        SilkComparable item = getItems().get(position);
        if (item instanceof QueueItem) {
            return 1;
        } else if (item instanceof Album) {
            return 2;
        } else if (item instanceof Artist) {
            return 3;
        } else return 0;
    }

    @Override
    public long getItemId(SilkComparable item) {
        if (item.getSilkId() instanceof Long)
            return (Long) item.getSilkId();
        else return -1;
    }

    public static class Header implements SilkComparable<Header> {
        private final String mName;

        public Header(Context context, int name) {
            mName = context.getString(name);
        }

        @Override
        public String toString() {
            return mName;
        }

        @Override
        public Object getSilkId() {
            return mName;
        }

        @Override
        public boolean equalTo(Header other) {
            return mName.equals(other.mName);
        }
    }
}