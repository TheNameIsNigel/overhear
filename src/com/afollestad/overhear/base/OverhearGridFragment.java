package com.afollestad.overhear.base;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.CursorLoader;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.GridView;
import com.afollestad.overhear.R;

/**
 * The base of all grid fragments, used for convenience (handles common functions that every
 * fragment uses, reducing the amount of code and complexity among activity Java files).
 *
 * @author Aidan Follestad
 */
public abstract class OverhearGridFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

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

    public abstract void onItemLongClick(int position, Cursor cursor, View view);

    public abstract void onInitialize();

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
    public final void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        GridView view = (GridView) inflater.inflate(R.layout.grid_fragment, null);
        view.setSmoothScrollbarEnabled(true);
        view.setFastScrollEnabled(true);
        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                getAdapter().getCursor().moveToPosition(position);
                OverhearGridFragment.this.onItemClick(position, getAdapter().getCursor());
            }
        });
        view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long l) {
                getAdapter().getCursor().moveToPosition(position);
                OverhearGridFragment.this.onItemLongClick(position, getAdapter().getCursor(), view);
                return true;
            }
        });
        view.setAdapter(getAdapter());
        return view;
    }

    public GridView getGridView() {
        return (GridView) getView();
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
        if (data == null)
            return;
        if (getAdapter() != null)
            getAdapter().changeCursor(data);
    }

    @Override
    public final void onLoaderReset(Loader<Cursor> arg0) {
        if (getAdapter() != null)
            getAdapter().changeCursor(null);
    }
}
