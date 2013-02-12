package com.afollestad.overhear.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.KeyEvent;
import android.widget.RemoteViews;
import com.afollestad.overhear.R;
import com.afollestad.overhear.ui.NowPlayingViewer;
import com.afollestad.overhear.ui.OverviewScreen;
import com.afollestad.overhearapi.Song;

public class NotificationViewCreator {

	@SuppressWarnings("deprecation")
    public static Notification createNotification(Context context, Song nowPlaying, Bitmap art, boolean playing) {
		Notification.Builder builder = new Notification.Builder(context);
		builder.setContent(createView(context, false, nowPlaying, art, playing));
		builder.setOngoing(true);
        builder.setPriority(Notification.PRIORITY_MAX);
		builder.setSmallIcon(R.drawable.stat_notify_music);

        Intent nowPlayingIntent = new Intent(context, NowPlayingViewer.class).
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(OverviewScreen.class);
        stackBuilder.addNextIntent(new Intent(context, OverviewScreen.class));
        stackBuilder.addNextIntent(nowPlayingIntent);
		builder.setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			return NotificationViewCreator16.createNotification(context, builder, nowPlaying, art, playing);
		}
        return builder.getNotification();
	}
	
	protected static RemoteViews createView(Context context, boolean big, Song nowPlaying, Bitmap art, boolean playing) {
		RemoteViews views;
		if(big) {
			views = new RemoteViews(context.getPackageName(), R.layout.status_bar_big);
		} else {
			views = new RemoteViews(context.getPackageName(), R.layout.status_bar);
		}
        if (art != null) {
            views.setImageViewBitmap(R.id.status_bar_album_art, art);
        }
        
        ComponentName rec = new ComponentName(context.getPackageName(),
                MediaButtonIntentReceiver.class.getName());
        Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
        mediaButtonIntent.setComponent(rec);
        
        KeyEvent mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
        PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(context,
                4, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_previous, mediaPendingIntent);
        
        mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
        mediaPendingIntent = PendingIntent.getBroadcast(context,
                1, mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_play, mediaPendingIntent);
        
        mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
        mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
        mediaPendingIntent = PendingIntent.getBroadcast(context, 2,
                mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_next, mediaPendingIntent);
        
        mediaKey = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_STOP);
        mediaButtonIntent.putExtra(Intent.EXTRA_KEY_EVENT, mediaKey);
        mediaPendingIntent = PendingIntent.getBroadcast(context, 3,
                mediaButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_collapse, mediaPendingIntent);
        
        if(playing)
        	views.setImageViewResource(R.id.status_bar_play, R.drawable.pause);
        else
        	views.setImageViewResource(R.id.status_bar_play, R.drawable.play);
        
        views.setTextViewText(R.id.status_bar_track_name, nowPlaying.getTitle());
        views.setTextViewText(R.id.status_bar_artist_name, nowPlaying.getArtist());
        if(big) {
        	views.setTextViewText(R.id.status_bar_album_name, nowPlaying.getAlbum());	
        }
        
        return views;
	}
}
