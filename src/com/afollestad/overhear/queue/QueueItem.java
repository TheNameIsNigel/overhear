package com.afollestad.overhear.queue;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.afollestad.overhearapi.Song;

public class QueueItem {

	public QueueItem(int songId, long playlistId, String data) {
		this.songId = songId;
		this.playlistId = playlistId;
		this.data = data;
	}
	
	private int songId;
	private long playlistId;
	private String data;
	
	public int getSongId() {
		return songId;
	}
	
	public long getPlaylistId() {
		return playlistId;
	}
	
	public String getData() {
		return data;
	}
	
	public Song getSong(Context context) {
		if(getPlaylistId() > -1) {
			return Song.fromData(context, getData());
		}
		return Song.fromId(context, getSongId());
	}
	
	public JSONObject getJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("song_id", getSongId());
			json.put("playlist_id", getPlaylistId());
			json.put("data", getData());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	public static QueueItem fromJSON(JSONObject json) {
		try {
			return new QueueItem(
					json.getInt("song_id"), 
					json.getLong("playlist_id"), 
					json.getString("data"));
		} catch(Exception e) {
			return null;
		}
	}
	
	public static QueueItem fromJSON(String json) {
		try {
			return fromJSON(new JSONObject(json));
		} catch(Exception e) {
			return null;
		}
	}
}
