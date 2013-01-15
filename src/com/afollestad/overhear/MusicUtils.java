package com.afollestad.overhear;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Song;

public class MusicUtils {
	
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

	public static void setRecents(Context context, ArrayList<Album> recents) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		try {
			JSONArray json = new JSONArray();
			for(Album album : recents) {
				json.put(album.getJSON());
			}
			prefs.edit().putString("recents", json.toString()).commit();
		} catch(Exception e) {
			throw new Error(e.getMessage());
		}
	}
	
	public static ArrayList<Album> getRecents(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		ArrayList<Album> recents = new ArrayList<Album>();
		if(!prefs.contains("recents")) {
			return recents;
		}
		try {
			JSONArray json = new JSONArray(prefs.getString("recents", null));
			for(int i = 0; i < json.length(); i++) {
				recents.add(Album.fromJSON(context, json.getJSONObject(i)));
			}
		} catch(Exception e) {
			throw new Error(e.getMessage());
		}
		return recents;
	}

	public static void setImageURL(Context context, String name, String url, String key) {
        context.getSharedPreferences(key, 0).edit()
        	.putString(name, url).commit();
    }

    public static String getImageURL(Context context, String name, String key) {
        return context.getSharedPreferences(key, 0)
        	.getString(name, null);
    }
    
    public static Bitmap getBitmapFromURL(String src) {
        try {
            URL url = new URL(src);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap myBitmap = BitmapFactory.decodeStream(input);
            return myBitmap;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Bitmap getResizedBitmap(Bitmap bitmap, int newHeight, int newWidth) {
        if (bitmap == null)
            return null;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float)newWidth) / width;
        float scaleHeight = ((float)newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    public static boolean isOnline(Context context) {
        boolean state = false;
        ConnectivityManager cm = (ConnectivityManager)context
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo wifiNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetwork != null) {
            state = wifiNetwork.isConnectedOrConnecting();
        }

        NetworkInfo mobileNetwork = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (mobileNetwork != null) {
            state = mobileNetwork.isConnectedOrConnecting();
        }

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            state = activeNetwork.isConnectedOrConnecting();
        }
        return state;
    }
}