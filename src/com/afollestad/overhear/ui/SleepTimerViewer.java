package com.afollestad.overhear.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Html;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.*;
import com.afollestad.overhear.MusicUtils;
import com.afollestad.overhear.Queue;
import com.afollestad.overhear.R;
import com.afollestad.overhear.SleepTimer;
import com.afollestad.overhear.adapters.AlbumAdapter;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.service.MusicService.MusicBinder;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;
import com.afollestad.overhearapi.Utils;
import com.androidquery.AQuery;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class SleepTimerViewer extends Activity {

    Calendar sleepTime;
    Timer timer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		setContentView(R.layout.sleep_timer_viewer);
        sleepTime = SleepTimer.getScheduledTime(this);
	}

	public void onResume() {
		super.onResume();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        }, 250, 250);
	}

	public void onPause() {
		super.onPause();
        timer.cancel();
        timer.purge();
        timer = null;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_sleep_timer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.cancel:
                SleepTimer.cancel(this);
                Toast.makeText(getApplicationContext(), R.string.sleep_timer_cancelled, Toast.LENGTH_SHORT).show();
                finish();
                return true;
        }
        return false;
    }

    public void update() {
        Calendar now = Calendar.getInstance();
        long diff = sleepTime.getTimeInMillis() - now.getTimeInMillis();
        TextView sleptAt = (TextView)findViewById(R.id.sleepAt);
        if(diff < 0) {
            sleptAt.setVisibility(View.VISIBLE);
            sleptAt.setText(getString(R.string.slept_at_str).replace("{time}", Utils.getFriendlyTime(now)));
            ((TextView)findViewById(R.id.text)).setText("0:00");
            timer.cancel();
            timer.purge();
            timer = null;
            return;
        }
        sleptAt.setVisibility(View.GONE);
        ((TextView)findViewById(R.id.text)).setText(Song.getDurationString(diff));
    }
}