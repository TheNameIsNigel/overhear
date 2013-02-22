package com.afollestad.overhear;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.overhearapi.Song;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

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

    public static AlertDialog createPlaylistChooseDialog(final Activity context, final Song songAdd, final Album albumAdd) {

        ArrayList<CharSequence> items = new ArrayList<CharSequence>();
        final ArrayList<Playlist> playlists = Playlist.getAllPlaylists(context);
        for(Playlist list : playlists) {
            items.add(list.getName());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
            .setTitle(R.string.add_to_playlist)
            .setItems(items.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {

                    Playlist list = playlists.get(which);
                    if(songAdd != null) {
                        list.insertSong(context, songAdd);
                    } else if(albumAdd != null) {
                        ArrayList<Song> albumSongs = Song.getAllFromScope(context, new String[] {
                                MediaStore.Audio.Media.ALBUM + " = '" + albumAdd.getName().replace("'", "''") + "' AND " +
                                        MediaStore.Audio.Media.ARTIST + " = '" + albumAdd.getArtist().getName().replace("'", "''") + "'",
                                MediaStore.Audio.Media.TRACK
                        });
                        list.insertSongs(context, albumSongs);
                    }

                    context.sendBroadcast(new Intent(MusicService.PLAYLIST_UPDATED));

                }
            });
        return builder.create();
    }
}