package com.afollestad.overhear;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import com.afollestad.overhearapi.Song;

import java.util.ArrayList;

/**
 * Various utilities for accessing the play queue content provider, also keeps track of your
 * current position in the queue.
 *
 * @author Aidan Follestad
 */
public class Queue {

    public final static Uri PROVIDER_URI = Uri.parse("content://com.afollestad.overhear.queueprovider");

    private static Cursor openCursor(Context context, String where) {
        return context.getContentResolver().query(PROVIDER_URI, null, where, null, Song.QUEUE_ID);
    }

    public static ArrayList<Song> setQueue(Context context, ArrayList<Song> songs) {
        Log.i("QUEUE", "setQueue(context, [" + songs.size() + "])");
        ArrayList<Song> toreturn = new ArrayList<Song>();
        clearQueue(context);
        for (Song song : songs) {
            toreturn.add(addToQueue(context, song));
        }
        return toreturn;
    }

    public static int canQueueSkip(Context context, Song targetSong) {
        Song focused = getFocused(context);
        Cursor cursor = openCursor(context, null);
        ArrayList<Integer> matchIndexes = new ArrayList<Integer>();
        int focusedAlbumStart = -1;
        int focusedAlbumEnd = -1;

        // Search for matching tracks for what is going to be played
        while (cursor.moveToNext()) {

            Song currentSong = Song.fromCursor(cursor);
            if (currentSong.getId() == targetSong.getId()) {
                matchIndexes.add(cursor.getPosition());
            }

            if (focused != null) {
                if (currentSong.getArtist().equals(focused.getArtist()) && currentSong.getAlbum().equals(focused.getAlbum())) {
                    focusedAlbumStart = cursor.getPosition();
                } else {
                    focusedAlbumEnd = cursor.getPosition();
                }
            }
        }

        // Prioritize matches within the bounds of the album the focused song is from
        int index = -1;
        if (focusedAlbumStart > -1) {
            if (focusedAlbumEnd == -1)
                focusedAlbumEnd = cursor.getCount() - 1;
            for (Integer mi : matchIndexes) {
                if (mi >= focusedAlbumStart && mi <= focusedAlbumEnd) {
                    index = mi;
                    break;
                }
            }
        }

        // If no matches were within the focused album boumds, use the first match that was found.
        if (index == -1 && matchIndexes.size() > 0)
            index = matchIndexes.get(0);
        cursor.close();
        return index;
    }

    public static ArrayList<Song> getQueue(Context context) {
        Log.i("QUEUE", "getQueue(context)");
        ArrayList<Song> queue = new ArrayList<Song>();
        Cursor cursor = openCursor(context, null);
        while (cursor.moveToNext()) {
            queue.add(Song.fromCursor(cursor));
        }
        cursor.close();
        return queue;
    }

    public static boolean increment(Context context, boolean playing) {
        clearPlaying(context);
        Cursor cursor = openCursor(context, null);
        boolean found = false;
        while (cursor.moveToNext()) {
            boolean hasFocus = (cursor.getInt(cursor.getColumnIndex(Song.QUEUE_FOCUS)) == 1);
            if (hasFocus) {
                if (cursor.moveToNext()) {
                    int queueId = cursor.getInt(cursor.getColumnIndex(Song.QUEUE_ID));
                    clearFocused(context);
                    ContentValues values = new ContentValues();
                    values.put(Song.QUEUE_FOCUS, 1);
                    values.put(Song.NOW_PLAYING, playing ? 1 : 0);
                    context.getContentResolver().update(PROVIDER_URI, values, Song.QUEUE_ID + " = " + queueId, null);
                    found = true;
                }
                break;
            }
        }
        cursor.close();
        Log.i("QUEUE", "increment(context, playing = " + playing + ") = " + found);
        return found;
    }

    public static boolean decrement(Context context, boolean playing) {
        clearPlaying(context);
        Cursor cursor = openCursor(context, null);
        boolean found = false;
        while (cursor.moveToNext()) {
            boolean hasFocus = (cursor.getInt(cursor.getColumnIndex(Song.QUEUE_FOCUS)) == 1);
            if (hasFocus) {
                if (cursor.moveToPrevious()) {
                    int queueId = cursor.getInt(cursor.getColumnIndex(Song.QUEUE_ID));
                    clearFocused(context);
                    ContentValues values = new ContentValues();
                    values.put(Song.QUEUE_FOCUS, 1);
                    values.put(Song.NOW_PLAYING, playing ? 1 : 0);
                    context.getContentResolver().update(PROVIDER_URI, values, Song.QUEUE_ID + " = " + queueId, null);
                    found = true;
                }
                break;
            }
        }
        cursor.close();
        Log.i("QUEUE", "decrement(context, playing = " + playing + ") = " + found);
        return found;
    }

    public static void clearQueue(Context context) {
        Log.i("QUEUE", "clearQueue(context)");
        context.getContentResolver().delete(PROVIDER_URI, null, null);
    }

    public static Song addToQueue(Context context, Song song) {
        Cursor cursor = openCursor(context, null);
        song.setQueueId(cursor.getCount() + 1);
        cursor.close();
        context.getContentResolver().insert(PROVIDER_URI, song.getContentValues(true));
        Log.i("QUEUE", "addToQueue(context, \"" + song.getTitle() + "\") = " + song.getQueueId());
        return song;
    }

    public static ArrayList<Song> addToQueue(Context context, ArrayList<Song> songs) {
        ArrayList<Song> toreturn = new ArrayList<Song>();
        Cursor cursor = openCursor(context, null);

        int startPos = cursor.getCount();
        for (Song si : songs) {
            startPos++;
            si.setQueueId(startPos);
            toreturn.add(addToQueue(context, si));
        }

        cursor.close();
        return toreturn;
    }

    public static boolean setFocused(Context context, Song song, boolean playing) {
        clearFocused(context);
        song.setIsPlaying(playing);
        song.setHasFocus(true);
        ContentValues values = new ContentValues();
        values.put(Song.NOW_PLAYING, playing ? 1 : 0);
        values.put(Song.QUEUE_FOCUS, 1);
        int updated = context.getContentResolver().update(PROVIDER_URI, values, Song.QUEUE_ID + " = " + song.getQueueId(), null);
//		if(updated == 0) {
//			addToQueue(context, song);
//		}
        Log.i("QUEUE", "setFocused(context, \"" + song.getTitle() + "\" = " + song.getQueueId() + ", " + playing + ") = " + (updated > 0));
        return updated > 0;
    }

    public static Song getFocused(Context context) {
        Song toreturn = null;
        Cursor cursor = openCursor(context, Song.QUEUE_FOCUS + " = 1 OR " + Song.NOW_PLAYING + " = 1");
        if (cursor.moveToFirst()) {
            toreturn = Song.fromCursor(cursor);
        }
        cursor.close();
        Log.i("QUEUE", "getFocused(context) = \"" + (toreturn != null ? toreturn.getTitle() : "null") + "\"");
        return toreturn;
    }

    public static void clearPlaying(Context context) {
        Log.i("QUEUE", "clearPlaying(context)");
        ContentValues values = new ContentValues();
        values.put(Song.NOW_PLAYING, 0);
        context.getContentResolver().update(PROVIDER_URI, values, Song.NOW_PLAYING + " = 1", null);
    }

    public static void clearFocused(Context context) {
        Log.i("QUEUE", "clearFocused(context)");
        ContentValues values = new ContentValues();
        values.put(Song.NOW_PLAYING, 0);
        values.put(Song.QUEUE_FOCUS, 0);
        context.getContentResolver().update(PROVIDER_URI, values, Song.QUEUE_FOCUS +
                " = 1 OR " + Song.NOW_PLAYING + " = 1", null);
    }
}