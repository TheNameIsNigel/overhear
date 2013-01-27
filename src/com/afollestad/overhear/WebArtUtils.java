package com.afollestad.overhear;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.WebArt;

public class WebArtUtils {

    public final static Uri ALBUMS_URI = Uri.parse("content://com.afollestad.overhear.webartprovider/albums");
    public final static Uri ARTISTS_URI = Uri.parse("content://com.afollestad.overhear.webartprovider/artists");

	public static void setImageURL(Context context, Album album, String url) {
        WebArt art = WebArt.fromAlbum(album, url);
        ContentValues values = art.getContentValues();
        int updated = context.getContentResolver().update(ALBUMS_URI, values,
                WebArt.getAlbumWhereStatement(album), null);
        if(updated == 0) {
            context.getContentResolver().insert(ALBUMS_URI, values);
        }
    }

    public static String getImageURL(Context context, Album album) {
        Cursor cursor = context.getContentResolver().query(ALBUMS_URI, null,
                WebArt.getAlbumWhereStatement(album), null, WebArt.NAME);
        String toreturn = null;
        if(cursor.moveToFirst()) {
            toreturn = WebArt.fromCursor(cursor).getUrl();
        }
        cursor.close();
        return toreturn;
    }

    public static void setImageURL(Context context, Artist artist, String url) {
        WebArt art = WebArt.fromArtist(artist, url);
        ContentValues values = art.getContentValues();
        int updated = context.getContentResolver().update(ARTISTS_URI, values,
                WebArt.getArtistWhereStatement(artist), null);
        if(updated == 0) {
            context.getContentResolver().insert(ARTISTS_URI, values);
        }
    }

    public static String getImageURL(Context context, Artist artist) {
        Cursor cursor = context.getContentResolver().query(ARTISTS_URI, null,
                WebArt.getArtistWhereStatement(artist), null, WebArt.NAME);
        String toreturn = null;
        if(cursor.moveToFirst()) {
            toreturn = WebArt.fromCursor(cursor).getUrl();
        }
        cursor.close();
        return toreturn;
    }
}