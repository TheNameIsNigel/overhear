package com.afollestad.overhear.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import com.afollestad.overhear.service.MusicService;

import java.util.Calendar;

public class SleepTimer {

    public static PendingIntent getIntent(Context context, boolean nullIfNotInit) {
        Intent serviceIntent = new Intent(context, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_SLEEP_TIMER);
        return PendingIntent.getService(context, 0, serviceIntent, nullIfNotInit ?
                PendingIntent.FLAG_NO_CREATE : PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public static boolean isScheduled(Context context) {
        return getIntent(context, true) != null;
    }

    public static Calendar getScheduledTime(Context context) {
        long time = PreferenceManager.getDefaultSharedPreferences(context).getLong("sleep_timer_schedule", -1l);
        if(time == -1) {
            return null;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        return calendar;
    }

    public static void schedule(Context context, int minutes) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Calendar time = Calendar.getInstance();
        time.setTimeInMillis(System.currentTimeMillis());
        time.add(Calendar.MINUTE, minutes);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, time.getTimeInMillis(), getIntent(context, false));
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("sleep_timer_schedule", time.getTimeInMillis()).commit();
    }

    public static void cancel(Context context) {
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmMgr.cancel(getIntent(context, false));
        PendingIntent intent = getIntent(context, true);
        if (intent != null)
            intent.cancel();
        PreferenceManager.getDefaultSharedPreferences(context).edit().remove("sleep_timer_schedule").commit();
    }
}
