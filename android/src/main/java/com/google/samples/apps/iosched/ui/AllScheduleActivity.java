package com.google.samples.apps.iosched.ui;

import com.google.samples.apps.iosched.model.AllScheduleHelper;
import com.google.samples.apps.iosched.model.BaseScheduleHelper;

/**
 * Based on Droidcon NY
 */
public class AllScheduleActivity extends BaseScheduleActivity {
    @Override
    BaseScheduleHelper makeScheduleHelper() {
        return new AllScheduleHelper(this);
    }

    @Override
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_FULL_SCHEDULE;
    }

}