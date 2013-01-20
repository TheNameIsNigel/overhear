package com.afollestad.overhear;

import java.util.ArrayList;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.afollestad.overhearapi.Song;

/**
 * Various utilities for accessing the play queue content provider, also keeps track of your 
 * current position in the queue.
 * 
 * @author Aidan Follestad
 */
public class Queue {
	
	public final static Uri PROVIDER_URI = Uri.parse("content://com.afollestad.overhear.queueprovider");

	private static Cursor openCursor(Context context, String where) {
		return context.getContentResolver().query(PROVIDER_URI, null, where, null, Song.DATE_QUEUED);
	}
	
	public static void setQueue(Context context, ArrayList<Song> songs) {
		clearQueue(context);
		for(Song song : songs) {
			addToQueue(context, song);
		}
	}
	
	public static ArrayList<Song> getQueue(Context context) {
		ArrayList<Song> queue = new ArrayList<Song>();
		Cursor cursor = openCursor(context, null);
		while(cursor.moveToNext()) {
			queue.add(Song.fromCursor(cursor));
		}
		cursor.close();
		return queue;
	}
	
	public static boolean increment(Context context, boolean playing) {
		Cursor cursor = openCursor(context, null);
		boolean found = false;
		while(cursor.moveToNext()) {
			boolean hasFocus = (cursor.getInt(cursor.getColumnIndex(Song.QUEUE_FOCUS)) == 1);
			if(hasFocus) {
				if(cursor.moveToNext()) {
					clearFocused(context);
					int id = cursor.getInt(cursor.getColumnIndex("_id"));
					ContentValues values = new ContentValues();
					values.put(Song.QUEUE_FOCUS, 1);
					values.put(Song.NOW_PLAYING, playing ? 1 : 0);
					context.getContentResolver().update(PROVIDER_URI, values, "_id = " + id, null);
					found = true;
				} else {
					clearPlaying(context);
				}
				break;
			}
		}
		cursor.close();
		return found;
	}
	
	public static boolean decrement(Context context, boolean playing) {
		Cursor cursor = openCursor(context, null);
		boolean found = false;
		while(cursor.moveToNext()) {
			boolean hasFocus = (cursor.getInt(cursor.getColumnIndex(Song.QUEUE_FOCUS)) == 1);
			if(hasFocus) {
				int id = cursor.getInt(cursor.getColumnIndex("_id"));
				if(cursor.moveToPrevious()) {
					clearFocused(context);
					id = cursor.getInt(cursor.getColumnIndex("_id"));
					ContentValues values = new ContentValues();
					values.put(Song.QUEUE_FOCUS, 1);
					values.put(Song.NOW_PLAYING, playing ? 1 : 0);
					context.getContentResolver().update(PROVIDER_URI, values, "_id = " + id, null);
					found = true;
				} else {
					clearPlaying(context);
				}
				break;
			}
		}
		cursor.close();
		return found;
	}	

	public static void clearQueue(Context context) {
		context.getContentResolver().delete(PROVIDER_URI, null, null);
	}
	
	public static void addToQueue(Context context, Song song) {
		context.getContentResolver().insert(PROVIDER_URI, song.getContentValues(true));
	}

	public static void setFocused(Context context, Song song, boolean playing) {
		clearFocused(context);
		song.setIsPlaying(playing);
		song.setHasFocus(true);
		ContentValues values = new ContentValues();
		values.put(Song.NOW_PLAYING, playing ? 1 : 0);
		values.put(Song.QUEUE_FOCUS, 1);
		int updated = context.getContentResolver().update(PROVIDER_URI, values, "_id = " + song.getId(), null);
		if(updated == 0) {
			addToQueue(context, song);
		}
	}
	
	public static Song getFocused(Context context) {
		Song toreturn = null;
		Cursor cursor = openCursor(context, Song.QUEUE_FOCUS + " = 1 OR " + Song.NOW_PLAYING + " = 1");
		if(cursor.moveToFirst()) { 
			toreturn = Song.fromCursor(cursor);
		}
		cursor.close();
		return toreturn;
	}
	
	public static void clearPlaying(Context context) {
		ContentValues values = new ContentValues();
		values.put(Song.NOW_PLAYING, 0);
		context.getContentResolver().update(PROVIDER_URI, values, Song.NOW_PLAYING + " = 1", null);
	}
	
	public static void clearFocused(Context context) {
		ContentValues values = new ContentValues();
		values.put(Song.NOW_PLAYING, 0);
		values.put(Song.QUEUE_FOCUS, 0);
		context.getContentResolver().update(PROVIDER_URI, values, Song.QUEUE_FOCUS +
				" = 1 OR " + Song.NOW_PLAYING + " = 1", null);
	}
	
	public static Song poll(Context context) {
		Cursor cursor = openCursor(context, null);
		Song toreturn = null;
		if(cursor.moveToFirst()) { 
			toreturn = Song.fromCursor(cursor);
		}
		cursor.close();
		return toreturn;
	}
	
	public static Song pull(Context context) {
		Song pulled = poll(context);
		context.getContentResolver().delete(PROVIDER_URI, "_id = " + pulled.getId(), null);
		return pulled;
	}
}