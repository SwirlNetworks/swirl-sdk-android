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
import com.swirl.Completion;
import com.swirl.Content;
import com.swirl.ContentManager;
import com.swirl.Swirl;
import com.swirl.SwirlListener;
// import com.swirl.NearbyManager;
// import com.swirl.swirlx.sample_integrations.KouponMediaListener;


public class BaseApplication extends Application {
	private BackgroundListener  backgroundListener = new BackgroundListener();

	@Override public void onCreate() {
		super.onCreate();

		SharedPreferences prefs = getSharedPreferences("Swirlx", MODE_PRIVATE);
		Bundle options = SettingsActivity.options(prefs);
        // uncomment this line to enable the use of Google Nearby
		// Swirl.getInstance(this).addListener(new NearbyManager(null));
		Swirl.getInstance(this).addListener(new ContentManager(this));

		// KouponMediaManager is provided with source code as a sample custom content integration with KouponMedia
		// You will need an account, api key and secret from Koupon Media to use this manager
		// Swirl.getInstance(this).addListener(new KouponMediaListener(this, "KOUPON-API-KEY", "KOUPON-SECRET", "KOUPON-USER"));

		Swirl.getInstance().addListener(backgroundListener);
		Swirl.getInstance().setUserInfo(SettingsActivity.userInfo(prefs));
		Swirl.getInstance().start(options);

	}

	class BackgroundListener extends SwirlListener {
		@Override protected void onStatusChange(int status) {
			Log.i("SWIRLX", "onStatusChange: " + status);
		}

		@Override protected void onError(int error) {
			Log.i("SWIRLX", "onError: " + error);
		}

		@Override protected void onStarted() {
		}

		@Override protected void onReceiveContentCustom(ContentManager manager, Content content, Completion completion) {
			Log.d("Swirlx", "Received Custom Content: "+content.toJSON().toString());
		}
	}
}

