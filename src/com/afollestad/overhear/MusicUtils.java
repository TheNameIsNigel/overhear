package com.afollestad.overhear;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

public class MusicUtils {
		
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
    	if(bitmap == null)
    		return null;
    	return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, false);
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