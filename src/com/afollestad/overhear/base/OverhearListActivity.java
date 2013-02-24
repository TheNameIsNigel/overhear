package com.afollestad.overhear.base;

import android.app.ListActivity;
import android.os.Bundle;

/**
 * @author Aidan Follestad
 */
public class OverhearListActivity extends ListActivity {

    private boolean mChangedConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null)
            mChangedConfig = savedInstanceState.getBoolean("changed_config", false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("changed_config", isChangingConfigurations());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mChangedConfig) {
            ((App)getApplication()).bind();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(!isChangingConfigurations()) {
            ((App)getApplication()).unbind();
        }
    }
}
