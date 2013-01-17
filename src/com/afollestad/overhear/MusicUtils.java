package com.afollestad.overhear;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;

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
    
    public static void browseArtist(Context context, String artistName) {
    	try {
    		Uri uri = Uri.parse("https://play.google.com/store/search?q=" +
    				URLEncoder.encode(artistName, "UTF-8") + "&c=music");
			context.startActivity(new Intent(Intent.ACTION_VIEW).setData(uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
    }
    
    public static Bitmap getLocalAlbumArt(Context context, Uri uri) {
    	InputStream input = null;
	    try {
	        input = context.getContentResolver().openInputStream(uri);
	        if (input == null)
	            return null;
	        Bitmap toreturn = BitmapFactory.decodeStream(input);
	        input.close();
	        return toreturn;
	    } catch(Exception e) {
	    	e.printStackTrace();
	    }
	    return null;
    }
}