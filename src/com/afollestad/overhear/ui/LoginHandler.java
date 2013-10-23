package com.afollestad.overhear.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import com.afollestad.overhear.R;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;

/**
 * Handles Twitter authentication.
 *
 * @author Aidan Follestad
 */
@SuppressLint("SetJavaScriptEnabled")
public class LoginHandler extends Activity {

    private Twitter twitter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        setContentView(R.layout.login_handler);

        final WebView view = (WebView) findViewById(R.id.webView);
        view.getSettings().setJavaScriptEnabled(true);
        view.getSettings().setAppCacheEnabled(false);
        view.getSettings().setSaveFormData(false);
        view.getSettings().setSavePassword(false);
        view.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, final String url) {
                if (url.startsWith("overhear://")) {
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
                                        setResult(RESULT_OK);
                                        finish();
                                    }
                                });
                            } catch (final TwitterException e) {
                                e.printStackTrace();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
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
                Toast.makeText(getApplicationContext(), description, Toast.LENGTH_LONG).show();
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

        twitter = com.afollestad.overhear.utils.Twitter.getTwitterInstance(this, false);
        new Thread(new Runnable() {
            public void run() {
                try {
                    final RequestToken requestToken = twitter.getOAuthRequestToken("overhear://callback");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            view.loadUrl(requestToken.getAuthorizationURL() + "&force_login=true");
                        }
                    });
                } catch (TwitterException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }
}