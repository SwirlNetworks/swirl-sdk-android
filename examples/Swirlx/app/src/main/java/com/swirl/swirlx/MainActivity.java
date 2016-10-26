/*
 * Swirlx
 * MainActivity
 *
 * Copyright (C) 2015-2016 Swirl Networks, Inc.
 */

package com.swirl.swirlx;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.util.Log;
import android.widget.TextView;

import com.swirl.Content;
import com.swirl.ContentManager;
import com.swirl.Location;
import com.swirl.Swirl;
import com.swirl.SwirlListener;
import com.swirl.Visit;
import com.swirl.VisitManager;
//import com.swirl.NearbyManager;

import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int RESET_DELAY    = 10;
    private static final int LOCATION_PERMISSION_REQUEST = 8;

    private int             startPending       = 0;
    private Handler         mainHandler        = new Handler(Looper.getMainLooper());
    private StatusListener  statusListener     = new StatusListener();
    private String          contentAttributes;

    static boolean settingsChanged;

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION }, LOCATION_PERMISSION_REQUEST);
        }
    }
    @Override public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST: {
                Swirl.getInstance().permissionsChanged();
            }
        }
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        Log.d("SWIRLX", "CREATE");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        updateIdentifierVersion();
        //NearbyManager.getInstance().setActivity(this);
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

    private void enable(int id, boolean enable) {
        View v;
        if ((v = findViewById(id)) != null) {
            v.setAlpha(enable ? 1.0f : 0.5f);
            v.setEnabled(enable);
        }
    }

    private void updateLocationStatus(Location location) {
        TextView status = (TextView)findViewById(R.id.location_status);
        if (location != null && startPending == 0) {
            status.setText( String.format( "%s\n%s\n%s\n", location.toString(), location.getSignal().toString(),
                    contentAttributes != null ? contentAttributes : "" ) );
            enable(R.id.all_visits, true);
        } else {
            status.setText("No Signal Detected");
            enable(R.id.all_visits, false);
        }

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
            SharedPreferences prefs = getSharedPreferences("Swirlx", MODE_PRIVATE);

            Bundle options = SettingsActivity.options(prefs);
            Swirl.getInstance().start(options);

            JSONObject userInfo = SettingsActivity.userInfo(prefs);
            Swirl.getInstance().setUserInfo(userInfo);

            //NearbyManager.getInstance().setActivity(this);

        }
    }

    private void reset() {
        if (startPending == 0) {
            updateStatus("Resetting");
            contentAttributes = "";
            Swirl.getInstance().reset();
            startPending = RESET_DELAY;
            startAfterCountdown();
        }
    }

    public void onShowVisits(View v) {
        startActivity(new Intent(this, VisitActivity.class));
    }

    public void onShowContent(View v) {
        ContentManager.getInstance().startContentViewActivity();
    }

    public void onShowMap(View v) {
        startActivity(new Intent(this, MapActivity.class));
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
            case R.id.action_settings2: {
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            }
            case R.id.action_reset:
            case R.id.action_reset2: {
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
    @Override public void onPause() {
        Swirl.getInstance().removeListener(statusListener);
        super.onPause();
    }
    @Override public void onResume() {
        super.onResume();
        Swirl.getInstance().addListener(statusListener);
        statusListener.onStatusChange(Swirl.getInstance().getStatus());
        statusListener.onLocationUpdate();

        //NearbyManager.getInstance().setActivity(this);

        if (settingsChanged) {
            settingsChanged = false;
            reset();
        }
    }
    @Override public void onStop() {
        super.onStop();
    }

    class StatusListener extends SwirlListener {
        private VisitManager visitManager = null;

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
                }
            }
        }

        VisitManager getVisitManager() {
            return visitManager != null ? visitManager :
                    (visitManager = (VisitManager)Swirl.getInstance().findListener(VisitManager.class));
        }

        Location getFirstLocation(VisitManager manager) {
            if ((visitManager = manager) != null) {
                ArrayList<Visit> activeVisits = visitManager.getActivePlacementVisits();
                Log.i("swirlx", "Visits:");
                for (Visit visit: activeVisits) {
                    Log.i("swirlx", visit.toString());
                }
                return activeVisits.size() > 0 ? activeVisits.get(0).getLocation() : null;
            }
            return null;
        }

        public void onLocationUpdate() {
            updateLocationStatus(getFirstLocation(getVisitManager()));
        }

//        @Override protected void onNearbyMessage(NearbyManager manager, String namespace, String type, byte[] content, int rssi) {
//            try {
//                Log.i("swirlx", "namespace=" + namespace + ", type=" + type + ", content=" + new String(content) + ", rssi=" + rssi);
//            } catch (Throwable e) {
//                Log.e("swirlx", Log.getStackTraceString(e));
//            }
//        }


        @Override protected void onBeginVisit(VisitManager manager, Visit visit) {
            visitManager = manager; onLocationUpdate();
        }
        @Override protected void onDwellVisit(VisitManager manager, Visit visit) {
            visitManager = manager; onLocationUpdate();
        }
        @Override protected void onEndVisit(VisitManager manager, Visit visit) {
            visitManager = manager; onLocationUpdate();
        }

        @Override protected void onReceiveContentURL(ContentManager manager, Content content, boolean fromNotification) {
            contentReceived(content);
        }
        @Override protected void onReceiveContentView(ContentManager manager, Content content, boolean fromNotification) {
            contentReceived(content);
        }
        @Override protected void onReceiveContentCustom(ContentManager manager, Content content, boolean fromNotification) {
            contentReceived(content);
        }
        private void contentReceived(Content content) {
            if (content.getAttributes() != null) {
                contentAttributes = String.format("Content Attributes: %s", content.getAttributes());
            } else {
                contentAttributes = "";
            }
            onLocationUpdate();
        }
    }
}
