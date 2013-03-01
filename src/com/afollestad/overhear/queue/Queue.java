package com.afollestad.overhear.queue;

import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;

import com.afollestad.overhearapi.Song;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

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
		this.context = context;
		this.queueItems = new ArrayList<QueueItem>();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		try {
			JSONArray array = new JSONArray(prefs.getString("queue", "[]"));
			for(int i = 0; i < array.length(); i++)
				queueItems.add(QueueItem.fromJSON(array.getJSONObject(i)));
			array = new JSONArray(prefs.getString("shuffled_queue", "[]"));
			if(array.length() > 0) {
				this.shuffledItems = new ArrayList<QueueItem>();
				for(int i = 0; i < array.length(); i++)
					shuffledItems.add(QueueItem.fromJSON(array.getJSONObject(i)));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		this.pos = prefs.getInt("pos", -1);
		this.repeatMode = prefs.getInt("repeat_mode", REPEAT_MODE_OFF);
	}
	
	private Context context;
	private ArrayList<QueueItem> queueItems;
	private ArrayList<QueueItem> shuffledItems;
	private int pos = -1;
	private int repeatMode;
	
	public final static int REPEAT_MODE_OFF = 0;
	public final static int REPEAT_MODE_ONCE = 1;
	public final static int REPEAT_MODE_ALL = 2;

	/**
	 * Gets the items (songs) in the queue.
	 */
	public ArrayList<QueueItem> getItems() {
		if(this.shuffledItems != null && this.shuffledItems.size() > 0) {
			return this.shuffledItems;
		}
		return queueItems;
	}
	
	/**
	 * Gets the current queue position of the focused song (the item that is currently playing or paused).
	 */
	public int getPosition() {
		return pos;
	}
	
	/**
	 * Checks if you can safely call the increment() method.
	 */
	public boolean canIncrement() {
		boolean repeat = getRepeatMode() == REPEAT_MODE_ONCE || getRepeatMode() == REPEAT_MODE_ALL;
		if(getItems().size() > 0 && repeat) {
			return true;
		} else {
			return (getPosition() + 1) < getItems().size();
		}
	}
	
	/**
	 * Checks if you can safely call the decrement() method.
	 */
	public boolean canDecrement() {
		boolean repeat = getRepeatMode() == REPEAT_MODE_ONCE || getRepeatMode() == REPEAT_MODE_ALL;
		if(getItems().size() > 0 && repeat) {
			return true;
		} else {
			return (getPosition() - 1) >= 0 || getItems().size() == 0;
		}
	}
	
	/**
	 * Increments to the next position in the queue (you must use canIncrement() before calling this method).
	 */
	public QueueItem increment() {
		if(getRepeatMode() == REPEAT_MODE_ONCE || getRepeatMode() == REPEAT_MODE_ALL) {
			// The position is maintained, and it's turned off if only need to repeat once.
			if(getRepeatMode() == REPEAT_MODE_ONCE)
				setRepeatMode(REPEAT_MODE_OFF);
		} else {
			pos++;
		}
		return getFocusedItem();
    }
    
	/**
	 * Decrements to the last position in the queue (you must use canDecrement() before calling this method). 
	 */
    public QueueItem decrement() {
    	if(getRepeatMode() == REPEAT_MODE_ONCE || getRepeatMode() == REPEAT_MODE_ALL) {
			// The position is maintained, and it's turned off if only need to repeat once.
			if(getRepeatMode() == REPEAT_MODE_ONCE)
				setRepeatMode(REPEAT_MODE_OFF);
		} else {
			pos--;
		} 
    	return getFocusedItem();
    }
    
    /**
     * Adds a song to the queue. The scope indicates where the song was loaded from.
     */
    public void add(Song song, int scope) {
    	queueItems.add(new QueueItem(song, scope));
    }
   
    /**
     * Adds a list of songs to the queue. The scope indicates where the songs were loaded from.
     */
    public void add(ArrayList<Song> songs, int scope) {
    	for(Song s : songs)
    		add(s, scope);
    }
    
    /**
     * Sets the entire queue, equivalent to clearing the queue and then using {@link #add(ArrayList, int)}.
     */
    public void set(ArrayList<Song> songs, int scope) {
    	queueItems.clear();
    	add(songs, scope);
    }
    
    /**
     * Finds a song in the queue, returns the index of the song, or -1 if it's not found.
     */
    public int find(QueueItem item) {
    	for(int index = 0; index < getItems().size(); index++) {
    		if(getItems().get(index).getSongId() == item.getSongId() &&
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
    	if(position > (getItems().size() - 1) || getItems().size() == 0 || position < 0) {
    		this.pos = -1;
    		return false;
    	}
    	this.pos = position;
    	return true;
    }

    /**
     * Generates a random index within the bound of the queue and moves to that position.
     */
    private static ArrayList<QueueItem> shuffle(ArrayList<QueueItem> items) {
    	Random random = new Random();
    	ArrayList<QueueItem> shuffledItems = new ArrayList<QueueItem>();
    	ArrayList<QueueItem> unusedItems= new ArrayList<QueueItem>(items);
    	
    	while(unusedItems.size() > 0) {
    		int nextPos = random.nextInt(unusedItems.size() - 1);
    		shuffledItems.add(unusedItems.get(nextPos));
    		unusedItems.remove(nextPos);
    	}        
    	
        return shuffledItems;
    }
    
    public boolean isShuffleOn() {
    	return this.shuffledItems != null;
    }
    
    public boolean toggleShuffle() {
    	if(this.shuffledItems == null) {
    		this.shuffledItems = shuffle(this.queueItems);
    		return true;
    	} else {
    		this.shuffledItems = null;
    		return false;
    	}
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
    	switch(getRepeatMode()) {
		case Queue.REPEAT_MODE_OFF:
			setRepeatMode(Queue.REPEAT_MODE_ONCE);
			break;
		case Queue.REPEAT_MODE_ONCE:
			setRepeatMode(Queue.REPEAT_MODE_ALL);
			break;
		case Queue.REPEAT_MODE_ALL:
			setRepeatMode(Queue.REPEAT_MODE_OFF);
			break;
		}
    }
    
    /**
     * Checks if the queue contains a song.
     */
    public boolean contains(QueueItem song) {
    	if(song == null) {
    		return false;
    	}
    	for(int index = 0; index < getItems().size(); index++) {
    		if(getItems().get(index).getSongId() == song.getSongId() &&
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
    public QueueItem getFocusedItem() {
    	if(getPosition() == -1) {
    		return null;
    	} else if(getItems().size() == 0) {
    		return null;
    	}
    	return getItems().get(getPosition()); 
    }
    
    /**
     * Gets the currently focused song in the queue (the currently playing or last paused song).
     * Equivalent to using {@link #getFocusedItem()}.getSong(context).
     */
    public Song getFocused() {
    	QueueItem item = getFocusedItem();
    	if(item == null) {
    		return null;
    	}
    	return item.getSong(context);
    }
    
    /**
     * Persists the queue in the local application preferences so it can be reloaded the next time the queue is initialized.
     */
	public void persist(Context context) {
		SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
		JSONArray array = new JSONArray();
		for(QueueItem item : getItems())
			array.put(item.getJSON());
		prefs.putString("queue", array.toString());
		if(shuffledItems != null) {
			array = new JSONArray();
			for(QueueItem item : shuffledItems)
				array.put(item.getJSON());
			prefs.putString("shuffled_queue", array.toString());
		}
		prefs.putInt("pos", getPosition());
		prefs.putInt("repeat_mode", getRepeatMode());
		prefs.commit();
		Log.i("Queue", "Persisted " + getItems().size() + " queue items, position " + pos);
	}
}