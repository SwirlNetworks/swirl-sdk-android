/*
 * Swirlx
 * SettingsActivity
 *
 * Copyright (C) 2015-2016 Swirl Networks, Inc.
 */

package com.swirl.swirlx;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import com.swirl.Settings;
import com.swirl.Test;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private static Map<String, ?> userInfoKeys;

    private Map<String, ?> original;

    private static void resetDefaults(SharedPreferences readonlyPrefs) {
        SharedPreferences.Editor prefs = readonlyPrefs.edit();
        prefs.putString(Settings.API_HOST, "live");
        prefs.putString(Settings.CONTENT_CODE, "");
        prefs.putString(Settings.BEACON_FILTER, "");
        prefs.putString("field_1", "");
        prefs.putString("field_2", "");
        prefs.putInt(Settings.LOCATION_HISTORY_MAX, 32);
        prefs.commit();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences("Swirlx", MODE_PRIVATE);

        if (prefs.getAll().isEmpty())
            resetDefaults(prefs);

        original = prefs.getAll();

        for (Map.Entry<String, ?> entry : original.entrySet()) {
            View v = findViewById(getResources().getIdentifier(entry.getKey(), "id", getPackageName()));
            if (v != null) {
                if (entry.getValue() instanceof String) {
                    ((EditText) v).setText((String) entry.getValue());
                } else if (entry.getValue() instanceof Integer) {
                    ((Spinner) v).setSelection((Integer) entry.getValue());
                } else if (entry.getValue() instanceof Boolean) {
                    ((Switch) v).setChecked((Boolean) entry.getValue());
                }
            }
        }
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences("Swirlx", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            View v = findViewById(getResources().getIdentifier(entry.getKey(), "id", getPackageName()));
            if (v != null) {
                if (entry.getValue() instanceof String) {
                    editor.putString(entry.getKey(), ((EditText) v).getText().toString());
                } else if (entry.getValue() instanceof Integer) {
                    editor.putInt(entry.getKey(), ((Spinner) v).getSelectedItemPosition());
                } else if (entry.getValue() instanceof Boolean) {
                    editor.putBoolean(entry.getKey(), ((Switch) v).isChecked());
                }
            }
        }
        editor.commit();

        MainActivity.settingsChanged = !prefs.getAll().equals(original);
    }

    public static Bundle options(SharedPreferences prefs) {
        if (prefs.getAll().isEmpty())
            resetDefaults(prefs);

        Bundle options = new Bundle();

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getValue() instanceof String) {
                options.putString(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                options.putInt(entry.getKey(), (Integer) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                options.putBoolean(entry.getKey(), (Boolean) entry.getValue());
            }
        }

        return options;
    }

    public static JSONObject userInfo(SharedPreferences prefs) {
        if (userInfoKeys == null) {
            userInfoKeys = new HashMap<>();
            userInfoKeys.put("field_1", null);
            userInfoKeys.put("field_2", null);
        }

        JSONObject userInfo = new JSONObject();

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (!userInfoKeys.containsKey(entry.getKey())) {
                continue;
            }

            Object value = entry.getValue();
            if (value instanceof String && ((String) value).length() == 0) {
                continue;
            }

            try {
                userInfo.put(entry.getKey(), value);
            } catch (JSONException ex) {
            }
        }

        return userInfo;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        loadPreferences();

        Button bClearLocks = (Button)findViewById(R.id.clear_location_locks);
        bClearLocks.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Test.clearLocationLock(new Test.Completion() {
                    @Override public void complete(boolean success) {
                        clearLocksCompletion(success);
                    }
                });
            }
        });
    }

    private void clearLocksCompletion(final boolean success) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override public void run() {
                if (success) {
                    Toast.makeText(getApplicationContext(), "Successfully cleared Swirl location locks for this device.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Failed to clear Swirl location locks for this device. Please check the Internet connection and try again.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePreferences();
    }
}
