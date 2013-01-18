package com.afollestad.overhear;

import java.util.ArrayList;
import java.util.Calendar;

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
	
	private static int position = -1;
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
	
	private static Cursor openCursor(Context context) {
		return context.getContentResolver().query(
				Uri.parse("content://com.afollestad.overhear.queueprovider"),
				null, null, null, Song.DATE_QUEUED);
	}
	
	public static void addToQueue(Context context, Song song) {
		song.setDateQueued(Calendar.getInstance());
		context.getContentResolver().insert(
				Uri.parse("content://com.afollestad.overhear.queueprovider"), 
				song.getContentValues());
	}
	
	public static void setQueue(Context context, ArrayList<Song> songs) {
		clearQueue(context);
		for(Song song : songs) {
			addToQueue(context, song);
		}
	}
	
	public static ArrayList<Song> getQueue(Context context) {
		ArrayList<Song> queue = new ArrayList<Song>();
		Cursor cursor = openCursor(context);
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
		Cursor cursor = openCursor(context);
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
	
	/**
	 * Persists the queue position for when the app is closed.
	 */
	public static void saveQueuePosition(Context context) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("queue_pos", position).commit();
	}
	
	public static void setQueuePosition(Context context, int position) {
		PreferenceManager.getDefaultSharedPreferences(context).edit().putInt("queue_pos", position).commit();
	}
	
	public static Song getPrevious(Context context) {
		return getQueue(context).get(position);
	}
}