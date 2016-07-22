/*
 * Swirlx
 * BaseApplication
 *
 * Copyright (C) 2015-2016 Swirl Networks, Inc.
 */

package com.swirl.swirlx;

import android.app.Application;
import android.os.Bundle;

import com.swirl.ContentManager;
import com.swirl.Swirl;

public class BaseApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        Bundle options = SettingsActivity.options(getSharedPreferences("Swirlx", MODE_PRIVATE));

        Swirl.getInstance(this).addListener(new ContentManager(this));
        Swirl.getInstance().start(options);
    }
}
