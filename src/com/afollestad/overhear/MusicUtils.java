package com.afollestad.overhear;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MusicUtils {
		
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
}