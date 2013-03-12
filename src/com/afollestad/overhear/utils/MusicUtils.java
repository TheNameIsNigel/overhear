package com.afollestad.overhear.utils;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.queue.QueueItem;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.views.AboutDialog;
import com.afollestad.overhear.views.PlaylistCreationDialog;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.overhearapi.Song;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * Convenience methods for various important functions in the app.
 *
 * @author Aidan Follestad
 */
public class MusicUtils {

    public static boolean isOnline(Context context) {
        if (context == null) {
            return false;
        }
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

    public static AlertDialog createPlaylistChooseDialog(final Activity context, final QueueItem songAdd, final Album albumAdd, final Artist artistAdd) {
        final ArrayList<Playlist> playlists = Playlist.getAllPlaylists(context);
        ArrayList<CharSequence> items = new ArrayList<CharSequence>();
        items.add(context.getString(R.string.create_playlist_ellipsis));
        for (Playlist list : playlists) {
            items.add(list.getName());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.add_to_playlist)
                .setItems(items.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (which == 0) {
                            final PlaylistCreationDialog createDiag = createNewPlaylistDialog(context, null);
                            createDiag.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    addToPlaylist(context, songAdd, albumAdd, artistAdd, createDiag.getCreatedPlaylist());
                                }
                            });
                            createDiag.show();
                        } else {
                            Playlist list = playlists.get(which - 1);
                            addToPlaylist(context, songAdd, albumAdd, artistAdd, list);
                        }
                    }
                });
        return builder.create();
    }

    private static void addToPlaylist(Activity context, QueueItem songAdd, Album albumAdd, Artist artistAdd, Playlist list) {
        if (songAdd != null) {
            list.insertSong(context, songAdd.getSongId());
            Toast.makeText(context, context.getString(R.string.added_to_playist).replace("{name}", songAdd.getTitle(context))
                    .replace("{list}", list.getName()), Toast.LENGTH_SHORT).show();
        } else if (albumAdd != null) {
            ArrayList<Integer> albumSongs = Song.getAllFromScope(context, new String[]{
                    MediaStore.Audio.Media.ALBUM + " = '" + albumAdd.getName().replace("'", "''") + "' AND " +
                            MediaStore.Audio.Media.ARTIST + " = '" + albumAdd.getArtist().getName().replace("'", "''") + "'",
                    MediaStore.Audio.Media.TRACK
            });
            list.insertSongs(context, albumSongs);
            Toast.makeText(context, context.getString(R.string.added_to_playist).replace("{name}", albumAdd.getName())
                    .replace("{list}", list.getName()), Toast.LENGTH_SHORT).show();
        } else if (artistAdd != null) {
            ArrayList<Integer> artistSongs = Song.getAllFromScope(context, new String[]{
                    MediaStore.Audio.Media.ARTIST + " = '" + artistAdd.getName().replace("'", "''") + "'",
                    MediaStore.Audio.Media.ALBUM
            });
            list.insertSongs(context, artistSongs);
            Toast.makeText(context, context.getString(R.string.added_to_playist).replace("{name}", artistAdd.getName())
                    .replace("{list}", list.getName()), Toast.LENGTH_SHORT).show();
        }
        context.sendBroadcast(new Intent(MusicService.PLAYLIST_UPDATED));
    }

    public static PlaylistCreationDialog createNewPlaylistDialog(final Activity context, final Playlist toRename) {
        return new PlaylistCreationDialog(context).setRenamePlaylist(toRename);
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

    public static AlertDialog createPlaylistClearDialog(final Activity context, final Playlist list) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.clear_str)
                .setMessage(context.getString(R.string.playlist_clear_confirm).replace("{name}", list.getName()))
                .setPositiveButton(R.string.yes_str, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                        list.clear(context);
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

    public static void addToQueue(Activity context, QueueItem item) {
        if (context == null) {
            return;
        }
        if (context instanceof OverhearActivity) {
            if (((OverhearActivity) context).getService() == null)
                return;
            ((OverhearActivity) context).getService().getQueue().add(item);
        } else {
            if (((OverhearListActivity) context).getService() == null)
                return;
            ((OverhearListActivity) context).getService().getQueue().add(item);
        }
    }

    public static void addToQueue(Activity context, ArrayList<QueueItem> items) {
        if (context == null) {
            return;
        }
        if (context instanceof OverhearActivity) {
            if (((OverhearActivity) context).getService() == null)
                return;
            ((OverhearActivity) context).getService().getQueue().add(items);
        } else {
            if (((OverhearListActivity) context).getService() == null)
                return;
            ((OverhearListActivity) context).getService().getQueue().add(items);
        }
    }

    public static boolean isFavorited(Context context, QueueItem song) {
        if (song == null)
            return false;
        Playlist favorites = Playlist.get(context, context.getString(R.string.favorites_str));
        if (favorites == null) {
            return false;
        }
        return favorites.contains(context, song.getSongId());
    }

    public static boolean toggleFavorited(Context context, QueueItem songItem) {
        if (songItem == null)
            return false;
        Playlist favorites = createFavoritesIfNotExists(context);
        if (!favorites.removeSongById(context, songItem.getSongId())) {
            favorites.insertSong(context, songItem.getSongId());
            return true;
        }
        return false;
    }

    public static Playlist createFavoritesIfNotExists(Context context) {
        Playlist favorites = Playlist.get(context, context.getString(R.string.favorites_str));
        if (favorites == null) {
            favorites = Playlist.create(context, context.getString(R.string.favorites_str));
        }
        return favorites;
    }

    @SuppressLint("CommitTransaction")
    public static void showAboutDialog(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog_about");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        new AboutDialog().show(ft, "dialog_about");
    }

    public static boolean isInstalled(Context context, String pname) {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(pname, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void startApp(Context context, String pname, String fullPath) {
        Intent intent = new Intent("android.intent.category.LAUNCHER");
        intent.setClassName(pname, fullPath);
        context.startActivity(intent);
    }
}