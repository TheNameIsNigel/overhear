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
    
    public void add(Song song) {
    	items.add(new QueueItem(song.getId(), song.getPlaylistId(), song.getData()));
    }
   
    public void add(ArrayList<Song> songs) {
    	for(Song s : songs)
    		add(s);
    }
    
    public void set(ArrayList<Song> songs) {
    	items.clear();
    	add(songs);
    }
    
    public int find(Song song) {
    	for(int index = 0; index < items.size(); index++) {
    		if(items.get(index).getSongId() == song.getId() &&
    				items.get(index).getPlaylistId() == song.getPlaylistId()) {
    			return index;
    		}
    	}
    	return -1;
    }
    
    public void move(int position) {
    	this.pos = position;
    }
    
    public boolean contains(Song song) {
    	if(song == null) {
    		return false;
    	}
    	for(int index = 0; index < items.size(); index++) {
    		if(items.get(index).getSongId() == song.getId() &&
    				items.get(index).getPlaylistId() == song.getPlaylistId()) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public Song getFocused() {
    	if(getPosition() == -1) {
    		return null;
    	} else if(items.size() == 0) {
    		return null;
    	}
    	QueueItem item = items.get(getPosition());
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
		
		Log.i("MusicService", "onDestroy() - Persisted " + items.size() + " queue items, position " + pos);
	}
}