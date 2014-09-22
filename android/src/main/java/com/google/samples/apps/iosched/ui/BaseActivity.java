/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.ui;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.TwitterAPI;
import com.google.samples.apps.iosched.io.JSONHandler;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.sync.ConferenceDataHandler;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.ui.debug.DebugActionRunnerActivity;
import com.google.samples.apps.iosched.ui.widget.MultiSwipeRefreshLayout;
import com.google.samples.apps.iosched.ui.widget.SwipeRefreshLayout;
import com.google.samples.apps.iosched.util.HelpUtils;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.LPreviewUtils;
import com.google.samples.apps.iosched.util.LPreviewUtilsBase;
import com.google.samples.apps.iosched.util.PrefUtils;
import com.google.samples.apps.iosched.util.UIUtils;
import com.google.samples.apps.iosched.util.WiFiUtils;

import java.io.IOException;
import java.util.ArrayList;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A base activity that handles common functionality in the app. This includes the
 * navigation drawer, login and authentication, Action Bar tweaks, amongst others.
 */
public abstract class BaseActivity extends Activity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        MultiSwipeRefreshLayout.CanChildScrollUpCallback {
    private static final String TAG = makeLogTag(BaseActivity.class);

    // Navigation drawer:
    private DrawerLayout mDrawerLayout;
    private LPreviewUtilsBase.ActionBarDrawerToggleWrapper mDrawerToggle;

    // allows access to L-Preview APIs through an abstract interface so we can compile with
    // both the L Preview SDK and with the API 19 SDK
    private LPreviewUtilsBase mLPreviewUtils;

    private ObjectAnimator mStatusBarColorAnimator;
    private ViewGroup mDrawerItemsListContainer;
    private Handler mHandler;

    // When set, these components will be shown/hidden in sync with the action bar
    // to implement the "quick recall" effect (the Action Bar and the header views disappear
    // when you scroll down a list, and reappear quickly when you scroll up).
    private ArrayList<View> mHideableHeaderViews = new ArrayList<View>();

    // Durations for certain animations we use:
    private static final int HEADER_HIDE_ANIM_DURATION = 300;

    // symbols for navdrawer items (indices must correspond to array below). This is
    // not a list of items that are necessarily *present* in the Nav Drawer; rather,
    // it's a list of all possible items.
    protected static final int NAVDRAWER_ITEM_FULL_SCHEDULE = 0;
    protected static final int NAVDRAWER_ITEM_EXPLORE = 1;
    protected static final int NAVDRAWER_ITEM_MAP = 2;
    protected static final int NAVDRAWER_ITEM_SOCIAL = 3;
    protected static final int NAVDRAWER_ITEM_VIDEO_LIBRARY = 4;
    protected static final int NAVDRAWER_ITEM_SIGN_IN = 5;
    protected static final int NAVDRAWER_ITEM_SETTINGS = 6;
    protected static final int NAVDRAWER_ITEM_EXPERTS_DIRECTORY = 7;
    protected static final int NAVDRAWER_ITEM_PEOPLE_IVE_MET = 8;

    //Partners
    protected static final int NAVDRAWER_ITEM_SONY = 9;
    protected static final int NAVDRAWER_ITEM_NVIDIA = 10;
    protected static final int NAVDRAWER_ITEM_OPPO = 11;
    protected static final int NAVDRAWER_ITEM_ONEPLUS = 12;
    protected static final int NAVDRAWER_ITEM_MEDIATEK = 13;
    protected static final int NAVDRAWER_ITEM_MOZILLA = 14;
    protected static final int NAVDRAWER_ITEM_PEBBLE = 15;
    protected static final int NAVDRAWER_ITEM_UBUNTU = 16;
    protected static final int NAVDRAWER_ITEM_JOLLA = 17;
    protected static final int NAVDRAWER_ITEM_SPUR = 18;

    protected static final int NAVDRAWER_ITEM_TWITTER = 19;

    protected static final int NAVDRAWER_ITEM_INVALID = -1;
    protected static final int NAVDRAWER_ITEM_SEPARATOR = -2;
    protected static final int NAVDRAWER_ITEM_SEPARATOR_SPECIAL = -3;

    // titles for navdrawer items (indices must correspond to the above)
    private static final int[] NAVDRAWER_TITLE_RES_ID = new int[]{
            R.string.navdrawer_item_my_schedule,
            R.string.navdrawer_item_explore,
            R.string.navdrawer_item_map,
            R.string.navdrawer_item_social,
            R.string.navdrawer_item_video_library,
            R.string.navdrawer_item_sign_in,
            R.string.navdrawer_item_settings,
            R.string.navdrawer_item_experts_directory,
            R.string.navdrawer_item_people_ive_met,
            R.string.partner_sony,
            R.string.partner_nvidia,
            R.string.partner_oppo,
            R.string.partner_oneplus,
            R.string.partner_mediatek,
            R.string.partner_mozilla,
            R.string.partner_pebble,
            R.string.partner_ubuntu,
            R.string.partner_jolla,
            R.string.partner_spur,
            R.string.twitter
    };

    // icons for navdrawer items (indices must correspond to above array)
    private static final int[] NAVDRAWER_ICON_RES_ID = new int[] {
            R.drawable.ic_drawer_my_schedule,  // My Schedule
            R.drawable.ic_drawer_explore,  // Explore
            R.drawable.ic_drawer_map, // Map
            R.drawable.ic_drawer_social, // Social
            R.drawable.ic_drawer_video_library, // Video Library
            0, // Sign in
            R.drawable.ic_drawer_settings,
            R.drawable.ic_drawer_experts,
            R.drawable.ic_drawer_people_met,
            R.drawable.ic_action_sony,
            R.drawable.ic_action_nvidia,
            R.drawable.ic_action_oppo,
            R.drawable.ic_action_oneplus,
            R.drawable.ic_action_mediatek,
            R.drawable.ic_action_mozilla,
            R.drawable.ic_action_pebble,
            R.drawable.ic_action_ubuntu,
            R.drawable.ic_action_jolla,
            R.drawable.ic_action_spur,
            R.drawable.ic_action_twitter
    };

    // delay to launch nav drawer item, to allow close animation to play
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;

    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;

    // list of navdrawer items that were actually added to the navdrawer, in order
    private ArrayList<Integer> mNavDrawerItems = new ArrayList<Integer>();

    // views that correspond to each navdrawer item, null if not yet created
    private View[] mNavDrawerItemViews = null;

    // SwipeRefreshLayout allows the user to swipe the screen down to trigger a manual refresh
    private SwipeRefreshLayout mSwipeRefreshLayout;

    // handle to our sync observer (that notifies us about changes in our sync state)
    private Object mSyncObserverHandle;

    // data bootstrap thread. Data bootstrap is the process of initializing the database
    // with the data cache that ships with the app.
    Thread mDataBootstrapThread = null;

    // variables that control the Action Bar auto hide behavior (aka "quick recall")
    private boolean mActionBarAutoHideEnabled = false;
    private int mActionBarAutoHideSensivity = 0;
    private int mActionBarAutoHideMinY = 0;
    private int mActionBarAutoHideSignal = 0;
    private boolean mActionBarShown = true;

    // A Runnable that we should execute when the navigation drawer finishes its closing animation
    private Runnable mDeferredOnDrawerClosedRunnable;

    private int mThemedStatusBarColor;
    private int mProgressBarTopWhenActionBarShown;
    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();
    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PrefUtils.init(this);

        // Check if the EULA has been accepted; if not, show it.
        if (!PrefUtils.isTosAccepted(this)) {
            Intent intent = new Intent(this, WelcomeActivity.class);
            startActivity(intent);
            finish();
        }

        mImageLoader = new ImageLoader(this);
        mHandler = new Handler();

        // Enable or disable each Activity depending on the form factor. This is necessary
        // because this app uses many implicit intents where we don't name the exact Activity
        // in the Intent, so there should only be one enabled Activity that handles each
        // Intent in the app.
        UIUtils.enableDisableActivitiesByFormFactor(this);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        ActionBar ab = getActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mLPreviewUtils = LPreviewUtils.getInstance(this);
        mThemedStatusBarColor = getResources().getColor(R.color.theme_primary_dark);
    }

    private void trySetupSwipeRefresh() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setColorScheme(
                    R.color.refresh_progress_1,
                    R.color.refresh_progress_2,
                    R.color.refresh_progress_3,
                    R.color.refresh_progress_4);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    //TODO Refresh
                }
            });

            if (mSwipeRefreshLayout instanceof MultiSwipeRefreshLayout) {
                MultiSwipeRefreshLayout mswrl = (MultiSwipeRefreshLayout) mSwipeRefreshLayout;
                mswrl.setCanChildScrollUpCallback(this);
            }
        }
    }

    protected void setProgressBarTopWhenActionBarShown(int progressBarTopWhenActionBarShown) {
        mProgressBarTopWhenActionBarShown = progressBarTopWhenActionBarShown;
        updateSwipeRefreshProgressBarTop();
    }

    private void updateSwipeRefreshProgressBarTop() {
        if (mSwipeRefreshLayout == null) {
            return;
        }

        if (mActionBarShown) {
            mSwipeRefreshLayout.setProgressBarTop(mProgressBarTopWhenActionBarShown);
        } else {
            mSwipeRefreshLayout.setProgressBarTop(0);
        }
    }

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses
     * of BaseActivity override this to indicate what nav drawer item corresponds to them
     * Return NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    /**
     * Sets up the navigation drawer as appropriate. Note that the nav drawer will be
     * different depending on whether the attendee indicated that they are attending the
     * event on-site vs. attending remotely.
     */
    private void setupNavDrawer() {
        // What nav drawer item should be selected?
        int selfItem = getSelfNavDrawerItem();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }
        if (selfItem == NAVDRAWER_ITEM_INVALID) {
            // do not show a nav drawer
            View navDrawer = mDrawerLayout.findViewById(R.id.navdrawer);
            if (navDrawer != null) {
                ((ViewGroup) navDrawer.getParent()).removeView(navDrawer);
            }
            mDrawerLayout = null;
            return;
        }

        mDrawerToggle = mLPreviewUtils.setupDrawerToggle(mDrawerLayout, new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerClosed(View drawerView) {
                // run deferred action, if we have one
                if (mDeferredOnDrawerClosedRunnable != null) {
                    mDeferredOnDrawerClosedRunnable.run();
                    mDeferredOnDrawerClosedRunnable = null;
                }
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                updateStatusBarForNavDrawerSlide(0f);
                onNavDrawerStateChanged(false, false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
                updateStatusBarForNavDrawerSlide(1f);
                onNavDrawerStateChanged(true, false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                invalidateOptionsMenu();
                onNavDrawerStateChanged(isNavDrawerOpen(), newState != DrawerLayout.STATE_IDLE);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                updateStatusBarForNavDrawerSlide(slideOffset);
                onNavDrawerSlide(slideOffset);
            }
        });
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // populate the nav drawer with the correct items
        populateNavDrawer();

        mDrawerToggle.syncState();

        // When the user runs the app for the first time, we want to land them with the
        // navigation drawer open. But just the first time.
        if (!PrefUtils.isWelcomeDone(this)) {
            // first run of the app starts with the nav drawer open
            PrefUtils.markWelcomeDone(this);
            mDrawerLayout.openDrawer(Gravity.START);
        }
    }

    // Subclasses can override this for custom behavior
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        if (mActionBarAutoHideEnabled && isOpen) {
            autoShowOrHideActionBar(true);
        }
    }

    protected void onNavDrawerSlide(float offset) {}

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START);
    }

    /** Populates the navigation drawer with the appropriate items. */
    private void populateNavDrawer() {
        boolean attendeeAtVenue = PrefUtils.isAttendeeAtVenue(this);
        mNavDrawerItems.clear();

        mNavDrawerItems.add(NAVDRAWER_ITEM_FULL_SCHEDULE);
        mNavDrawerItems.add(NAVDRAWER_ITEM_EXPLORE);

        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR);

        mNavDrawerItems.add(NAVDRAWER_ITEM_MAP);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SOCIAL);
        mNavDrawerItems.add(NAVDRAWER_ITEM_TWITTER);

        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR_SPECIAL);

        mNavDrawerItems.add(NAVDRAWER_ITEM_SONY);
        mNavDrawerItems.add(NAVDRAWER_ITEM_NVIDIA);
        mNavDrawerItems.add(NAVDRAWER_ITEM_OPPO);
        mNavDrawerItems.add(NAVDRAWER_ITEM_ONEPLUS);
        mNavDrawerItems.add(NAVDRAWER_ITEM_MEDIATEK);
        mNavDrawerItems.add(NAVDRAWER_ITEM_MOZILLA);
        mNavDrawerItems.add(NAVDRAWER_ITEM_PEBBLE);
        mNavDrawerItems.add(NAVDRAWER_ITEM_UBUNTU);
        mNavDrawerItems.add(NAVDRAWER_ITEM_JOLLA);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SPUR);

        // If attendee is on-site, show the People I've Met item
        if (BuildConfig.SUPPORTS_PEER_BADGE_SCANNING && attendeeAtVenue) {
            mNavDrawerItems.add(NAVDRAWER_ITEM_PEOPLE_IVE_MET);
        }

        //mNavDrawerItems.add(NAVDRAWER_ITEM_EXPERTS_DIRECTORY);
        //mNavDrawerItems.add(NAVDRAWER_ITEM_SETTINGS);

        // Other items that are always in the nav drawer irrespective of whether the
        // attendee is on-site or remote:
        if(BuildConfig.HAS_VIDEO_LIBRARY) {
            mNavDrawerItems.add(NAVDRAWER_ITEM_VIDEO_LIBRARY);
        }

        createNavDrawerItems();
    }

    private void createNavDrawerItems() {
        mDrawerItemsListContainer = (ViewGroup) findViewById(R.id.navdrawer_items_list);
        if (mDrawerItemsListContainer == null) {
            return;
        }

        mNavDrawerItemViews = new View[mNavDrawerItems.size()];
        mDrawerItemsListContainer.removeAllViews();
        int i = 0;
        for (int itemId : mNavDrawerItems) {
            mNavDrawerItemViews[i] = makeNavDrawerItem(itemId, mDrawerItemsListContainer);
            mDrawerItemsListContainer.addView(mNavDrawerItemViews[i]);
            ++i;
        }
    }

    /**
     * Sets up the given navdrawer item's appearance to the selected state. Note: this could
     * also be accomplished (perhaps more cleanly) with state-based layouts.
     */
    private void setSelectedNavDrawerItem(int itemId) {
        if (mNavDrawerItemViews != null) {
            for (int i = 0; i < mNavDrawerItemViews.length; i++) {
                if (i < mNavDrawerItems.size()) {
                    int thisItemId = mNavDrawerItems.get(i);
                    formatNavDrawerItem(mNavDrawerItemViews[i], thisItemId, itemId == thisItemId);
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PrefUtils.PREF_ATTENDEE_AT_VENUE)) {
            LOGD(TAG, "Attendee at venue preference changed, repopulating nav drawer and menu.");
            populateNavDrawer();
            invalidateOptionsMenu();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavDrawer();

        trySetupSwipeRefresh();
        updateSwipeRefreshProgressBarTop();

        View mainContent = findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        } else {
            LOGW(TAG, "No view with ID main_content to fade in.");
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (id) {
            case R.id.menu_about:
                HelpUtils.showAbout(this);
                return true;

            case R.id.menu_wifi:
                WiFiUtils.showWiFiDialog(this);
                return true;

            case R.id.menu_i_o_hunt:
                launchIoHunt();
                return true;

            case R.id.menu_debug:
                if (BuildConfig.DEBUG) {
                    startActivity(new Intent(this, DebugActionRunnerActivity.class));
                }
                return true;

            case R.id.menu_refresh:
                //TODO
                break;

            case R.id.menu_io_extended:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(Config.IO_EXTENDED_LINK)));
                break;

            case R.id.menu_map:
                Uri locationUri = constructExternalMappingAppUri();
                Intent intent = new Intent(Intent.ACTION_VIEW, locationUri);
                if(intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    finish();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchIoHunt() {
        if (!TextUtils.isEmpty(Config.IO_HUNT_PACKAGE_NAME)) {
            LOGD(TAG, "Attempting to launch I/O hunt.");
            PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(Config.IO_HUNT_PACKAGE_NAME);
            if (launchIntent != null) {
                // start I/O Hunt
                LOGD(TAG, "I/O hunt intent found, launching.");
                startActivity(launchIntent);
            } else {
                // send user to the Play Store to download it
                LOGD(TAG, "I/O hunt intent NOT found, going to Play Store.");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                        Config.PLAY_STORE_URL_PREFIX + Config.IO_HUNT_PACKAGE_NAME));
                startActivity(intent);
            }
        }
    }

    private void goToNavDrawerItem(int item) {
        Intent intent;
        switch (item) {
            case NAVDRAWER_ITEM_FULL_SCHEDULE:
                intent = new Intent(this, AllScheduleActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_EXPLORE:
                intent = new Intent(this, BrowseSessionsActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_MAP:
                Uri locationUri = constructExternalMappingAppUri();
                intent = new Intent(Intent.ACTION_VIEW, locationUri);
                if(intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    finish();
                }
                break;
            case NAVDRAWER_ITEM_SOCIAL:
                intent = new Intent(this, SocialActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_EXPERTS_DIRECTORY:
                intent = new Intent(this, ExpertsDirectoryActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_PEOPLE_IVE_MET:
                //DEPRECATED
                break;
            case NAVDRAWER_ITEM_SIGN_IN:
                //DEPRECATED
                break;
            case NAVDRAWER_ITEM_SETTINGS:
                intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                break;
            case NAVDRAWER_ITEM_VIDEO_LIBRARY:
                intent = new Intent(this, VideoLibraryActivity.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_TWITTER:
                TwitterAPI.createTweetDialog(this, "XDADevConQA");
                break;
            case NAVDRAWER_ITEM_SONY:
                openURL("http://www.sonymobile.com/us/");
                finish();
                break;
            case NAVDRAWER_ITEM_NVIDIA:
                openURL("http://www.nvidia.com/content/global/global.php");
                finish();
                break;
            case NAVDRAWER_ITEM_OPPO:
                openURL("http://global.oppo.com/");
                finish();
                break;
            case NAVDRAWER_ITEM_ONEPLUS:
                openURL("http://oneplus.net");
                finish();
                break;
            case NAVDRAWER_ITEM_MEDIATEK:
                openURL("http://www.mediatek.com/");
                finish();
                break;
            case NAVDRAWER_ITEM_MOZILLA:
                openURL("http://www.mozilla.org/");
                finish();
                break;
            case NAVDRAWER_ITEM_PEBBLE:
                openURL("https://getpebble.com/");
                finish();
                break;
            case NAVDRAWER_ITEM_UBUNTU:
                openURL("http://www.ubuntu.com/");
                finish();
                break;
            case NAVDRAWER_ITEM_JOLLA:
                openURL("http://jolla.com/");
                finish();
                break;
            case NAVDRAWER_ITEM_SPUR:
                openURL("http://epitomical.com/");
                finish();
                break;
        }
    }

    private Uri constructExternalMappingAppUri() {
        String uri = "geo:0,0?q="
                + BuildConfig.VENUE_LATITUDE + "," + BuildConfig.VENUE_LONGITUDE
                + "("+BuildConfig.CONFERENCE_NAME+")";
        return Uri.parse(uri);
    }

    private void onNavDrawerItemClicked(final int itemId) {
        if (itemId == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(Gravity.START);
            return;
        }

        if (isSpecialItem(itemId)) {
            goToNavDrawerItem(itemId);
        } else {
            // launch the target Activity after a short delay, to allow the close animation to play
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToNavDrawerItem(itemId);
                }
            }, NAVDRAWER_LAUNCH_DELAY);

            // change the active item on the list so the user can see the item changed
            setSelectedNavDrawerItem(itemId);
            // fade out the main content
            View mainContent = findViewById(R.id.main_content);
            if (mainContent != null) {
                mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
            }
        }

        mDrawerLayout.closeDrawer(Gravity.START);
    }

    protected void configureStandardMenuItems(Menu menu) {
        MenuItem wifiItem = menu.findItem(R.id.menu_wifi);
        if (wifiItem != null && !WiFiUtils.shouldOfferToSetupWifi(this, false)) {
            wifiItem.setVisible(false);
        }

        MenuItem debugItem = menu.findItem(R.id.menu_debug);
        if (debugItem != null) {
            debugItem.setVisible(BuildConfig.DEBUG);
        }

        MenuItem ioExtendedItem = menu.findItem(R.id.menu_io_extended);
        if (ioExtendedItem != null) {
            ioExtendedItem.setVisible(PrefUtils.shouldOfferIOExtended(this, false));
        }

        // if attendee is remote, show map on the overflow instead of on the nav bar
        final boolean isRemote = !PrefUtils.isAttendeeAtVenue(this);
        final MenuItem mapItem = menu.findItem(R.id.menu_map);
        if (mapItem != null) {
            mapItem.setVisible(isRemote);
        }

        MenuItem ioHuntItem = menu.findItem(R.id.menu_i_o_hunt);
        if (ioHuntItem != null) {
            ioHuntItem.setVisible(!isRemote && !TextUtils.isEmpty(Config.IO_HUNT_PACKAGE_NAME));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
     */
    public static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

    /**
     * Converts a fragment arguments bundle into an intent.
     */
    public static Intent fragmentArgumentsToIntent(Bundle arguments) {
        Intent intent = new Intent();
        if (arguments == null) {
            return intent;
        }

        final Uri data = arguments.getParcelable("_uri");
        if (data != null) {
            intent.setData(data);
        }

        intent.putExtras(arguments);
        intent.removeExtra("_uri");
        return intent;
    }

    @Override
    public void onStart() {
        LOGD(TAG, "onStart");
        super.onStart();

        // Perform one-time bootstrap setup, if needed
        if (!PrefUtils.isDataBootstrapDone(this) && mDataBootstrapThread == null) {
            LOGD(TAG, "One-time data bootstrap not done yet. Doing now.");
            performDataBootstrap();
        }
    }

    /**
     * Performs the one-time data bootstrap. This means taking our prepackaged conference data
     * from the R.raw.bootstrap_data resource, and parsing it to populate the database. This
     * data contains the sessions, speakers, etc.
     */
    private void performDataBootstrap() {
        final Context appContext = getApplicationContext();
        LOGD(TAG, "Starting data bootstrap background thread.");
        mDataBootstrapThread = new Thread(new Runnable() {
            @Override
            public void run() {
                LOGD(TAG, "Starting data bootstrap process.");
                try {
                    // Load data from bootstrap raw resource
                    String bootstrapJson = JSONHandler.parseResource(appContext, R.raw.bootstrap_data);

                    // Apply the data we read to the database with the help of the ConferenceDataHandler
                    ConferenceDataHandler dataHandler = new ConferenceDataHandler(appContext);
                    dataHandler.applyConferenceData(new String[]{bootstrapJson},
                            Config.BOOTSTRAP_DATA_TIMESTAMP, false);
                    SyncHelper.performPostSyncChores(appContext);
                    LOGI(TAG, "End of bootstrap -- successful. Marking boostrap as done.");
                    PrefUtils.markSyncSucceededNow(appContext);
                    PrefUtils.markDataBootstrapDone(appContext);
                    getContentResolver().notifyChange(Uri.parse(ScheduleContract.CONTENT_AUTHORITY),
                            null, false);
                } catch (IOException ex) {
                    // This is serious -- if this happens, the app won't work :-(
                    // This is unlikely to happen in production, but IF it does, we apply
                    // this workaround as a fallback: we pretend we managed to do the bootstrap
                    // and hope that a remote sync will work.
                    LOGE(TAG, "*** ERROR DURING BOOTSTRAP! Problem in bootstrap data?");
                    LOGE(TAG, "Applying fallback -- marking boostrap as done; sync might fix problem.");
                    PrefUtils.markDataBootstrapDone(appContext);
                }

                mDataBootstrapThread = null;
            }
        });
        mDataBootstrapThread.start();
    }

    @Override
    public void onStop() {
        LOGD(TAG, "onStop");
        super.onStop();
    }

    /**
     * Initializes the Action Bar auto-hide (aka Quick Recall) effect.
     */
    private void initActionBarAutoHide() {
        mActionBarAutoHideEnabled = true;
        mActionBarAutoHideMinY = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_min_y);
        mActionBarAutoHideSensivity = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_sensivity);
    }

    /**
     * Indicates that the main content has scrolled (for the purposes of showing/hiding
     * the action bar for the "action bar auto hide" effect). currentY and deltaY may be exact
     * (if the underlying view supports it) or may be approximate indications:
     * deltaY may be INT_MAX to mean "scrolled forward indeterminately" and INT_MIN to mean
     * "scrolled backward indeterminately".  currentY may be 0 to mean "somewhere close to the
     * start of the list" and INT_MAX to mean "we don't know, but not at the start of the list"
     */
    private void onMainContentScrolled(int currentY, int deltaY) {
        if (deltaY > mActionBarAutoHideSensivity) {
            deltaY = mActionBarAutoHideSensivity;
        } else if (deltaY < -mActionBarAutoHideSensivity) {
            deltaY = -mActionBarAutoHideSensivity;
        }

        if (Math.signum(deltaY) * Math.signum(mActionBarAutoHideSignal) < 0) {
            // deltaY is a motion opposite to the accumulated signal, so reset signal
            mActionBarAutoHideSignal = deltaY;
        } else {
            // add to accumulated signal
            mActionBarAutoHideSignal += deltaY;
        }

        boolean shouldShow = currentY < mActionBarAutoHideMinY ||
                (mActionBarAutoHideSignal <= -mActionBarAutoHideSensivity);
        autoShowOrHideActionBar(shouldShow);
    }

    protected void autoShowOrHideActionBar(boolean show) {
        if (show == mActionBarShown) {
            return;
        }

        mActionBarShown = show;
        getLPreviewUtils().showHideActionBarIfPartOfDecor(show);
        onActionBarAutoShowOrHide(show);
    }

    protected void enableActionBarAutoHide(final ListView listView) {
        initActionBarAutoHide();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            final static int ITEMS_THRESHOLD = 3;
            int lastFvi = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                onMainContentScrolled(firstVisibleItem <= ITEMS_THRESHOLD ? 0 : Integer.MAX_VALUE,
                        lastFvi - firstVisibleItem > 0 ? Integer.MIN_VALUE :
                                lastFvi == firstVisibleItem ? 0 : Integer.MAX_VALUE
                );
                lastFvi = firstVisibleItem;
            }
        });
    }

    private View makeNavDrawerItem(final int itemId, ViewGroup container) {
        boolean selected = getSelfNavDrawerItem() == itemId;
        int layoutToInflate = 0;
        if (itemId == NAVDRAWER_ITEM_SEPARATOR) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else if (itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else {
            layoutToInflate = R.layout.navdrawer_item;
        }
        View view = getLayoutInflater().inflate(layoutToInflate, container, false);

        if (isSeparator(itemId)) {
            // we are done
            UIUtils.setAccessibilityIgnore(view);
            return view;
        }

        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        int iconId = itemId >= 0 && itemId < NAVDRAWER_ICON_RES_ID.length ?
                NAVDRAWER_ICON_RES_ID[itemId] : 0;
        int titleId = itemId >= 0 && itemId < NAVDRAWER_TITLE_RES_ID.length ?
                NAVDRAWER_TITLE_RES_ID[itemId] : 0;

        // set icon and text
        iconView.setVisibility(iconId > 0 ? View.VISIBLE : View.GONE);
        if (iconId > 0) {
            iconView.setImageResource(iconId);
        }
        titleView.setText(getString(titleId));

        formatNavDrawerItem(view, itemId, selected);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavDrawerItemClicked(itemId);
            }
        });

        return view;
    }

    private boolean isSpecialItem(int itemId) {
        return itemId == NAVDRAWER_ITEM_SETTINGS;
    }

    private boolean isSeparator(int itemId) {
        return itemId == NAVDRAWER_ITEM_SEPARATOR || itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL;
    }

    private void formatNavDrawerItem(View view, int itemId, boolean selected) {
        if (isSeparator(itemId)) {
            // not applicable
            return;
        }

        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        TextView titleView = (TextView) view.findViewById(R.id.title);

        // configure its appearance according to whether or not it's selected
        titleView.setTextColor(selected ?
                getResources().getColor(R.color.navdrawer_text_color_selected) :
                getResources().getColor(R.color.navdrawer_text_color));
        iconView.setColorFilter(selected ?
                getResources().getColor(R.color.navdrawer_icon_tint_selected) :
                getResources().getColor(R.color.navdrawer_icon_tint));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
    }

    protected void onRefreshingStateChanged(boolean refreshing) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    protected void enableDisableSwipeRefresh(boolean enable) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(enable);
        }
    }

    protected void registerHideableHeaderView(View hideableHeaderView) {
        if (!mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.add(hideableHeaderView);
        }
    }

    protected void deregisterHideableHeaderView(View hideableHeaderView) {
        if (mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.remove(hideableHeaderView);
        }
    }

    public LPreviewUtilsBase getLPreviewUtils() {
        return mLPreviewUtils;
    }

    private void updateStatusBarForNavDrawerSlide(float slideOffset) {
        if (mStatusBarColorAnimator != null) {
            mStatusBarColorAnimator.cancel();
        }

        if (!mActionBarShown) {
            mLPreviewUtils.setStatusBarColor(Color.BLACK);
            return;
        }

        mLPreviewUtils.setStatusBarColor((Integer) ARGB_EVALUATOR.evaluate(slideOffset,
                mThemedStatusBarColor, Color.BLACK));
    }

    protected void onActionBarAutoShowOrHide(boolean shown) {
        if (mStatusBarColorAnimator != null) {
            mStatusBarColorAnimator.cancel();
        }
        mStatusBarColorAnimator = ObjectAnimator.ofInt(mLPreviewUtils, "statusBarColor",
                shown ? mThemedStatusBarColor : Color.BLACK).setDuration(250);
        mStatusBarColorAnimator.setEvaluator(ARGB_EVALUATOR);
        mStatusBarColorAnimator.start();

        updateSwipeRefreshProgressBarTop();

        for (View view : mHideableHeaderViews) {
            if (shown) {
                view.animate()
                        .translationY(0)
                        .alpha(1)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            } else {
                view.animate()
                        .translationY(-view.getBottom())
                        .alpha(0)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            }
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return false;
    }

    public void openURL(String url) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
}
