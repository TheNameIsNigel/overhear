package com.afollestad.overhear;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

@SuppressLint("SetJavaScriptEnabled")
public class LoginHandler extends Activity {

	private Twitter twitter;
	public static Twitter getTwitterInstance(Context context, boolean nullIfNotAuthenticated) {
		Twitter client = TwitterFactory.getSingleton();
		SharedPreferences prefs = context.getSharedPreferences("twitter_account", 0);
		String token = prefs.getString("token", null);
		String secret = prefs.getString("secret", null);
		if(token == null || secret == null) {
			if(nullIfNotAuthenticated)
				return null;
			else
				client.setOAuthAccessToken(null);
		} else {
			client.setOAuthAccessToken(new AccessToken(token, secret));
		}

		return client;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login_handler);

		final WebView view = (WebView)findViewById(R.id.webView);
		view.getSettings().setJavaScriptEnabled(true);
		view.getSettings().setAppCacheEnabled(false);
		view.setWebViewClient(new WebViewClient() {
			
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, final String url) {
				if(url.startsWith("overhear://")) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								final AccessToken access = twitter.getOAuthAccessToken(Uri.parse(url).getQueryParameter("oauth_verifier"));
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										getSharedPreferences("twitter_account", 0).edit()
											.putString("token", access.getToken())
											.putString("secret", access.getTokenSecret())
											.commit();
										finish();
									}
								});
							} catch (final TwitterException e) {
								e.printStackTrace();
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Crouton.makeText(LoginHandler.this, e.getMessage(), Style.ALERT);
									}
								});
							}
						}

					}).start();
					return true;
				} else return false;
			}

			@Override
			public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
				Crouton.makeText(LoginHandler.this, description, Style.ALERT);
			}

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favIcon) {
				view.setVisibility(View.GONE);
				findViewById(R.id.webProgress).setVisibility(View.VISIBLE);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				view.setVisibility(View.VISIBLE);
				findViewById(R.id.webProgress).setVisibility(View.GONE);
			}
		});

		twitter = getTwitterInstance(this, false);
		new Thread(new Runnable() {
			public void run() {
				try {
					final RequestToken requestToken = twitter.getOAuthRequestToken("overhear://callback");
					runOnUiThread(new Runnable() {
						public void run() {
							view.loadUrl(requestToken.getAuthorizationURL());
						}
					});
				} catch (TwitterException e) {
					e.printStackTrace();
					Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
					return;
				}
			}
		}).start();
	}
}