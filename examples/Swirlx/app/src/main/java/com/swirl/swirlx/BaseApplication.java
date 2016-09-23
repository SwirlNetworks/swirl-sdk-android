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

import org.json.JSONObject;

public class BaseApplication extends Application {
	private BackgroundListener  backgroundListener = new BackgroundListener();

	@Override public void onCreate() {
		super.onCreate();

		SharedPreferences prefs = getSharedPreferences("Swirlx", MODE_PRIVATE);

		Bundle options = SettingsActivity.options(prefs);
		Swirl.getInstance(this).addListener(new ContentManager(this));
		Swirl.getInstance().addListener(backgroundListener);
		Swirl.getInstance().start(options);

		JSONObject userInfo = SettingsActivity.userInfo(prefs);
		Swirl.getInstance().setUserInfo(userInfo);
	}

	class BackgroundListener extends SwirlListener {
		@Override protected void onReceiveContentCustom(ContentManager manager, Content content, boolean fromNotification) {
			Log.d("Swirlx", "Received Custom Content: "+content.toJSON().toString());
		}
	}
}

