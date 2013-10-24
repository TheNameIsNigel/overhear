package com.afollestad.overhear.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.adapters.ArtistAdapter;
import com.afollestad.overhear.ui.ArtistViewer;
import com.afollestad.overhear.ui.LoginHandler;
import com.afollestad.overhear.utils.Twitter;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.overhearapi.LastFM.ArtistInfo;
import com.afollestad.overhearapi.Utils;
import com.afollestad.silk.fragments.SilkFragment;
import org.json.JSONException;
import org.json.JSONObject;
import twitter4j.ResponseList;
import twitter4j.TwitterException;
import twitter4j.User;

import java.util.ArrayList;

/**
 * Loads and displays bio information about an artist (e.g. tweets, last.fm information, etc.).
 *
 * @author Aidan Follestad
 */
public class BioListFragment extends SilkFragment {

    private final static int LOGIN_HANDER_RESULT = 600;
    private Artist artist;
    private ResponseList<User> possibleUsers;
    private User twitterUser;

    public BioListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && getView() != null) {
            ((TextView) getView().findViewById(R.id.bioAbout)).setText(savedInstanceState.getString("lastfm_bio"));
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
        if (artist != null)
            outState.putString("artist", artist.getJSON().toString());
        outState.putString("lastfm_bio", ((TextView) getView().findViewById(R.id.bioAbout)).getText().toString());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected int getLayout() {
        return R.layout.bio_fragment;
    }

    @Override
    public String getTitle() {
        return null;
    }

    private void showAlternateAccountPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.alternate_accounts);
        ArrayList<CharSequence> items = new ArrayList<CharSequence>();
        items.add(getString(R.string.none_str));
        for (User user : possibleUsers) {
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
        twitter4j.Twitter twitter = Twitter.getTwitterInstance(getActivity(), false);
        try {
            return twitter.searchUsers(artist.getName(), 0);
        } catch (TwitterException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadLastFm() {
        if (artist == null) return;
        String currentBio = ((TextView) getView().findViewById(R.id.bioAbout)).getText().toString();
        if (!currentBio.equals(getString(R.string.loading_str))) {
            return;
        }

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
                                    Spanned bio = info.getBioSummary();
                                    if (bio.charAt(0) == '\n') {
                                        bio = (Spanned) bio.subSequence(1, bio.length() - 1);
                                    }
                                    ((TextView) getView().findViewById(R.id.bioAbout)).setText(bio);
                                }

                                LinearLayout similarArtists = (LinearLayout) getView().findViewById(R.id.bioSimilarArtists);
                                similarArtists.removeAllViews();
                                if (info.getSimilarArtists().size() > 0) {
                                    for (final ArtistInfo simArt : info.getSimilarArtists()) {
                                        View v = ArtistAdapter.getViewForArtistInfo(getActivity(), simArt);
                                        v.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View view) {
                                                Artist artist = Artist.getArtist(getActivity(), simArt.getName());
                                                if (artist == null)
                                                    return;
                                                getActivity().startActivity(new Intent(getActivity(), ArtistViewer.class)
                                                        .putExtra("artist", artist.getJSON().toString()));
                                            }
                                        });
                                        similarArtists.addView(v);
                                    }
                                }
                                similarArtists.requestLayout();
                                similarArtists.invalidate();
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
        if (artist == null) return;
        final Handler mHandler = new Handler();
        new Thread(new Runnable() {
            @SuppressWarnings("unused")
            public void run() {
                try {
                    twitter4j.Twitter twitter = Twitter.getTwitterInstance(getActivity(), true);
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

                    final long altTwit = Twitter.getSocialAccount(getActivity(), artist);
                    if (altTwit > 0) {
                        twitterUser = twitter.showUser(altTwit);
                    } else if (altTwit == -1) {
                        possibleUsers = getPossibleUsers();
                        if (possibleUsers.size() > 0) {
                            for (User possibleUser : possibleUsers) {
                                if (possibleUser.isVerified()) {
                                    twitterUser = possibleUser;
                                    Twitter.setSocialAccount(getActivity(), artist, twitterUser.getId());
                                    break;
                                }
                            }
                        }
                    } else twitterUser = null;
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
                                if (altTwit == 0) {
                                    updates.setText(R.string.social_profile_none);
                                } else updates.setText(R.string.no_social_profile);
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
        if (requestCode == LOGIN_HANDER_RESULT && resultCode == Activity.RESULT_OK)
            update();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.socialUpdates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (twitterUser == null) return;
                Uri uri = Uri.parse("https://twitter.com/" + twitterUser.getScreenName() +
                        "/status/" + twitterUser.getStatus().getId());
                startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
            }
        });
        view.findViewById(R.id.socialUpdates).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                twitter4j.Twitter twitter = Twitter.getTwitterInstance(getActivity(), true);
                if (twitter == null) return false;
                else if (possibleUsers == null) {
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
                } else showAlternateAccountPopup();
                return true;
            }
        });
        loadLastFm();
        loadTwitter();
    }

    void update() {
        if (getActivity() != null && getView() != null) {
            loadTwitter();
        } else twitterUser = null;
    }
}
