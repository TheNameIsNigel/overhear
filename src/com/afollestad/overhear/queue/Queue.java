package com.afollestad.overhear.queue;

import java.util.ArrayList;

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
		this.items = new ArrayList<QueueItem>();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		try {
			JSONArray array = new JSONArray(prefs.getString("queue", "[]"));
			for(int i = 0; i < array.length(); i++)
				items.add(QueueItem.fromJSON(array.getJSONObject(i))); 
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		this.pos = prefs.getInt("pos", -1);
	}
	
	private Context context;
	private ArrayList<QueueItem> items;
	private int pos = -1;

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
		return pos;
	}
	
	/**
	 * Attempts to increment the queue (move to the next song), returns false if that's not possible (end of queue).
	 */
	public boolean increment() {
    	if((getPosition() + 1) >= (items.size() - 1)) {
    		return false;
    	}
    	pos++;
    	return true;
    }
    
	/**
	 * Attempts to decrement the queue (move to the previous song), returns false if that's not possible (already at the beginning of the queue). 
	 */
    public boolean decrement() {
    	if((getPosition() - 1) < 0 || items.size() == 0) {
    		return false;
    	}
    	pos--;
    	return true; 
    }
    
    /**
     * Adds a song to the queue. The scope indicates where the song was loaded from.
     */
    public void add(Song song, int scope) {
    	items.add(new QueueItem(song, scope));
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
    	items.clear();
    	add(songs, scope);
    }
    
    /**
     * Finds a song in the queue, returns the index of the song, or -1 if it's not found.
     */
    public int find(QueueItem item) {
    	for(int index = 0; index < items.size(); index++) {
    		if(items.get(index).getSongId() == item.getSongId() &&
    				items.get(index).getPlaylistId() == item.getPlaylistId() &&
    				items.get(index).getScope() == item.getScope()) {
    			return index;
    		}
    	}
    	return -1;
    }
    
    /**
     * Moves to a position in the queue. Returns true if successful.
     */
    public boolean move(int position) {
    	if(position > (items.size() - 1) || items.size() == 0 || position < 0) {
    		this.pos = -1;
    		return false;
    	}
    	this.pos = position;
    	return true;
    }
    
    /**
     * Checks if the queue contains a song.
     */
    public boolean contains(QueueItem song) {
    	if(song == null) {
    		return false;
    	}
    	for(int index = 0; index < items.size(); index++) {
    		if(items.get(index).getSongId() == song.getSongId() &&
    				items.get(index).getPlaylistId() == song.getPlaylistId() &&
    				items.get(index).getScope() == song.getScope()) {
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
    	} else if(items.size() == 0) {
    		return null;
    	}
    	return items.get(getPosition()); 
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
		prefs.putInt("pos", getPosition());
		prefs.commit();
		Log.i("Queue", "Persisted " + items.size() + " queue items, position " + pos);
	}
}