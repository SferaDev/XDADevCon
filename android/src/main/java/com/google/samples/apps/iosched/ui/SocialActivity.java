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

import android.app.ActionBar;
import android.app.Fragment;
import android.os.Bundle;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.R;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class SocialActivity extends BaseActivity {
    private static final String TAG = makeLogTag(SocialActivity.class);
    private static final String SCREEN_LABEL = "Social";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_social);
        getLPreviewUtils().trySetActionBar();
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, createSocialFragment())
                    .commit();
        }

        overridePendingTransition(0, 0);
    }

    private void updateActionBarNavigation() {
        boolean show = !isNavDrawerOpen();
        if (getLPreviewUtils().shouldChangeActionBarForDrawer()) {
            ActionBar ab = getActionBar();
            ab.setDisplayShowTitleEnabled(show);
            ab.setDisplayUseLogoEnabled(!show);
        }
    }

    protected Fragment createSocialFragment() {
        try {
            Method fragmentCreator = BuildConfig.SOCIAL_FRAGMENT_CLASS.getMethod("newInstance");
            return (Fragment) fragmentCreator.invoke(null);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Unable to create social fragment "+BuildConfig.SOCIAL_FRAGMENT_CLASS.getName(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Unable to create social fragment "+BuildConfig.SOCIAL_FRAGMENT_CLASS.getName(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Unable to create social fragment "+BuildConfig.SOCIAL_FRAGMENT_CLASS.getName(), e);
        }
    }

    @Override
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        super.onNavDrawerStateChanged(isOpen, isAnimating);
        updateActionBarNavigation();
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_SOCIAL;
    }

    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
    }
}
