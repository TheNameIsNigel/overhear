package com.afollestad.overhear.queue;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.afollestad.overhearapi.Song;

/**
 * Represents an item contained in a {@link Queue}
 * 
 * @author Aidan Follestad
 */
public class QueueItem {

	private QueueItem(int id, long playlist, String title, String data, String artist, String album, long duration, int scope) {
		this.songId = id;
		this.playlistId = playlist;
		this.title = title;
		this.data = data;
		this.artist = artist;
		this.album = album;
		this.duration = duration;
		this.scope = scope;
	}
	/**
	 * Initializes a QueueItem from a Song. The scope indicates where the song was loaded from.
	 */
	public QueueItem(Song song, int scope) {
		this.songId = song.getId();
		this.playlistId = song.getPlaylistId();
		this.title = song.getTitle();
		this.data = song.getData();
		this.artist = song.getArtist();
		this.album = song.getAlbum();
		this.duration = song.getDuration();
		this.scope = scope;
	}
	
	private int songId;
	private long playlistId;
	private String title;
	private String data;
	private String artist;
	private String album;
	private long duration;
	private int scope;

	/**
	 * The song was loaded outside of any scope.
	 */
	public final static int SCOPE_SINGULAR = 0;
	/**
	 * The song was loaded from the entire library of songs (e.g. the "Songs" tab in the OverviewScreen).
	 */
	public final static int SCOPE_All_SONGS = 1;
	/**
	 * The song was loaded from an album (e.g. in the album viewer).
	 */
	public final static int SCOPE_ALBUM = 2;
	/**
	 * The song was loaded from a playlist (e.g. in the playlist viewer).
	 */
	public final static int SCOPE_PLAYLIST = 3;
	/**
	 * The song was loaded from a genre (e.g. in the genre viewer).
	 */
	public final static int SCOPE_GENRE = 4;
	/**
	 * The song was loaded from an artist (e.g. in the artist viewer).
	 */
	public final static int SCOPE_ARTIST = 5;
	
	/**
	 * Gets the ID of the song, this value is different when loaded from a playlist.
	 */
	public int getSongId() {
		return songId;
	}
	
	/**
	 * Gets the ID of the playlist that the song was loaded from.
	 */
	public long getPlaylistId() {
		return playlistId;
	}
	
	/**
	 * Gets the name of the song.
	 * @return
	 */
	public String getTitle() {
		return title;
	}
	
	/**
	 * Gets the data (path to the media file) of the song.
	 * @return
	 */
	public String getData() {
		return data;
	}
	
	/**
	 * Gets the artist the song belongs to.
	 */
	public String getArtist() {
		return artist;
	}
	
	/**
	 * Gets the album the song belongs to.
	 */
	public String getAlbum() {
		return album;
	}
	
	/**
	 * Gets the duration (in milliseconds) of the song.
	 */
	public long getDuration() {
		return duration;
	}
	
	/**
	 * Gets the scope, which indicates where the song was loaded from.
	 */
	public int getScope() {
		return scope;
	}
	
	/**
	 * Gets a {@link Song} instance from the QueueItem.
	 */
	public Song getSong(Context context) {
		return Song.fromId(context, getSongId());
	}
	
	public JSONObject getJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("song_id", getSongId());
			json.put("playlist_id", getPlaylistId());
			json.put("title", getTitle());
			json.put("data", getData());
			json.put("album", getAlbum());
			json.put("artist", getArtist());
			json.put("duration", getDuration());
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
					json.optString("title"),
					json.optString("data"),
					json.optString("artist"),
					json.optString("album"),
					json.optLong("duration"),
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
