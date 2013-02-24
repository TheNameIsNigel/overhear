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
import android.widget.*;
import com.afollestad.overhear.R;

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
        getLoaderManager().initLoader(0, null, this);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View toreturn = inflater.inflate(R.layout.grid_fragment, null);
        GridView grid = (GridView) toreturn.findViewById(R.id.grid_base);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
                getAdapter().getCursor().moveToPosition(position);
                OverhearGridFragment.this.onItemClick(position, getAdapter().getCursor());
            }
        });
        grid.setAdapter(getAdapter());
        return toreturn;
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
        if (data == null || data.getCount() == 0) {
            final TextView empty = (TextView) getView().findViewById(R.id.empty);
            empty.setText(getEmptyText());
            ((GridView) getView().findViewById(R.id.grid_base)).setEmptyView(empty);
        }
        if(getAdapter() != null)
            getAdapter().changeCursor(data);
    }

    @Override
    public final void onLoaderReset(Loader<Cursor> arg0) {
        if (getAdapter() != null)
            getAdapter().changeCursor(null);
    }
}
