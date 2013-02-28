package com.afollestad.overhear.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.utils.Twitter;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.Song;
import twitter4j.ResponseList;
import twitter4j.StatusUpdate;
import twitter4j.TwitterException;
import twitter4j.User;

/**
 * A dialog used to tweet the currently playing song.
 * 
 * @author Aidan Follestad
 */
public class TweetNowPlaying extends OverhearActivity {

    private twitter4j.Twitter twitter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_tweet_now_playing);
        twitter = com.afollestad.overhear.utils.Twitter.getTwitterInstance(getApplicationContext(), true);
        loadInitialText();
        findViewById(R.id.tweetBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                send();
            }
        });
        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void send() {
        final TextView text = (TextView) findViewById(R.id.tweetText);
        final Button send = (Button) findViewById(R.id.tweetBtn);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    twitter.updateStatus(new StatusUpdate(text.getText().toString().trim()));
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), R.string.tweeted_str, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                } catch (final TwitterException e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            send.setEnabled(true);
                            Toast.makeText(getApplicationContext(), getString(R.string.failed_tweet_str)
                                    .replace("{error}", e.getMessage()), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    private void loadInitialText() {
        final TextView text = (TextView) findViewById(R.id.tweetText);
        final Button send = (Button) findViewById(R.id.tweetBtn);
        final Song last = getService().getQueue().getFocused();
        if (last == null) {
            finish();
            return;
        }

        text.setText(R.string.loading_str);
        text.setEnabled(false);
        send.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {

                String displayArtist = last.getArtist();
                final long altTwit = Twitter.getSocialAccount(getApplicationContext(), new Artist(displayArtist, null));
                if (altTwit > 0) {
                    try {
                        User user = twitter.showUser(altTwit);
                        displayArtist = "@" + user.getScreenName();
                    } catch (TwitterException e) {
                        e.printStackTrace();
                    }
                } else if (altTwit == -1) {
                    try {
                        ResponseList<User> twitterMatches = twitter.searchUsers(last.getArtist(), 0);
                        if (twitterMatches.size() > 0) {
                            for (int i = 0; i < twitterMatches.size(); i++) {
                                if (twitterMatches.get(i).isVerified()) {
                                    displayArtist = "@" + twitterMatches.get(i).getScreenName();
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                final String fArtist = displayArtist;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        text.setText("");
                        text.append(getString(R.string.now_playing_tweet_content)
                                .replace("{title}", last.getTitle())
                                .replace("{artist}", fArtist));
                        text.setEnabled(true);
                        send.setEnabled(true);
                    }
                });

            }
        }).start();
    }

	@Override
	public void onBound() {		
	}
}