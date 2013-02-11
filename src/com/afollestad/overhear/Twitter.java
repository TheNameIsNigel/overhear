package com.afollestad.overhear;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import com.afollestad.overhearapi.Artist;
import twitter4j.TwitterFactory;

public class Twitter {

    public final static Uri ALTERNATE_TWITTER_URI = Uri.parse("content://com.afollestad.overhear.artistsocialaccounts");

    public static twitter4j.Twitter getTwitterInstance(Context context, boolean nullIfNotAuthenticated) {
        twitter4j.Twitter client = new TwitterFactory().getInstance();
        client.setOAuthConsumer("DlG3XT5adlDNRKUkZMMvA", "hDzUkzmge2gHwBP6AWdLNql2q2fdAN61enmfJBooZU");
        SharedPreferences prefs = context.getSharedPreferences("twitter_account", 0);
        String token = prefs.getString("token", null);
        String secret = prefs.getString("secret", null);
        if(token == null || secret == null) {
            if(nullIfNotAuthenticated)
                return null;
            else
                client.setOAuthAccessToken(null);
        } else {
            client.setOAuthAccessToken(new twitter4j.auth.AccessToken(token, secret));
        }

        return client;
    }

    public static void setSocialAccount(Context context, Artist artist, long userId) {
        ContentValues values = new ContentValues();
        values.put("twitter_id", userId);
        int updated = context.getContentResolver().update(ALTERNATE_TWITTER_URI, values,
                "artist_name = '" + artist.getName().replace("'", "''") + "'", null);
        if(updated == 0) {
            values.put("artist_name", artist.getName());
            context.getContentResolver().insert(ALTERNATE_TWITTER_URI, values);
        }
    }

    public static long getSocialAccount(Context context, Artist artist) {
        Cursor cursor = context.getContentResolver().query(ALTERNATE_TWITTER_URI, null,
                "artist_name = '" + artist.getName().replace("'", "''") + "'", null, null);
        long toreturn = -1l;
        if(cursor.moveToFirst()) {
            toreturn = cursor.getLong(cursor.getColumnIndex("twitter_id"));
        }
        cursor.close();
        return toreturn;
    }
}
