package com.afollestad.overhear;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
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
        ConnectivityManager cm = (ConnectivityManager) context
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
        final ArrayList<Playlist> playlists = Playlist.getAllPlaylists(context);
        if (playlists.size() == 0) {
            Toast.makeText(context, R.string.no_playlists, Toast.LENGTH_SHORT).show();
            return null;
        }
        ArrayList<CharSequence> items = new ArrayList<CharSequence>();
        for (Playlist list : playlists) {
            items.add(list.getName());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.add_to_playlist)
                .setItems(items.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        Playlist list = playlists.get(which);
                        if (songAdd != null) {
                            list.insertSong(context, songAdd);
                        } else if (albumAdd != null) {
                            ArrayList<Song> albumSongs = Song.getAllFromScope(context, new String[]{
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

    public static Dialog createNewPlaylistDialog(final Activity context, final Playlist toRename) {
        final Dialog diag = new Dialog(context, R.style.DarkTheme_DialogCustom);
        diag.setContentView(R.layout.input_dialog);
        diag.setCancelable(true);
        if (toRename != null) {
            diag.setTitle(R.string.rename);
        } else {
            diag.setTitle(R.string.create_playlist);
        }
        final EditText input = (EditText) diag.findViewById(R.id.input);
        input.setHint(R.string.new_playlist_name_hint);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                diag.findViewById(R.id.ok).setEnabled(!input.getText().toString().trim().isEmpty());
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        diag.findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                diag.dismiss();
                if (toRename != null) {
                    toRename.rename(context, input.getText().toString().trim());
                } else {
                    Playlist.create(context, input.getText().toString().trim());
                }
                context.sendBroadcast(new Intent(MusicService.PLAYLIST_UPDATED));
            }
        });
        diag.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                diag.dismiss();
            }
        });
        return diag;
    }

    public static AlertDialog createPlaylistDeleteDialog(final Activity context, final Playlist list) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.delete)
                .setMessage(context.getString(R.string.playlist_delete_confirm).replace("{name}", list.getName()))
                .setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        list.delete(context);
                        context.sendBroadcast(new Intent(MusicService.PLAYLIST_UPDATED));
                    }
                })
                .setNegativeButton(R.string.no_str, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
        return builder.create();
    }
}