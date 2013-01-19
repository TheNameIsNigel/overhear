package com.afollestad.overhear;

import java.util.ArrayList;
import java.util.Calendar;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.afollestad.overhearapi.Song;

/**
 * Various utilities for accessing the play queue content provider, also keeps track of your 
 * current position in the queue.
 * 
 * @author Aidan Follestad
 */
public class QueueUtils {
	
	public static int position = -1;
	private static int getPosition(Context context) {
		if(position == -1) {
			position = PreferenceManager.getDefaultSharedPreferences(context).getInt("queue_pos", 0);
		}
		return position;
	}
	
	public static boolean increment(Context context) {
		int queueSize = getQueue(context).size();
		if((getPosition(context) + 1) > (queueSize - 1)) {
			position++;
		} else {
			return false;
		}
		return true;
	}
	
	public static boolean decrement(Context context) {
		if((position - 1) >= 0) {
			position--;
		} else {
			return false;
		}
		return true;
	}

	public static void clearQueue(Context context) {
		context.getContentResolver().delete(Uri.parse("content://com.afollestad.overhear.queueprovider"), null, null);
		position = 0;
		PreferenceManager.getDefaultSharedPreferences(context).edit().remove("queue_pos").commit();
	}
	
	private static Cursor openCursor(Context context, String where) {
		return context.getContentResolver().query(
				Uri.parse("content://com.afollestad.overhear.queueprovider"),
				null, where, null, Song.DATE_QUEUED);
	}
	
	public static void addToQueue(Context context, Song song) {
		context.getContentResolver().insert(
				Uri.parse("content://com.afollestad.overhear.queueprovider"), 
				song.getContentValues(true));
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
	
	/**
	 * Gets the top item in the queue (the item that would come next); this method does not remove it from the queue,
	 * use {@link #pull(Context)} for that.
	 */
	public static Song poll(Context context) {
		Cursor cursor = openCursor(context, null);
		Song toreturn = null;
		if(cursor.moveToFirst()) { 
			toreturn = Song.fromCursor(cursor);
		}
		cursor.close();
		return toreturn;
	}
	
	/**
	 * Gets the top item in the queue and removes it.
	 */
	public static Song pull(Context context) {
		Song pulled = poll(context);
		context.getContentResolver().delete(Uri.parse("content://com.afollestad.overhear.queueprovider"), 
				"_id = " + pulled.getId(), null);
		return pulled;
	}
	
	public static Song getNowPlaying(Context context) {
		Song toreturn = null;
		Cursor cursor = openCursor(context, Song.NOW_PLAYING + " = 1");
		if(cursor.moveToFirst()) { 
			toreturn = Song.fromCursor(cursor);
		}
		cursor.close();
		return toreturn;
	}
	
	public static void clearNowPlaying(Context context) {
		ContentValues values = new ContentValues();
		values.put(Song.NOW_PLAYING, 0);
		context.getContentResolver().update(
				Uri.parse("content://com.afollestad.overhear.queueprovider"), 
				values, Song.NOW_PLAYING + " = 1", null);
	}
	
	public static void setNowPlaying(Context context, Song song) {
		ContentValues values = new ContentValues();
		values.put(Song.NOW_PLAYING, 1);
		int updated = context.getContentResolver().update(
				Uri.parse("content://com.afollestad.overhear.queueprovider"), 
				values, "_id = " + song.getId(), null);
		if(updated == 0) {
			song.setIsPlaying(true);
			addToQueue(context, song);
		}
	}

	public static void setMostRecent(Context context, Song song) {
		song.setDateQueued(Calendar.getInstance());
		ContentValues values = new ContentValues();
		values.put(Song.DATE_QUEUED, song.getDateQueued().getTimeInMillis());
		int updated = context.getContentResolver().update(
				Uri.parse("content://com.afollestad.overhear.queueprovider"), 
				values, "_id = " + song.getId(), null);
		if(updated == 0) {
			addToQueue(context, song);
		}
	}
	
	/**
	 * Persists the queue position for when the app is closed.
	 */
	public static void saveQueuePosition(Context context) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("queue_pos", position).commit();
	}
	
	public static Song getPrevious(Context context) {
		ArrayList<Song> queue = getQueue(context);
		if((position - 1) < 0 || queue.size() == 0) {
			return null;
		}
		return queue.get(position - 1);
	}
}