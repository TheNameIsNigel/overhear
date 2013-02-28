package com.afollestad.overhear.utils;

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
 * Convenience methods for accessing the recents content provider. 
 * 
 * @author Aidan Follestad
 */
public class Recents {

	public final static Uri PROVIDER_URI = Uri.parse("content://com.afollestad.overhear.recentsprovider");
	
	/**
	 * Adds a song's album to the recent history database.
	 */
	public static void add(final Context context, final Song song) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Album album = Album.getAlbum(context, song.getAlbum(), song.getArtist());
				if(album == null) {
					String artist = song.getArtist();
					if(artist == null || artist.trim().isEmpty()) {
						artist = context.getString(R.string.unknown_str);
					}
					album = new Album(context.getString(R.string.unknown_str), artist);
				}
				
				album.setDateQueued(null); //Sets time to right now
				int updated = context.getContentResolver().update(PROVIDER_URI, album.getContentValues(true, false),
		                MediaStore.Audio.AlbumColumns.ALBUM + " = '" + album.getName().replace("'", "''") + "' AND " +
		                MediaStore.Audio.AlbumColumns.ARTIST + " = '" + album.getArtist().getName().replace("'", "''") + "'", 
		                null);
				if(updated == 0) {
			        long biggestId = getBiggestId(context);
			        if(biggestId == -1)
			            biggestId = 1;
			        else
			        	biggestId++;
			        album.setQueueId(biggestId);
					context.getContentResolver().insert(PROVIDER_URI, album.getContentValues(true, true));
				}
		        context.sendBroadcast(new Intent(MusicService.RECENTS_UPDATED));
			}
		}).start();
	}

    public static void clear(Context context) {
        context.getContentResolver().delete(PROVIDER_URI, null, null);
        context.sendBroadcast(new Intent(MusicService.RECENTS_UPDATED));
    }

    public static long getBiggestId(Context context) {
        Cursor cursor = context.getContentResolver().query(PROVIDER_URI, null, null, null, Song.QUEUE_ID + " DESC");
        if(!cursor.moveToFirst()) {
            return -1;
        }
        long toreturn = cursor.getLong(cursor.getColumnIndex(Song.QUEUE_ID));
        cursor.close();
        return toreturn;
    }

    public final static String SORT = Album.DATE_QUEUED + " DESC LIMIT 15";
}
