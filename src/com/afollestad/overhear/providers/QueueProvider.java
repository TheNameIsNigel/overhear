package com.afollestad.overhear.providers;

import com.afollestad.overhearapi.Album;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class QueueProvider extends ContentProvider {

    private SQLiteOpenHelper mOpenHelper;
    private static final String DBNAME = "overhear";
    private static final String TABLE_QUEUE = "queue";
    private SQLiteDatabase db;
    
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
				Log.i("OVERHEAR RecentsProvider", "Upgrading database from version " + oldVersion + 
						" to " + newVersion + ", which will destroy all old data");
				db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUEUE);
				onCreate(db);
			}
    	};
    	db = mOpenHelper.getWritableDatabase();
    	db.execSQL(Album.getCreateTableStatement(TABLE_QUEUE));
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
		return db.query(TABLE_QUEUE, projection, selection, selectionArgs, null, null, sortOrder);
	}
	
	/**
	 * The URI is ignored, for now.
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		return db.update(TABLE_QUEUE, values, selection, selectionArgs);
	}
	
	/**
	 * The URI is ignored, for now.
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		db.insert(TABLE_QUEUE, null, values);
		return null;
	}
	
	/**
	 * The URI is ignored, for now.
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		return db.delete(TABLE_QUEUE, selection, selectionArgs);
	}
}
