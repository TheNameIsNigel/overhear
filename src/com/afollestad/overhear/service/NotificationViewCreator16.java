package com.afollestad.overhear.service;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import com.afollestad.overhearapi.Song;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NotificationViewCreator16 {

	public static Notification createNotification(Context context, Notification.Builder builder, Song nowPlaying, Bitmap art, boolean playing) {
        builder.setPriority(Notification.PRIORITY_HIGH);
		Notification status = builder.build();
        status.bigContentView = NotificationViewCreator.createView(context, true, nowPlaying, art, playing);
        return status;
	}
}
