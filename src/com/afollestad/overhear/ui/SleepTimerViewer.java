package com.afollestad.overhear.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.overhear.R;
import com.afollestad.overhear.SleepTimer;
import com.afollestad.overhearapi.Song;
import com.afollestad.overhearapi.Utils;

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

    @Override
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

    @Override
	public void onPause() {
		super.onPause();
        if(timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
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
                startActivity(new Intent(this, OverviewScreen.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
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
        long diff = -1;
        if(sleepTime != null) {
            diff = sleepTime.getTimeInMillis() - now.getTimeInMillis();
        }
        TextView sleptAt = (TextView)findViewById(R.id.sleepAt);
        if(diff < 0) {
            sleptAt.setVisibility(View.VISIBLE);
            sleptAt.setText(getString(R.string.slept_at_str).replace("{time}", Utils.getFriendlyTime(now)));
            ((TextView)findViewById(R.id.text)).setText("0:00");
            if(timer != null) {
                timer.cancel();
                timer.purge();
                timer = null;
            }
            return;
        }
        sleptAt.setVisibility(View.GONE);
        ((TextView)findViewById(R.id.text)).setText(Song.getDurationString(diff));
    }
}