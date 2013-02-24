package com.afollestad.overhear.base;

import android.app.Activity;
import android.os.Bundle;

/**
 * @author Aidan Follestad
 */
public class OverhearActivity extends Activity {

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
            Overhear.get(this).bind();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if(!isChangingConfigurations()) {
            Overhear.get(this).unbind();
        }
    }
}
