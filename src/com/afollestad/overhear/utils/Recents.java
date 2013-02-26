package com.afollestad.overhear.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.afollestad.overhear.R;
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
		if(album == null) {
			String artist = song.getArtist();
			if(artist == null || artist.trim().isEmpty()) {
				artist = context.getString(R.string.unknown_str);
			}
			album = new Album(context.getString(R.string.unknown_str), artist);
		}
		
		album.setDateQueued(null); //Sets time to right now
        ContentValues values = album.getContentValues(true);
		int updated = context.getContentResolver().update(PROVIDER_URI, values,
                MediaStore.Audio.AlbumColumns.ALBUM + " = '" + album.getName().replace("'", "''") + "' AND " +
                MediaStore.Audio.AlbumColumns.ARTIST + " = '" + album.getArtist().getName().replace("'", "''") + "'", null);
		if(updated == 0) {
			values = album.getContentValues(true);
			long queueId = 1;
	        Album mostRecent = getMostRecent(context);
	        if(mostRecent != null)
	            queueId = mostRecent.getQueueId() + 1;
	        album.setQueueId(queueId);
			context.getContentResolver().insert(PROVIDER_URI, values);
		}
        context.sendBroadcast(new Intent(MusicService.RECENTS_UPDATED));
	}

    public static void clear(Context context) {
        context.getContentResolver().delete(PROVIDER_URI, null, null);
        context.sendBroadcast(new Intent(MusicService.RECENTS_UPDATED));
    }

    public static Album getMostRecent(Context context) {
        Cursor cursor = context.getContentResolver().query(PROVIDER_URI, null, null, null, SORT);
        if(!cursor.moveToFirst()) {
            return null;
        }
        Album toreturn = Album.fromCursor(cursor);
        cursor.close();
        return toreturn;
    }

    public final static String SORT = Album.DATE_QUEUED + " DESC LIMIT 15";
}
