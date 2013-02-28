package com.afollestad.overhear.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.TaskStackBuilder;
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

        Notification noti = builder.getNotification();
        noti.flags = Notification.FLAG_HIGH_PRIORITY;
        return noti;
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
        
        PendingIntent pi = PendingIntent.getService(context, 1, new Intent(MusicService.ACTION_REWIND), PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_previous, pi);
        
        pi = PendingIntent.getService(context, 2, new Intent(MusicService.ACTION_TOGGLE_PLAYBACK), PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_play, pi);
        
        pi = PendingIntent.getService(context, 2, new Intent(MusicService.ACTION_SKIP), PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_next, pi);
        
        pi = PendingIntent.getService(context, 2, new Intent(MusicService.ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.status_bar_collapse, pi);
        
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
