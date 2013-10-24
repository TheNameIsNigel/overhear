package com.afollestad.overhear.base;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.GridView;
import com.afollestad.overhear.R;
import com.afollestad.silk.caching.SilkComparable;
import com.afollestad.silk.fragments.SilkCursorListFragment;

/**
 * The base of all list fragments, used for convenience (handles common functions that every
 * fragment uses, reducing the amount of code and complexity among activity Java files).
 *
 * @author Aidan Follestad
 */
public abstract class OverhearListFragment<ItemType extends SilkComparable> extends SilkCursorListFragment<ItemType> {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    protected abstract BroadcastReceiver getReceiver();

    protected abstract IntentFilter getFilter();

    /**
     * Life cycle methods
     */

    @Override
    public final void onResume() {
        super.onResume();
        if (getAdapter() != null)
            getAdapter().notifyDataSetChanged();
    }

    @Override
    public final void onStart() {
        super.onStart();
        if (getReceiver() != null)
            getActivity().registerReceiver(getReceiver(), getFilter());
    }

    @Override
    public final void onStop() {
        super.onStop();
        if (getReceiver() != null)
            getActivity().unregisterReceiver(getReceiver());
    }

    @Override
    public final void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!(getListView() instanceof GridView)) {
            int pad = getResources().getDimensionPixelSize(R.dimen.list_side_padding);
            getListView().setPadding(pad, 0, pad, 0);
        }
        getListView().setSmoothScrollbarEnabled(true);
        getListView().setFastScrollEnabled(true);
    }
}