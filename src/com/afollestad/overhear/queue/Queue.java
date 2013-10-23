package com.afollestad.overhear.queue;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Random;

/**
 * Used for creating, modifying, managing, and modifying the music service's queue.
 *
 * @author Aidan Follestad
 */
public class Queue {

    /**
     * Initializes the queue, and loads any persisted queue data.
     */
    public Queue(Context context) {
        this.items = new ArrayList<QueueItem>();
        this.shuffler = new Shuffler(context);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        try {
            JSONArray array = new JSONArray(prefs.getString("queue", "[]"));
            for (int i = 0; i < array.length(); i++)
                items.add(QueueItem.fromJSON(array.getJSONObject(i)));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        this.position = prefs.getInt("pos", -1);
        this.repeatMode = prefs.getInt("repeat_mode", REPEAT_MODE_OFF);
    }


    private ArrayList<QueueItem> items;
    private int position = -1;
    private int repeatMode;
    private boolean shuffle;
    private Shuffler shuffler;

    public final static int REPEAT_MODE_OFF = 0;
    public final static int REPEAT_MODE_ONCE = 1;
    public final static int REPEAT_MODE_ALL = 2;


    /**
     * Gets the items (songs) in the queue.
     */
    public ArrayList<QueueItem> getItems() {
        return items;
    }


    /**
     * Gets the current queue position of the focused song (the item that is currently playing or paused).
     */
    public int getPosition() {
        return position;
    }

    /**
     * Checks if you can safely call the increment() method.
     */
    public boolean canIncrement() {
        boolean repeat = getRepeatMode() == REPEAT_MODE_ONCE || getRepeatMode() == REPEAT_MODE_ALL;
        if (getItems().size() > 0 && repeat)
            return true;
        else
            return isShuffleOn() || (position + 1) < getItems().size();
    }

    /**
     * Checks if you can safely call the decrement() method.
     */
    public boolean canDecrement() {
        boolean repeat = getRepeatMode() == REPEAT_MODE_ONCE || getRepeatMode() == REPEAT_MODE_ALL;
        if (getItems().size() > 0 && repeat) {
            return true;
        } else
            return isShuffleOn() || (position - 1) >= 0 || getItems().size() == 0;
    }

    /**
     * Increments to the next position in the queue (you must use canIncrement() before calling this method).
     */
    public QueueItem increment() {
        if (getRepeatMode() == REPEAT_MODE_ONCE || getRepeatMode() == REPEAT_MODE_ALL) {
            if (position == -1)
                position = 0;
            // The position is maintained, and it's turned off if only need to repeat once.
            if (getRepeatMode() == REPEAT_MODE_ONCE)
                setRepeatMode(REPEAT_MODE_OFF);
        } else {
            if (shuffle) {
                position = shuffler.nextInt(this);
            } else {
                position++;
            }
        }
        return getFocused();
    }

    /**
     * Decrements to the last position in the queue (you must use canDecrement() before calling this method).
     */
    public QueueItem decrement() {
        if (position == -1) {
            position = 0;
        } else {
            position--;
        }
        return getFocused();
    }


    /**
     * Adds an item to the queue.
     */
    public void add(QueueItem item) {
        items.add(item);
    }

    public void add(ArrayList<QueueItem> items) {
        for (QueueItem i : items)
            add(i);
    }

    /**
     * Sets the entire queue, equivalent to clearing the queue and adding each item individually.
     */
    public void set(ArrayList<QueueItem> items) {
        this.items.clear();
        add(items);
        shuffler.reset();
    }

    /**
     * Finds a song in the queue, returns the index of the song, or -1 if it's not found.
     */
    public int find(QueueItem item) {
        for (int index = 0; index < getItems().size(); index++) {
            if (getItems().get(index).getSongId() == item.getSongId() &&
                    getItems().get(index).getPlaylistId() == item.getPlaylistId() &&
                    getItems().get(index).getScope() == item.getScope()) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Moves to a position in the queue. Returns true if successful.
     */
    public boolean move(int position) {
        if (position > (getItems().size() - 1) || getItems().size() == 0 || position < 0) {
            this.position = -1;
            return false;
        }
        this.position = position;
        this.shuffler.reset();
        return true;
    }


    public boolean isShuffleOn() {
        return this.shuffle;
    }

    public boolean toggleShuffle() {
        this.shuffle = !this.shuffle;
        if (this.shuffle)
            this.shuffler.setPrevious(getPosition());
        else
            this.shuffler.reset();
        return this.shuffle;
    }


    public void setRepeatMode(int mode) {
        this.repeatMode = mode;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    /**
     * Goes to the next queue mode based on the current (off to once, once to always, always to off).
     */
    public void nextRepeatMode() {
        switch (getRepeatMode()) {
            case Queue.REPEAT_MODE_OFF:
                setRepeatMode(Queue.REPEAT_MODE_ALL);
                break;
            case Queue.REPEAT_MODE_ALL:
                setRepeatMode(Queue.REPEAT_MODE_ONCE);
                break;
            case Queue.REPEAT_MODE_ONCE:
                setRepeatMode(Queue.REPEAT_MODE_OFF);
                break;
        }
    }


    /**
     * Checks if the queue contains a song.
     */
    public boolean contains(QueueItem song) {
        if (song == null)
            return false;
        for (int index = 0; index < getItems().size(); index++) {
            if (getItems().get(index).getSongId() == song.getSongId() &&
                    getItems().get(index).getPlaylistId() == song.getPlaylistId() &&
                    getItems().get(index).getScope() == song.getScope()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the currently focused item in the queue (the currently playing or last paused song).
     */
    public QueueItem getFocused() {
        if (getPosition() == -1) {
            return null;
        } else if (getItems().size() == 0) {
            return null;
        }
        return getItems().get(getPosition());
    }


    /**
     * Persists the queue in the local application preferences so it can be reloaded the next time the queue is initialized.
     */
    public void persist(Context context) {
        SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        JSONArray array = new JSONArray();
        for (QueueItem item : getItems())
            array.put(item.getJSON());

        prefs.putString("queue", array.toString());
        prefs.putInt("pos", position);
        prefs.putInt("repeat_mode", getRepeatMode());
        prefs.commit();

        this.shuffler.persist(context);
        Log.i("Queue", "Persisted " + getItems().size() + " queue items, position " + position);
    }


    private static class Shuffler {

        public Shuffler(Context context) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            mPrevious = prefs.getInt("shuffle_previous", -1);
        }

        private int mPrevious = -1;
        private Random mRandom = new Random();

        public int nextInt(Queue queue) {
            if (queue.getItems().size() == 0) {
                return -1;
            } else if (queue.getItems().size() == 1) {
                return 0;
            }
            int ret = 0;
            int interval = queue.getItems().size();
            do {
                ret = mRandom.nextInt(interval);
            } while (ret == mPrevious);
            return ret;
        }

        public void setPrevious(int previous) {
            mPrevious = previous;
        }

        public void reset() {
            mPrevious = -1;
        }

        public void persist(Context context) {
            SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
            prefs.putInt("shuffle_previous", mPrevious);
            prefs.commit();
        }
    }
}