package com.afollestad.overhear;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

/**
 * Various utilities for modifying the recent history.
 * @author Aidan Follestad
 */
public class Recents {

	public final static Uri PROVIDER_URI = Uri.parse("content://com.afollestad.overhear.recentsprovider");
	
	/**
	 * Adds a song's album to the recent history database.
	 */
	public static void add(Context context, Song song) {
		Album album = Album.getAlbum(context, song.getAlbum(), song.getArtist());
        album.setQueueId(getSize(context) + 1);
		ContentValues values = album.getContentValues(true);
		int updated = context.getContentResolver().update(PROVIDER_URI, values, "_id = " + album.getAlbumId(), null);
		if(updated == 0) {
			context.getContentResolver().insert(PROVIDER_URI, values);
		}
        context.sendBroadcast(new Intent(MusicService.RECENTS_UPDATED));
	}

    public static int getSize(Context context) {
        Cursor cursor = context.getContentResolver().query(PROVIDER_URI, null, null, null, null);
        int rows = cursor.getCount();
        cursor.close();
        return rows;
    }
}
