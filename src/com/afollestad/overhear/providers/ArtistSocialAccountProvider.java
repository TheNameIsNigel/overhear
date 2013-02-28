package com.afollestad.overhear.providers;

import com.afollestad.overhear.base.Overhear;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/**
 * The provider used to store the IDs of Twitter accounts that are associated with artists. When
 * you select a different account for an artist in the artist viewer, it's saved in this provider. 
 * 
 * @author Aidan Follestad
 */
public class ArtistSocialAccountProvider extends ContentProvider {

    private SQLiteOpenHelper mOpenHelper;
    private static final String DBNAME = "overhear";
    private static final String TABLE_NAME = "artist_social_accounts";
    private SQLiteDatabase db;

    @Override
	public boolean onCreate() {
    	mOpenHelper = new SQLiteOpenHelper(getContext(), DBNAME, null, Overhear.DATABASE_VERSION) {
			@Override
			public void onCreate(SQLiteDatabase db) { }
			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				if(newVersion <= oldVersion) {
					return;
				}
				Log.i("OVERHEAR ArtistSocialAccountProvider", "Upgrading database from version " + oldVersion +
						" to " + newVersion + ", which will destroy all old data");
                db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
				onCreate(db);
			}
    	};

    	db = mOpenHelper.getWritableDatabase();
    	db.execSQL(getCreateTableStatement());
    	return true;
	}

    public static String getCreateTableStatement() {
        return "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(" +
                "artist_name TEXT PRIMARY KEY," +
                "twitter_id INTEGER" +
                ");";
    }

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return db.query(TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);
	}
	
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return db.update(TABLE_NAME, values, selection, selectionArgs);
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		db.insert(TABLE_NAME, null, values);
		return null;
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return db.delete(TABLE_NAME, selection, selectionArgs);
	}
}
