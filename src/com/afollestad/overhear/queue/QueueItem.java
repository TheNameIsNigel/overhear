package com.afollestad.overhear.queue;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.afollestad.overhearapi.Song;

public class QueueItem {

	private QueueItem(int id, long playlist, String data, int scope) {
		this.songId = id;
		this.playlistId = playlist;
		this.data = data;
		this.scope = scope;
	}
	public QueueItem(Song song, int scope) {
		this.songId = song.getId();
		this.playlistId = song.getPlaylistId();
		this.data = song.getData();
	}
	
	private int songId;
	private long playlistId;
	private String data;
	private int scope;

	public final static int SCOPE_SINGULAR = 0;
	public final static int SCOPE_All_SONGS = 1;
	public final static int SCOPE_ALBUM = 2;
	public final static int SCOPE_PLAYLIST = 3;
	public final static int SCOPE_GENRE = 4;
	public final static int SCOPE_ARTIST = 5;
	
	public int getSongId() {
		return songId;
	}
	
	public long getPlaylistId() {
		return playlistId;
	}
	
	public String getData() {
		return data;
	}
	
	public int getScope() {
		return scope;
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
			json.put("scope", getScope());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	
	public static QueueItem fromJSON(JSONObject json) {
		try {
			return new QueueItem(
					json.optInt("song_id", -1), 
					json.optLong("playlist_id", -1), 
					json.optString("data"),
					json.optInt("scope", 0));
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
