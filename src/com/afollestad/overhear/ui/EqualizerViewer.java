package com.afollestad.overhear.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.fragments.NowPlayingBarFragment;
import com.afollestad.overhear.utils.Twitter;
import com.afollestad.overhear.views.VerticalSeekBar;

/**
 * The equalizer interface.
 *
 * @author Aidan Follestad
 */
public class EqualizerViewer extends OverhearActivity {

    private final static int TWEET_PLAYING_LOGIN = 400;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TWEET_PLAYING_LOGIN && resultCode == Activity.RESULT_OK) {
            startActivity(new Intent(this, TweetNowPlaying.class));
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.equalizer);
    }

    private void loadPresets() {
        short m = getService().getEqualizer().getNumberOfPresets();
        ArrayAdapter<String> presetAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetAdapter.add(getString(R.string.user_defined));
        for (int k = 0; k < m; k++)
            presetAdapter.add(getService().getEqualizer().getPresetName((short) k));
        Spinner spinner = (Spinner) findViewById(R.id.presetSpinner);
        spinner.setAdapter(presetAdapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int index, long id) {
                if (index == 0)
                    return;
                getService().getEqualizer().usePreset((short) (index - 1));
                loadBands();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        if (getService().getEqualizer().getCurrentPreset() > -1)
            spinner.setSelection(getService().getEqualizer().getCurrentPreset() + 1);
    }

    private void loadBands() {
        short bands = getService().getEqualizer().getNumberOfBands();
        final Spinner presetSpinner = (Spinner) findViewById(R.id.presetSpinner);
        final short minEQLevel = getService().getEqualizer().getBandLevelRange()[0];
        final short maxEQLevel = getService().getEqualizer().getBandLevelRange()[1];
        final LinearLayout mBandsView = (LinearLayout) findViewById(R.id.bands);
        mBandsView.removeAllViews();

        for (short i = 0; i < bands; i++) {
            final short band = i;
            LinearLayout bandView = (LinearLayout) getLayoutInflater().inflate(R.layout.equalizer_band, null);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            layoutParams.weight = 1;
            bandView.setLayoutParams(layoutParams);

            VerticalSeekBar bar = (VerticalSeekBar) bandView.findViewById(R.id.bar);
            bar.setMax(maxEQLevel - minEQLevel);
            bar.setProgress(getService().getEqualizer().getBandLevel(band));
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    presetSpinner.setSelection(0);
                    getService().getEqualizer().setBandLevel(band, (short) (progress + minEQLevel));
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            TextView frequency = (TextView) bandView.findViewById(R.id.frequency);
            frequency.setText((getService().getEqualizer().getCenterFreq(band) / 1000) + "");
            mBandsView.addView(bandView);
        }

        mBandsView.requestLayout();
        mBandsView.invalidate();
    }

    private void loadBassBoost() {
        SeekBar strengthBar = (SeekBar) findViewById(R.id.bassBoostStrength);
        if (!getService().getBassBoost().getStrengthSupported()) {
            strengthBar.setEnabled(false);
            return;
        }
        strengthBar.setProgress(getService().getBassBoost().getRoundedStrength());
        strengthBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean isUser) {
                if (isUser) {
                    getService().getBassBoost().setStrength((short) progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.equilizer_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(this, OverviewScreen.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                finish();
                return true;
            case R.id.tweetPlaying:
                if (Twitter.getTwitterInstance(getApplicationContext(), true) == null)
                    startActivityForResult(new Intent(this, LoginHandler.class), TWEET_PLAYING_LOGIN);
                else
                    startActivity(new Intent(this, TweetNowPlaying.class));
                return true;
        }
        return false;
    }

    @Override
    public void onBound() {
        ((NowPlayingBarFragment) getFragmentManager().findFragmentById(R.id.nowPlaying)).update();
        loadBands();
        loadPresets();
        loadBassBoost();
    }
}