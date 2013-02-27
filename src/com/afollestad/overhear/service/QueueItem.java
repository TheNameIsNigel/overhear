package com.afollestad.overhear.service;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.afollestad.overhearapi.Song;

public class QueueItem {

	private QueueItem() { }
    public QueueItem(int songId, long playlistId) {
    	this.songId = songId;
    	this.playlistId = playlistId;
    }

    private int songId;
    private long playlistId;
    
    public int getSongId() {
    	return songId;
    }
    
    public long getPlaylistId() {
    	return playlistId;
    }
    
    public Song getSong(Context context) {
    	return Song.fromId(context, getSongId());
    }
    
    public JSONObject getJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("song_id", this.songId);
			json.put("playlist_id", this.playlistId);
		} catch(Exception e) {
			e.printStackTrace();
		}
		return json;
	}
    
    public static QueueItem fromJSON(String json) {
    	try {
			return fromJSON(new JSONObject(json));
		} catch (JSONException e) {
			e.printStackTrace();
			return null;
		}
    }

	public static QueueItem fromJSON(JSONObject json) {
		QueueItem item = new QueueItem();
		try {
			item.songId = json.getInt("song_id");
			item.playlistId = json.getLong("playlist_id");
		} catch(Exception e) {
			e.printStackTrace();
		}
		return item;
	}
}