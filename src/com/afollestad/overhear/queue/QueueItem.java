package com.afollestad.overhear.queue;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.afollestad.overhearapi.Song;

public class QueueItem {

	private QueueItem(int id, long playlist, String data) {
		this.songId = id;
		this.playlistId = playlist;
		this.data = data;
	}
	public QueueItem(Song song) {
		this.songId = song.getId();
		this.playlistId = song.getPlaylistId();
		this.data = song.getData();
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
			Song song = Song.fromData(context, getData());
			song.setPlaylistId(getPlaylistId());
			return song;
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
