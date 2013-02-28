package com.afollestad.overhear.providers;

import com.afollestad.overhear.base.Overhear;
import com.afollestad.overhearapi.Album;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

/**
 * The provider used to store a list of recently played music.
 * 
 * @author Aidan Follestad
 */
public class RecentsProvider extends ContentProvider {

    private SQLiteOpenHelper mOpenHelper;
    private static final String DBNAME = "overhear";
    private static final String TABLE_RECENTS_OLD = "recents";
    private static final String TABLE_RECENTS = "recently_played";
    private static final String TABLE_QUEUE = "queue";
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
				Log.i("OVERHEAR RecentsProvider", "Upgrading database from version " + oldVersion + 
						" to " + newVersion + ", which will destroy all old data");
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECENTS);
				onCreate(db);
			}
    	};
    	db = mOpenHelper.getWritableDatabase();
    	//TODO remove when cleanup of old queue style is no longer needed
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUEUE);
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECENTS_OLD);
    	db.execSQL(Album.getCreateTableStatement(TABLE_RECENTS));
    	return true;
	}

    /**
     * This returns null, for now.
     */
	@Override
	public String getType(Uri uri) {
		return null;
	}
	
	/**
	 * The URI is ignored, for now.
	 */
	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return db.query(TABLE_RECENTS, projection, selection, selectionArgs, null, null, sortOrder);
	}
	
	/**
	 * The URI is ignored, for now.
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return db.update(TABLE_RECENTS, values, selection, selectionArgs);
	}
	
	/**
	 * The URI is ignored, for now.
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		db.insert(TABLE_RECENTS, null, values);
		return null;
	}
	
	/**
	 * The URI is ignored, for now.
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return db.delete(TABLE_RECENTS, selection, selectionArgs);
	}
}
