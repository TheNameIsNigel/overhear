package com.afollestad.overhear.queue;

import java.util.ArrayList;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * A basic class that handles shuffle algorithms for the Queue class. 
 * 
 * @author Aidan Follestad
 */
public class Shuffler {

	public Shuffler(Context context) {
		random = new Random();
		history = new ArrayList<Integer>();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		try {
			JSONArray array = new JSONArray(prefs.getString("history", "[]"));
			for(int i = 0; i < array.length(); i++)
				history.add(array.getInt(i));
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private Random random;
	private ArrayList<Integer> history;
	
	/**
	 * Generates a random index for the next song to be played from the queue while in shuffle mode. Automatically
	 * makes sure you don't get the same song twice in a row, and makes sure every song gets played once.
	 */
	public int nextIndex(Queue queue) {
		if(history.size() == queue.getItems().size()) {
			return -1;
		}
		
		int nextPos = random.nextInt(queue.getItems().size() - 1);
		do {
			nextPos = random.nextInt(queue.getItems().size() - 1);
		} while(
				history.size() > 0 && 
				(history.contains(Integer.valueOf(nextPos)) || 
				history.get(history.size() - 1) + 1 == nextPos));
		history.add(Integer.valueOf(nextPos));	
		return nextPos;
	}
	
	public int previousIndex() {
		if(history.size() == 0) {
			return -1;
		}
		return history.get(history.size() - 1);
	}
	
	public void persist(Context context) {
		SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
		JSONArray array = new JSONArray();
		for(Integer index : history)
			array.put(index);		
		prefs.putString("history", array.toString());
		prefs.commit();
		
		Log.i("Shuffler", "Persisted " + history.size() + " history items");
	} 
}
