package com.afollestad.overhear.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearListActivity;
import com.afollestad.overhear.service.MusicService;
import com.afollestad.overhear.service.QueueItem;
import com.afollestad.overhearapi.Album;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Playlist;
import com.afollestad.overhearapi.Song;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

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

    private static Playlist newList;

    public static AlertDialog createPlaylistChooseDialog(final Activity context, final Song songAdd, final Album albumAdd, final Artist artistAdd) {
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
                        if(which == 0) {
                            Dialog createDiag = createNewPlaylistDialog(context, null);
                            createDiag.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialogInterface) {
                                    addToPlaylist(context, songAdd, albumAdd, artistAdd, newList);
                                    newList = null;
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

    private static void addToPlaylist(Activity context, Song songAdd, Album albumAdd, Artist artistAdd, Playlist list) {
        if (songAdd != null) {
            list.insertSong(context, songAdd);
            Toast.makeText(context, context.getString(R.string.added_to_playist).replace("{name}", songAdd.getTitle())
                    .replace("{list}", list.getName()), Toast.LENGTH_SHORT).show();
        } else if (albumAdd != null) {
            ArrayList<Song> albumSongs = Song.getAllFromScope(context, new String[]{
                    MediaStore.Audio.Media.ALBUM + " = '" + albumAdd.getName().replace("'", "''") + "' AND " +
                            MediaStore.Audio.Media.ARTIST + " = '" + albumAdd.getArtist().getName().replace("'", "''") + "'",
                    MediaStore.Audio.Media.TRACK
            });
            list.insertSongs(context, albumSongs);
            Toast.makeText(context, context.getString(R.string.added_to_playist).replace("{name}", albumAdd.getName())
                    .replace("{list}", list.getName()), Toast.LENGTH_SHORT).show();
        } else if(artistAdd != null) {
        	ArrayList<Song> artistSongs = Song.getAllFromScope(context, new String[]{
                        MediaStore.Audio.Media.ARTIST + " = '" + artistAdd.getName().replace("'", "''") + "'",
                    MediaStore.Audio.Media.ALBUM
            });
            list.insertSongs(context, artistSongs);
            Toast.makeText(context, context.getString(R.string.added_to_playist).replace("{name}", artistAdd.getName())
                    .replace("{list}", list.getName()), Toast.LENGTH_SHORT).show();
        }
        context.sendBroadcast(new Intent(MusicService.PLAYLIST_UPDATED));
    }

    public static Dialog createNewPlaylistDialog(final Activity context, final Playlist toRename) {
        newList = null;
        final Dialog diag = new Dialog(context);
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
                    newList = Playlist.create(context, input.getText().toString().trim());
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
    
    public static ArrayList<QueueItem> getQueueItemArray(ArrayList<Song> songs) {
    	ArrayList<QueueItem> toreturn = new ArrayList<QueueItem>();
    	for(Song s : songs) {
    		toreturn.add(new QueueItem(s.getId(), s.getPlaylistId()));
    	}
    	return toreturn;
    }
    
    public static boolean queueContains(ArrayList<QueueItem> queue, Song song) {
    	if(song == null) {
    		return false;
    	}
    	for(QueueItem a : queue) {
    		if(a.getSongId() == song.getId() && a.getPlaylistId() == song.getPlaylistId()) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public static int findInQueue(ArrayList<QueueItem> queue, Song song) {
    	for(int index = 0; index < queue.size(); index++) {
    		QueueItem curItem = queue.get(index);
    		if(curItem.getSongId() == song.getId() && curItem.getPlaylistId() == song.getPlaylistId()) {
    			return index;
    		}
    	}
    	return -1;
    }
    
    public static void addToQueue(Activity context, Song song) {
    	if(context == null) {
    		return;
    	}
    	if(context instanceof OverhearActivity) {
    		if(((OverhearActivity)context).getService() == null)
    			return;
    		((OverhearActivity)context).getService().addToQueue(song);
    	} else {
    		if(((OverhearListActivity)context).getService() == null)
    			return;
    		((OverhearListActivity)context).getService().addToQueue(song);
    	}
    }
    
    public static void addToQueue(Activity context, ArrayList<Song> songs) {
    	if(context == null) {
    		return;
    	}
    	if(context instanceof OverhearActivity) {
    		if(((OverhearActivity)context).getService() == null)
    			return;
    		((OverhearActivity)context).getService().addToQueue(songs);
    	} else {
    		if(((OverhearListActivity)context).getService() == null)
    			return;
    		((OverhearListActivity)context).getService().addToQueue(songs);
    	}
    }
    
    public static Song getFocused(Activity context) {
    	if(context == null) {
    		return null;
    	}
    	if(context instanceof OverhearActivity) {
    		if(((OverhearActivity)context).getService() == null)
    			return null;
    		return ((OverhearActivity)context).getService().getFocused();
    	} else {
    		if(((OverhearListActivity)context).getService() == null)
    			return null;
    		return ((OverhearListActivity)context).getService().getFocused();
    	}
    }
    
    public static boolean isPlaying(Activity context) {
    	if(context == null) {
    		return false;
    	}
    	if(context instanceof OverhearActivity) {
    		if(((OverhearActivity)context).getService() == null)
    			return false;
    		return ((OverhearActivity)context).getService().isPlaying();
    	} else {
    		if(((OverhearListActivity)context).getService() == null)
    			return false;
    		return ((OverhearListActivity)context).getService().isPlaying();
    	}
    }
    
    public static void persistentQueue(Context context, ArrayList<QueueItem> queue, int queuePos) {
    	JSONArray queueArray = new JSONArray();
    	for(QueueItem val : queue) {
    		queueArray.put(val.getJSON());
    	}
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	prefs.edit().putString("queue", queueArray.toString()).putInt("queue_pos", queuePos).commit();
    }
    
    public static ArrayList<QueueItem> getPersistedQueue(Context context) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	try {
			JSONArray array = new JSONArray(prefs.getString("queue", "[]"));
			ArrayList<QueueItem> toreturn = new ArrayList<QueueItem>();
			for(int i = 0; i < array.length(); i++) {
				toreturn.add(QueueItem.fromJSON(array.getJSONObject(i)));
			}
			return toreturn;
		} catch (JSONException e) {
			e.printStackTrace();
		}
    	return null;
    }
    
    public static int getPersistedQueuePos(Context context) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    	return prefs.getInt("queue_pos", -1);
    }
}