package com.afollestad.overhear.ui;

import android.app.Activity;
import android.content.Intent;
import android.media.audiofx.Equalizer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.*;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.fragments.NowPlayingBarFragment;
import com.afollestad.overhear.utils.Twitter;

/**
 * The equalizer interface.
 *
 * @author Aidan Follestad
 */
public class EqualizerViewer extends OverhearActivity {

    private Equalizer mEqualizer;
    public final static int TWEET_PLAYING_LOGIN = 400;

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
        short m = mEqualizer.getNumberOfPresets();
        ArrayAdapter<String> presetAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for(int k=0; k <m ; k++)
            presetAdapter.add(mEqualizer.getPresetName((short) k));
        ((Spinner)findViewById(R.id.presetSpinner)).setAdapter(presetAdapter);
    }

    private void loadBands() {
        mEqualizer = new Equalizer(0, getService().getMediaPlayer().getAudioSessionId());
        mEqualizer.setEnabled(true);
        short bands = mEqualizer.getNumberOfBands();

        final short minEQLevel = mEqualizer.getBandLevelRange()[0];
        final short maxEQLevel = mEqualizer.getBandLevelRange()[1];

        final LinearLayout mBandsView = (LinearLayout)findViewById(R.id.bands);
        mBandsView.removeAllViews();

        for (short i = 0; i < bands; i++) {
            final short band = i;

            TextView freqTextView = new TextView(this);
            freqTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            freqTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            freqTextView.setText((mEqualizer.getCenterFreq(band) / 1000) + " Hz");
            mBandsView.addView(freqTextView);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView minDbTextView = new TextView(this);
            minDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            minDbTextView.setText((minEQLevel / 100) + " dB");

            TextView maxDbTextView = new TextView(this);
            maxDbTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            maxDbTextView.setText((maxEQLevel / 100) + " dB");

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.FILL_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.weight = 1;
            SeekBar bar = new SeekBar(this);
            bar.setLayoutParams(layoutParams);
            bar.setMax(maxEQLevel - minEQLevel);
            bar.setProgress(mEqualizer.getBandLevel(band));

            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar seekBar, int progress,
                                              boolean fromUser) {
                    mEqualizer.setBandLevel(band, (short) (progress + minEQLevel));
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

            row.addView(minDbTextView);
            row.addView(bar);
            row.addView(maxDbTextView);

            mBandsView.addView(row);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.album_viewer, menu);
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
        ((NowPlayingBarFragment) getFragmentManager().findFragmentById(R.id.nowPlaying)).update(true);
        loadBands();
        loadPresets();
    }
}