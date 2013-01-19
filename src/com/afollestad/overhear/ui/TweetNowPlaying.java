package com.afollestad.overhear.ui;

import twitter4j.ResponseList;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import com.afollestad.overhear.QueueUtils;
import com.afollestad.overhear.R;
import com.afollestad.overhearapi.Song;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class TweetNowPlaying extends Activity {

	private Twitter twitter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.dialog_tweet_now_playing); 
		twitter = LoginHandler.getTwitterInstance(getApplicationContext(), true);
		loadInitialText();
		findViewById(R.id.tweetBtn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				send();
			}
		});
	}

	private void send() {
		final TextView text = (TextView)findViewById(R.id.tweetText);
		final Button send = (Button)findViewById(R.id.tweetBtn);
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
		final TextView text = (TextView)findViewById(R.id.tweetText);
		final Button send = (Button)findViewById(R.id.tweetBtn);
		final Song last = QueueUtils.poll(this);
		
		text.setText(R.string.loading_str);
		text.setEnabled(false);
		send.setEnabled(false);
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				String displayArtist = last.getArtist();
				try {
					ResponseList<User> possibleUsers = twitter.searchUsers(last.getArtist(), 0);
					if(possibleUsers.size() > 0) {
						for(int i = 0; i < possibleUsers.size(); i++) {
							if(possibleUsers.get(i).isVerified()) {
								displayArtist = "@" + possibleUsers.get(i).getScreenName();
								break;
							}
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
				final String fArtist = displayArtist;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						text.setText("");
						text.append("#NowPlaying - " + last.getTitle() + " by " + fArtist);
						text.setEnabled(true);
						send.setEnabled(true);
					}
				});
			}
		}).start();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.dialog_tweet_now_playing, menu);
		return true;
	}
}
