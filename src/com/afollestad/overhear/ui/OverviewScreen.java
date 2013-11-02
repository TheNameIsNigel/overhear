package com.afollestad.overhear.ui;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.afollestad.overhear.R;
import com.afollestad.overhear.base.OverhearActivity;
import com.afollestad.overhear.base.OverhearGridFragment;
import com.afollestad.overhear.base.TaggedFragmentAdapter;
import com.afollestad.overhear.fragments.*;
import com.afollestad.overhear.utils.MusicUtils;
import com.afollestad.overhear.utils.Recents;
import com.afollestad.overhear.utils.Store;
import com.afollestad.overhear.utils.Twitter;

import java.io.File;
import java.util.Locale;

/**
 * The main screen of the app.
 *
 * @author Aidan Follestad
 */
public class OverviewScreen extends OverhearActivity {

    private final static int TWEET_PLAYING_LOGIN = 400;
    private ViewPager mViewPager;

    @SuppressLint("CommitTransaction")
    private static void showAboutDialog(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog_about");
        if (prev != null)
            ft.remove(prev);
        ft.addToBackStack(null);
        new AboutDialog().show(ft, "dialog_about");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TWEET_PLAYING_LOGIN && resultCode == Activity.RESULT_OK)
            startActivity(new Intent(this, TweetNowPlaying.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Create the favorites playlist if it doesn't already exist so songs, artists, and albums can be added to it manually
        MusicUtils.createFavoritesIfNotExists(this);
        setupTabs();
    }

    /**
     * Sets up the action bar tabs and the view pager.
     */
    private void setupTabs() {
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(5);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                getActionBar().setSelectedNavigationItem(position);
            }
        });

        ActionBar.TabListener mTabListener = new ActionBar.TabListener() {
            @Override
            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

            }

            @Override
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                invalidateOptionsMenu();
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                Fragment frag = getFragmentManager().findFragmentByTag("page:" + tab.getPosition());
                if (frag instanceof ListFragment) {
                    if (frag.getView() != null)
                        ((ListFragment) frag).getListView().setSelection(0);
                } else if (frag instanceof OverhearGridFragment) {
                    if (frag.getView() != null)
                        ((OverhearGridFragment) frag).getListView().setSelection(0);
                }
            }
        };

        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++)
            actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(mTabListener));
        actionBar.setSelectedNavigationItem(Store.i(this, "focused_tab", 2));
    }

    @Override
    public void onPause() {
        super.onPause();
        Store.put(this, "focused_tab", mViewPager.getCurrentItem());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overview_screen, menu);
        menu.findItem(R.id.createPlaylist).setVisible(mViewPager.getCurrentItem() == 0);
        menu.findItem(R.id.clearRecents).setVisible(mViewPager.getCurrentItem() == 1);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.tweetPlaying:
                if (Twitter.getTwitterInstance(getApplicationContext(), true) == null)
                    startActivityForResult(new Intent(this, LoginHandler.class), TWEET_PLAYING_LOGIN);
                else
                    startActivity(new Intent(this, TweetNowPlaying.class));
                return true;
            case R.id.search:
                startActivity(new Intent(this, SearchScreen.class));
                return true;
            case R.id.about:
                showAboutDialog(this);
                return true;
            case R.id.createPlaylist:
                MusicUtils.createNewPlaylistDialog(this, null).show();
                return true;
            case R.id.clearRecents:
                Recents.clear(this);
                return true;
        }
        return false;
    }

    @Override
    public void onBound() {
        ((NowPlayingBarFragment) getFragmentManager().findFragmentById(R.id.nowPlaying)).update();
    }

    public static class AboutDialog extends DialogFragment {

        private static final String VERSION_UNAVAILABLE = "N/A";

        public AboutDialog() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get app version
            PackageManager pm = getActivity().getPackageManager();
            String packageName = getActivity().getPackageName();
            String versionName;
            try {
                PackageInfo info = pm.getPackageInfo(packageName, 0);
                versionName = info.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                versionName = VERSION_UNAVAILABLE;
            }

            // Build the about body view and append the link to see OSS licenses
            LayoutInflater layoutInflater = getActivity().getLayoutInflater();
            View rootView = layoutInflater.inflate(R.layout.about_dialog, null);
            TextView nameAndVersionView = (TextView) rootView.findViewById(
                    R.id.app_name_and_version);
            nameAndVersionView.setText(Html.fromHtml(
                    getString(R.string.app_name_and_version, versionName)));

            TextView aboutBodyView = (TextView) rootView.findViewById(R.id.about_body);
            aboutBodyView.setText(Html.fromHtml(getString(R.string.about_body)));
            aboutBodyView.setMovementMethod(new LinkMovementMethod());

            return new AlertDialog.Builder(getActivity())
                    .setView(rootView)
                    .setNegativeButton(R.string.close_str, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton(R.string.twitter_str, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int i) {
                            dialog.dismiss();
                            startActivity(new Intent(Intent.ACTION_VIEW).setData(
                                    Uri.parse("http://twitter.com/OverhearApp")));
                        }
                    })
                    .create();
        }
    }

    public class SectionsPagerAdapter extends TaggedFragmentAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new PlayListFragment();
                case 1:
                    return new RecentsListFragment();
                case 2:
                    return new ArtistListFragment();
                case 3:
                    return new AlbumListFragment();
                case 4:
                    return new SongListFragment();
                case 5:
                    return new GenreListFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 6;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.playlists_str).toUpperCase(Locale.getDefault());
                case 1:
                    return getString(R.string.recent_str).toUpperCase(Locale.getDefault());
                case 2:
                    return getString(R.string.artists_str).toUpperCase(Locale.getDefault());
                case 3:
                    return getString(R.string.albums_str).toUpperCase(Locale.getDefault());
                case 4:
                    return getString(R.string.songs_str).toUpperCase(Locale.getDefault());
                case 5:
                    return getString(R.string.genres_str).toUpperCase(Locale.getDefault());
            }
            return null;
        }
    }
}