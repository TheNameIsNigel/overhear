package com.afollestad.overhear.queue;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import com.afollestad.overhearapi.Song;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class Queue {

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

	public ArrayList<QueueItem> getItems() {
		return items;
	}
	
	public int getPosition() {
		return pos;
	}
	
	public boolean increment() {
    	if((getPosition() + 1) >= (items.size() - 1)) {
    		return false;
    	}
    	pos++;
    	return true;
    }
    
    public boolean decrement() {
    	if((getPosition() - 1) < 0 || items.size() == 0) {
    		return false;
    	}
    	pos--;
    	return true; 
    }
    
    public void add(Song song, int scope) {
    	items.add(new QueueItem(song, scope));
    }
   
    public void add(ArrayList<Song> songs, int scope) {
    	for(Song s : songs)
    		add(s, scope);
    }
    
    public void set(ArrayList<Song> songs, int scope) {
    	items.clear();
    	add(songs, scope);
    }
    
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
    
    public void move(int position) {
    	this.pos = position;
    }
    
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
    
    public QueueItem getFocusedItem() {
    	if(getPosition() == -1) {
    		return null;
    	} else if(items.size() == 0) {
    		return null;
    	}
    	return items.get(getPosition()); 
    }
    
    public Song getFocused() {
    	QueueItem item = getFocusedItem();
    	if(item == null) {
    		return null;
    	}
    	return item.getSong(context);
    }
    
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