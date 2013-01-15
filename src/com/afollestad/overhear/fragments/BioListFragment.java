package com.afollestad.overhear.fragments;

import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.User;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.overhear.R;
import com.afollestad.overhear.ui.ArtistViewer;
import com.afollestad.overhear.ui.LoginHandler;
import com.afollestad.overhearapi.Artist;
import com.afollestad.overhearapi.LastFM;
import com.afollestad.overhearapi.Utils;
import com.afollestad.overhearapi.LastFM.ArtistInfo;

public class BioListFragment extends Fragment {

	public BioListFragment() {  }

	private Artist artist;
	private User twitterUser;
	public final static int LOGIN_HANDER_RESULT = 600;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		artist = ((ArtistViewer)getActivity()).artist;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.bio_fragment, null);
		view.findViewById(R.id.socialUpdates).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(twitterUser == null)
					return;
				Uri uri = Uri.parse("https://twitter.com/" + twitterUser.getScreenName() +
						"/status/" + twitterUser.getStatus().getId());
				startActivity(new Intent(Intent.ACTION_VIEW).setData(uri));
			}
		});
		return view;
	}

	private void loadLastFm() {
		final Handler mHandler = new Handler();
		new Thread(new Runnable() {
			public void run() {
				try {
					final ArtistInfo info = LastFM.getArtistInfo(artist.getName());
					mHandler.post(new Runnable() {
						public void run() {
							if(getView() != null) {
								if(info.getBioContent() == null) {
									((TextView)getView().findViewById(R.id.bioAbout)).setText(R.string.no_bio_str);
								} else {
									((TextView)getView().findViewById(R.id.bioAbout)).setText(info.getBioContent());
								}
							}
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					mHandler.post(new Runnable() {
						public void run() {
							((TextView)getView().findViewById(R.id.bioAbout)).setText(R.string.failed_load_artist_bio);
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
					Twitter twitter = LoginHandler.getTwitterInstance(getActivity(), true);
					if(twitter == null) {
						mHandler.post(new Runnable() {
							public void run() {
								Button action = (Button)getView().findViewById(R.id.bioUpdatesAction);
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
					ResponseList<User> possibleUsers = twitter.searchUsers(artist.getName(), 0);
					boolean found = false;
					if(possibleUsers.size() > 0) {
						for(int i = 0; i < possibleUsers.size(); i++) {
							if(possibleUsers.get(i).isVerified()) {
								twitterUser = possibleUsers.get(i);
								mHandler.post(new Runnable() {
									public void run() {
										if(getView() != null) {
											getView().findViewById(R.id.bioUpdatesAction).setVisibility(View.GONE);
											TextView updates = (TextView)getView().findViewById(R.id.bioUpdates);
											updates.setText(twitterUser.getStatus().getText());
											updates.setVisibility(View.VISIBLE);
											String source = getString(R.string.social_update_source)
													.replace("{time}", Utils.getFriendlyTime(twitterUser.getStatus().getCreatedAt()))
													.replace("{user}", "@" + twitterUser.getScreenName())
													.replace("{network}", "Twitter");
											TextView sourceTxt = (TextView)getView().findViewById(R.id.bioUpdateSource);
											sourceTxt.setText(source);
											sourceTxt.setVisibility(View.VISIBLE);
										}
									}
								});
								found = true;
								break;
							}
						}
					}
					if(!found) {
						mHandler.post(new Runnable() {
							public void run() { 
								TextView updates = (TextView)getView().findViewById(R.id.bioUpdates); 
								updates.setText(R.string.no_social_profile);
								updates.setVisibility(View.VISIBLE);
								getView().findViewById(R.id.bioUpdateSource).setVisibility(View.GONE);
								getView().findViewById(R.id.bioUpdatesAction).setVisibility(View.GONE);
							}
						});
					}
				} catch(Exception e) {
					e.printStackTrace();
					mHandler.post(new Runnable() {
						public void run() {
							((Button)getView().findViewById(R.id.bioUpdatesAction)).setText(R.string.error_str);
						}
					});
				}
			}
		}).start();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == LOGIN_HANDER_RESULT && resultCode == Activity.RESULT_OK) {
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
		if(getActivity() != null && getView() != null) {
			loadTwitter();
		} else {
			twitterUser = null;
		}
	}
}
