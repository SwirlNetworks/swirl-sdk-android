/*
 * Swirlx
 * BaseApplication
 *
 * Copyright (C) 2015-2016 Swirl Networks, Inc.
 */

package com.swirl.swirlx;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.swirl.Content;
import com.swirl.ContentManager;
import com.swirl.Swirl;
import com.swirl.SwirlListener;
// import com.swirl.NearbyManager;

public class BaseApplication extends Application {
	private BackgroundListener  backgroundListener = new BackgroundListener();

	@Override public void onCreate() {
		super.onCreate();

		SharedPreferences prefs = getSharedPreferences("Swirlx", MODE_PRIVATE);
		Bundle options = SettingsActivity.options(prefs);
        // uncomment this line to enable the use of Google Nearby
		// Swirl.getInstance(this).addListener(new NearbyManager(null));
		Swirl.getInstance(this).addListener(new ContentManager(this));
		Swirl.getInstance().addListener(backgroundListener);

		Swirl.getInstance().setUserInfo(SettingsActivity.userInfo(prefs));
		Swirl.getInstance().start(options);

	}

	class BackgroundListener extends SwirlListener {
		@Override protected void onReceiveContentCustom(ContentManager manager, Content content, boolean fromNotification) {
			Log.d("Swirlx", "Received Custom Content: "+content.toJSON().toString());
		}
	}
}

