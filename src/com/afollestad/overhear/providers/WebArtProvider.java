package com.afollestad.overhear.providers;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import com.afollestad.overhearapi.WebArt;

public class WebArtProvider extends ContentProvider {

    private SQLiteOpenHelper mOpenHelper;
    private static final String DBNAME = "overhear";
    private static final String ALBUM_ART = "album_art";
    private static final String ARTIST_ART = "artist_art";
    private SQLiteDatabase db;

    private static UriMatcher sUriMatcher;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI("com.afollestad.overhear.webartprovider", "albums", 1);
        sUriMatcher.addURI("com.afollestad.overhear.webartprovider", "artists", 2);
    }

    @Override
	public boolean onCreate() {
    	mOpenHelper = new SQLiteOpenHelper(getContext(), DBNAME, null, 1) {
			@Override
			public void onCreate(SQLiteDatabase db) { }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				if(newVersion <= oldVersion) {
					return;
				}
				Log.i("OVERHEAR WebArtProvider", "Upgrading database from version " + oldVersion +
						" to " + newVersion + ", which will destroy all old data");
				db.execSQL("DROP TABLE IF EXISTS " + ALBUM_ART);
                db.execSQL("DROP TABLE IF EXISTS " + ARTIST_ART);
				onCreate(db);
			}
    	};

    	db = mOpenHelper.getWritableDatabase();
    	db.execSQL(WebArt.getCreateTableStatement(ALBUM_ART));
        db.execSQL(WebArt.getCreateTableStatement(ARTIST_ART));
    	return true;
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

    private String chooseTable(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case 1:
                return ALBUM_ART;
            case 2:
                return ARTIST_ART;
            default:
                throw new IllegalArgumentException("Invalid web art provider table found in the URI " + uri.toString() + ". Expected 'artists' or 'albums'.");
        }
    }

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return db.query(chooseTable(uri), projection, selection, selectionArgs, null, null, sortOrder);
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return db.update(chooseTable(uri), values, selection, selectionArgs);
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		db.insert(chooseTable(uri), null, values);
		return null;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return db.delete(chooseTable(uri), selection, selectionArgs);
	}
}
