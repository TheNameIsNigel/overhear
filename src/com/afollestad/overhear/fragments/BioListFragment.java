package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.Twitter;
import com.afollestad.overhear.ui.ArtistViewer;
import com.afollestad.overhear.ui.LoginHandler;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.overhearapi.LastFM.ArtistInfo;
import com.afollestad.overhearapi.Utils;
import org.json.JSONException;
import org.json.JSONObject;
import twitter4j.ResponseList;
import twitter4j.TwitterException;
import twitter4j.User;

import java.util.ArrayList;

public class BioListFragment extends Fragment {

    public BioListFragment() {
    }

    private Artist artist;
    private ResponseList<User> possibleUsers;
    private User twitterUser;
    public final static int LOGIN_HANDER_RESULT = 600;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            try {
                artist = Artist.fromJSON(new JSONObject(savedInstanceState.getString("artist")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            artist = ((ArtistViewer) getActivity()).artist;
        }
        setRetainInstance(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("artist", artist.getJSON().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.bio_fragment, null);
        view.findViewById(R.id.socialUpdates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (twitterUser == null)
                    return;
                Uri uri = Uri.parse("https://twitter.com/" + twitterUser.getScreenName() +
                        "/status/" + twitterUser.getStatus().getId());
                startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
            }
        });
        view.findViewById(R.id.socialUpdates).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (possibleUsers == null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            possibleUsers = getPossibleUsers();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showAlternateAccountPopup();
                                }
                            });
                        }
                    }).start();
                } else {
                    showAlternateAccountPopup();
                }
                return true;
            }
        });
        return view;
    }

    private void showAlternateAccountPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.alternate_accounts);
        ArrayList<CharSequence> items = new ArrayList<CharSequence>();
        items.add(getString(R.string.none_str));
        for (int i = 0; i < possibleUsers.size(); i++) {
            User user = possibleUsers.get(i);
            items.add("@" + user.getScreenName());
        }
        builder.setItems(items.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                if (which == 0) {
                    twitterUser = null;
                    Twitter.setSocialAccount(getActivity(), artist, 0);
                } else {
                    twitterUser = possibleUsers.get(which - 1);
                    Twitter.setSocialAccount(getActivity(), artist, twitterUser.getId());
                }
                loadTwitter();
            }
        });
        builder.create().show();
    }

    private ResponseList<User> getPossibleUsers() {
        twitter4j.Twitter twitter = com.afollestad.overhear.Twitter.getTwitterInstance(getActivity(), false);
        try {
            return twitter.searchUsers(artist.getName(), 0);
        } catch (TwitterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadLastFm() {
        final Handler mHandler = new Handler();
        new Thread(new Runnable() {
            public void run() {
                try {
                    final ArtistInfo info = LastFM.getArtistInfo(artist.getName());
                    mHandler.post(new Runnable() {
                        public void run() {
                            if (getView() != null) {
                                if (info.getBioContent() == null) {
                                    ((TextView) getView().findViewById(R.id.bioAbout)).setText(R.string.no_bio_str);
                                } else {
                                    Spanned bio = info.getBioContent();
                                    if(bio.charAt(0) == '\n') {
                                        bio = (Spanned)bio.subSequence(1, bio.length() - 1);
                                    }
                                    ((TextView) getView().findViewById(R.id.bioAbout)).setText(bio);
                                }
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    mHandler.post(new Runnable() {
                        public void run() {
                            if (getView() != null)
                                ((TextView) getView().findViewById(R.id.bioAbout)).setText(R.string.failed_load_artist_bio);
                        }
                    });
                }
            }
        }).start();
    }

    private void loadTwitter() {
        final Handler mHandler = new Handler();
        new Thread(new Runnable() {
            public void run() {
                try {
                    twitter4j.Twitter twitter = com.afollestad.overhear.Twitter.getTwitterInstance(getActivity(), true);
                    if (twitter == null) {
                        mHandler.post(new Runnable() {
                            public void run() {
                                Button action = (Button) getView().findViewById(R.id.bioUpdatesAction);
                                action.setEnabled(true);
                                action.setVisibility(View.VISIBLE);
                                action.setText(R.string.login_twitter);
                                action.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View arg0) {
                                        startActivityForResult(
                                                new Intent(getActivity(), LoginHandler.class),
                                                LOGIN_HANDER_RESULT);
                                    }
                                });
                                getView().findViewById(R.id.bioUpdateSource).setVisibility(View.GONE);
                                getView().findViewById(R.id.bioUpdates).findViewById(View.GONE);
                            }
                        });
                        return;
                    }

                    final long altTwit = com.afollestad.overhear.Twitter.getSocialAccount(getActivity(), artist);
                    if (altTwit > 0) {
                        twitterUser = twitter.showUser(altTwit);
                    } else if (altTwit == -1) {
                        possibleUsers = getPossibleUsers();
                        if (possibleUsers.size() > 0) {
                            for (int i = 0; i < possibleUsers.size(); i++) {
                                if (possibleUsers.get(i).isVerified()) {
                                    twitterUser = possibleUsers.get(i);
                                    Twitter.setSocialAccount(getActivity(), artist, twitterUser.getId());
                                }
                                break;
                            }
                        }
                    } else {
                        twitterUser = null;
                    }

                    if (twitterUser != null) {
                        mHandler.post(new Runnable() {
                            public void run() {
                                if (getView() != null) {
                                    getView().findViewById(R.id.bioUpdatesAction).setVisibility(View.GONE);
                                    TextView updates = (TextView) getView().findViewById(R.id.bioUpdates);
                                    updates.setText(twitterUser.getStatus().getText());
                                    updates.setVisibility(View.VISIBLE);
                                    String source = getString(R.string.social_update_source)
                                            .replace("{time}", Utils.getFriendlyTime(twitterUser.getStatus().getCreatedAt()))
                                            .replace("{user}", "@" + twitterUser.getScreenName())
                                            .replace("{network}", "Twitter");
                                    TextView sourceTxt = (TextView) getView().findViewById(R.id.bioUpdateSource);
                                    sourceTxt.setText(source);
                                    sourceTxt.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    } else {
                        mHandler.post(new Runnable() {
                            public void run() {
                                TextView updates = (TextView) getView().findViewById(R.id.bioUpdates);
                                if(altTwit == 0) {
                                    updates.setText(R.string.social_profile_none);
                                } else {
                                    updates.setText(R.string.no_social_profile);
                                }
                                updates.setVisibility(View.VISIBLE);
                                getView().findViewById(R.id.bioUpdateSource).setVisibility(View.GONE);
                                getView().findViewById(R.id.bioUpdatesAction).setVisibility(View.GONE);
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    if (getView() != null) {
                        mHandler.post(new Runnable() {
                            public void run() {
                                ((Button) getView().findViewById(R.id.bioUpdatesAction)).setText(R.string.error_str);
                            }
                        });
                    }
                }
            }
        }).start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == LOGIN_HANDER_RESULT && resultCode == Activity.RESULT_OK) {
            update();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadLastFm();
        loadTwitter();
    }

    public void update() {
        if (getActivity() != null && getView() != null) {
            loadTwitter();
        } else {
            twitterUser = null;
        }
    }
}
