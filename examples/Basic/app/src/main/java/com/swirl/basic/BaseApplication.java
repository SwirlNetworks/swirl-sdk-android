/*
 * Basic
 * BaseApplication
 *
 * Copyright (C) 2015-2016 Swirl Networks, Inc.
 */

package com.swirl.basic;

import android.app.Application;
import android.os.Bundle;

import com.swirl.ContentManager;
import com.swirl.Swirl;

public class BaseApplication extends Application {
	@Override public void onCreate() {
		super.onCreate();

		Bundle options = new Bundle();
		// set any options you want to set at startup (or pass null)

		Swirl.getInstance(this).addListener(new ContentManager(this));
		Swirl.getInstance().start(options);
	}
}
