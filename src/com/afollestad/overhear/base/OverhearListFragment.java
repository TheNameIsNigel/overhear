package com.afollestad.overhear.base;

import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.CursorLoader;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ListView;
import com.afollestad.overhear.R;

/**
 * The base of all list fragments, used for convenience (handles common functions that every
 * fragment uses, reducing the amount of code and complexity among activity Java files).
 * 
 * @author Aidan Follestad
 */
public abstract class OverhearListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        this.onInitialize();
    }

    public abstract Uri getLoaderUri();

    public abstract String getLoaderSelection();

    public abstract String getLoaderSort();

    public abstract CursorAdapter getAdapter();

    public abstract BroadcastReceiver getReceiver();

    public abstract IntentFilter getFilter();

    public abstract String getEmptyText();

    public abstract void onItemClick(int position, Cursor cursor);

    public abstract void onInitialize();

    /**
     * Life cycle methods
     */

    @Override
    public final void onResume() {
        super.onResume();
        if(getAdapter() != null)
            getAdapter().notifyDataSetChanged();
    }

    @Override
    public final void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setListAdapter(getAdapter());
        getLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public final void onStart() {
        super.onStart();
        if(getReceiver() != null)
            getActivity().registerReceiver(getReceiver(), getFilter());
    }

    @Override
    public final void onStop() {
        super.onStop();
        if(getReceiver() != null)
            getActivity().unregisterReceiver(getReceiver());
    }

    @Override
    public final void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int pad = getResources().getDimensionPixelSize(R.dimen.list_side_padding);
        getListView().setPadding(pad, 0, pad, 0);
        getListView().setSmoothScrollbarEnabled(true);
        getListView().setFastScrollEnabled(true);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(view.findViewById(R.id.options) != null)
                    view.findViewById(R.id.options).performClick();
                return true;
            }
        });
        setEmptyText(getEmptyText());
    }

    @Override
    public final void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        getAdapter().getCursor().moveToPosition(position);
        this.onItemClick(position, getAdapter().getCursor());
    }


    /**
     * Cursor loader methods
     */

    @Override
    public final Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),
                getLoaderUri(),
                null,
                getLoaderSelection(),
                null,
                getLoaderSort());
    }

    @Override
    public final void onLoadFinished(Loader<Cursor> arg0, Cursor data) {
        if(data == null)
            return;
        if(getAdapter() != null)
            getAdapter().changeCursor(data);
    }

    @Override
    public final void onLoaderReset(Loader<Cursor> arg0) {
        if (getAdapter() != null)
            getAdapter().changeCursor(null);
    }
}
