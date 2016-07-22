/*
 * Swirlx
 * MainActivity
 *
 * Copyright (C) 2015-2016 Swirl Networks, Inc.
 */

package com.swirl.swirlx;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.swirl.ContentManager;
import com.swirl.Location;
import com.swirl.Region;
import com.swirl.Swirl;
import com.swirl.SwirlListener;
import com.swirl.Visit;
import com.swirl.VisitManager;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RESET_DELAY    = 10;

    private int             startPending    = 0;
    private Handler         mainHandler     = new Handler(Looper.getMainLooper());
    private StatusListener  statusListener  = new StatusListener();

    public static boolean settingsChanged;

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION }, 8);
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Swirl.getInstance().addListener(statusListener);
        statusListener.onStatusChange(Swirl.getInstance().getStatus());

        updateIdentifierVersion();

        requestPermissions();
    }

    private void updateIdentifierVersion() {
        TextView textView = (TextView)findViewById(R.id.identifier_version);
        textView.setText(String.format("Identifier: %s / Version: %s",
                Swirl.getInstance().getUserKey().substring(0, 5),
                Swirl.getInstance().getVersion()));
    }

    private String onOff(int value) {
        return value != 0 ? "On" : "Off";
    }

    private void updateDeviceStatus() {
        int status = Swirl.getInstance().getStatus();
        TextView textView = (TextView)findViewById(R.id.device_status);
        textView.setText(String.format("Location: %s / Bluetooth: %s / Network: %s",
                onOff(status & Swirl.STATUS_LOCATION), onOff(status & Swirl.STATUS_BLUETOOTH),
                onOff(status & Swirl.STATUS_NETWORK)));
    }

    private void updateStatus(String message) {
        TextView status = (TextView)findViewById(R.id.swirl_status);
        status.setText(message);
        updateDeviceStatus();
    }

    private void updateLocationStatus(Location location) {
        TextView status = (TextView)findViewById(R.id.location_status);
        if (location != null && startPending == 0) {
            android.location.Location geo = Swirl.getInstance().getLocation();
            String geo_string = String.format("Geo: %.4f, %.4f", geo.getLatitude(), geo.getLongitude());
            if (location.getSignal() instanceof Region) {
                float distance = ((Region) location.getSignal()).getCenter().distanceTo(geo);
                geo_string = String.format("%s, distance: %.3fm", geo_string, distance);
            }
            status.setText(location.toString() + "\n" + location.getSignal().toString() + "\n" + geo_string);
        } else
            status.setText("No Signal Detected");
        updateDeviceStatus();
    }

    private void startAfterCountdown() {
        if (startPending > 0) {
            updateStatus(String.format("Starting in %d", startPending--));
            updateLocationStatus(null);
            mainHandler.postDelayed(new Runnable() {
                @Override public void run() {
                    startAfterCountdown();
                }
            }, 1000);
        } else {
            Bundle options = SettingsActivity.options(getSharedPreferences("Swirlx", MODE_PRIVATE));
            Swirl.getInstance().start(options);
        }
    }

    private void reset() {
        if (startPending == 0) {
            updateStatus("Resetting");
            Swirl.getInstance().reset();
            startPending = RESET_DELAY;
            startAfterCountdown();
        }
    }

    public void onShowContent(View v) {
        ContentManager.getInstance().startContentViewActivity();
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:  {
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            }
            case R.id.action_reset: {
                reset();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override public void onDestroy() {
        super.onDestroy();
    }
    @Override public void onPause() {
        Log.d("SWIRLX", "PAUSE");
        super.onPause();
    }
    @Override public void onResume() {
        super.onResume();
        Swirl.getInstance().addListener(statusListener);

        if (settingsChanged) {
            settingsChanged = false;
            reset();
        }
    }
    @Override public void onStop() {
        super.onStop();
    }

    class StatusListener extends SwirlListener {
        public StatusListener() {
        }

        @Override protected void onStatusChange(int status) {
            switch (status & Swirl.STATUS_SWIRL_MASK) {
                case Swirl.STATUS_NONE:                         updateStatus("Stopped");    break;
                case Swirl.STATUS_PENDING:                      updateStatus("Starting");   break;
                case Swirl.STATUS_RUNNING:                      updateStatus("Running");    break;
                case Swirl.STATUS_RUNNING|Swirl.STATUS_PENDING: updateStatus("Stopping");   break;
                default: {
                    updateStatus("Error");
//                [Swirl shared].error.localizedDescription]];
                }
            }
        }
        private Visit getFirstVisit(VisitManager manager) {
            ArrayList<Visit> activeVisits = manager.getActivePlacementVisits();
            Log.i("swirlx", "Visits:");
            for (Visit visit: activeVisits) {
                Log.i("swirlx", visit.toString());
            }
            return activeVisits.size() > 0 ? activeVisits.get(0) : null;
        }

        @Override protected void onBeginVisit(VisitManager manager, Visit visit) {
            updateLocationStatus(getFirstVisit(manager).getLocation());
        }
        @Override protected void onDwellVisit(VisitManager manager, Visit visit) {
            updateLocationStatus(getFirstVisit(manager).getLocation());
        }
        @Override protected void onEndVisit(VisitManager manager, Visit visit) {
            updateLocationStatus(getFirstVisit(manager).getLocation());
        }

    }
}
