package com.afollestad.overhear;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.afollestad.overhearapi.Song;

public class MusicUtils {
	
	public static void setNowPlaying(Context context, Song song) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if(song == null) {
			prefs.edit().remove("now_playing").commit();
		} else { 
			prefs.edit().putString("now_playing", song.getJSON().toString()).commit();
		}
	}
	
	public static Song getNowPlaying(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if(!prefs.contains("now_playing")) {
			return null;
		}
		try {
			return Song.fromJSON(new JSONObject(prefs.getString("now_playing", null)));
		} catch(Exception e) {
			throw new Error(e.getMessage());
		}
	}
	
	public static void setLastPlaying(Context context, Song song) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if(song == null) {
			prefs.edit().remove("last_playing").commit();
		} else { 
			prefs.edit().putString("last_playing", song.getJSON().toString()).commit();
		}
	}
	
	public static Song getLastPlaying(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if(!prefs.contains("last_playing")) {
			return null;
		}
		try {
			return Song.fromJSON(new JSONObject(prefs.getString("last_playing", null)));
		} catch(Exception e) {
			throw new Error(e.getMessage());
		}
	}
}
