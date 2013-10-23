package com.afollestad.overhear.queue;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import com.afollestad.overhearapi.Song;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Represents an item contained in a {@link Queue}
 *
 * @author Aidan Follestad
 */
public class QueueItem {

    public QueueItem(int id, long playlist, int scope) {
        this.songId = id;
        this.playlistId = playlist;
        this.scope = scope;
    }

    private int playlistRow = -1;
    private final int songId;
    private final long playlistId;
    private final int scope;
    private String album;
    private String artist;
    private String title;
    private long duration;
    private String data;


    public static String[] getProjection(String idCol) {
        if (idCol.equals(MediaStore.Audio.Playlists.Members.AUDIO_ID)) {
            return new String[]{"_id", idCol, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA};
        } else {
            return new String[]{idCol, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.DATA};
        }
    }

    private void loadMeta(Context context) {
        if (album != null && artist != null && title != null)
            return;
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                getProjection("_id"), "_id = " + getSongId(), null, null);

        cursor.moveToFirst();
        if (playlistId > -1)
            playlistRow = cursor.getInt(cursor.getColumnIndex("_id"));
        album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
        artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
        title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
        duration = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
        data = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
        cursor.close();
    }

    public String getAlbum(Context context) {
        loadMeta(context);
        return album;
    }

    public String getArtist(Context context) {
        loadMeta(context);
        return artist;
    }

    public String getTitle(Context context) {
        loadMeta(context);
        return title;
    }

    public long getDuration(Context context) {
        loadMeta(context);
        return duration;
    }

    public String getDurationString(Context context) {
        return Song.getDurationString(getDuration(context));
    }

    public String getData(Context context) {
        loadMeta(context);
        return data;
    }

    public int getPlaylistRow() {
        return playlistRow;
    }

    QueueItem setAlbum(String album) {
        this.album = album;
        return this;
    }

    QueueItem setArtist(String artist) {
        this.artist = artist;
        return this;
    }

    QueueItem setTitle(String title) {
        this.title = title;
        return this;
    }

    QueueItem setDuration(long duration) {
        this.duration = duration;
        return this;
    }

    QueueItem setData(String data) {
        this.data = data;
        return this;
    }

    QueueItem setPlaylistRow(int playlistRow) {
        this.playlistRow = playlistRow;
        return this;
    }


    /**
     * The song was loaded outside of any scope.
     */
    public final static int SCOPE_SINGULAR = 0;
    /**
     * The song was loaded from the entire library of songs (e.g. the "Songs" tab in the OverviewScreen).
     */
    public final static int SCOPE_All_SONGS = 1;
    /**
     * The song was loaded from an album (e.g. in the album viewer).
     */
    public final static int SCOPE_ALBUM = 2;
    /**
     * The song was loaded from a playlist (e.g. in the playlist viewer).
     */
    public final static int SCOPE_PLAYLIST = 3;
    /**
     * The song was loaded from a genre (e.g. in the genre viewer).
     */
    public final static int SCOPE_GENRE = 4;
    /**
     * The song was loaded from an artist (e.g. in the artist viewer).
     */
    public final static int SCOPE_ARTIST = 5;

    /**
     * Gets the ID of the song, this value is different when loaded from a playlist.
     */
    public int getSongId() {
        return songId;
    }

    /**
     * Gets the ID of the playlist that the song was loaded from.
     */
    public long getPlaylistId() {
        return playlistId;
    }

    /**
     * Gets the scope, which indicates where the song was loaded from.
     */
    public int getScope() {
        return scope;
    }

//	public Song getSong(Context context) {
//		Song song = Song.fromId(context, getSongId());
//		song.setPlaylistId(getPlaylistId());
//		return song;
//	}

    public JSONObject getJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("song_id", getSongId());
            json.put("playlist_id", getPlaylistId());
            json.put("scope", getScope());
            json.put("artist", artist);
            json.put("album", album);
            json.put("title", title);
            json.put("duration", duration);
            json.put("data", data);
            json.put("playlist_row", playlistRow);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static QueueItem fromJSON(JSONObject json) {
        try {
            return new QueueItem(
                    json.optInt("song_id", -1),
                    json.optLong("playlist_id", -1),
                    json.optInt("scope", 0))
                    .setAlbum(json.optString("album"))
                    .setArtist(json.optString("artist"))
                    .setTitle(json.optString("title"))
                    .setDuration(json.optLong("duration"))
                    .setData(json.optString("data"))
                    .setPlaylistRow(json.optInt("playlist_row"));
        } catch (Exception e) {
            return null;
        }
    }

    public static QueueItem fromJSON(String json) {
        try {
            return fromJSON(new JSONObject(json));
        } catch (Exception e) {
            return null;
        }
    }

    public static ArrayList<QueueItem> getAll(Context context, String where, String sort, long playlist, int scope) {
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{"_id"}, where, null, sort);
        ArrayList<QueueItem> items = new ArrayList<QueueItem>();
        while (cursor.moveToNext()) {
            items.add(fromCursor(cursor, playlist, scope));
        }
        cursor.close();
        return items;
    }

    public static QueueItem fromCursor(Cursor cursor, long playlist, int scope) {
        String idCol = "_id";
        if (playlist > -1)
            idCol = MediaStore.Audio.Playlists.Members.AUDIO_ID;
        return new QueueItem(
                cursor.getInt(cursor.getColumnIndex(idCol)),
                playlist,
                scope
        );
    }

    public static ArrayList<QueueItem> getAllFromIds(ArrayList<Integer> ids, long playlist, int scope) {
        ArrayList<QueueItem> items = new ArrayList<QueueItem>();
        for (int i : ids) {
            items.add(new QueueItem(i, playlist, scope));
        }
        return items;
    }
}
