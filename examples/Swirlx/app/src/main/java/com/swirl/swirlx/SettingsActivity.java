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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import com.swirl.Settings;
import com.swirl.Test;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private static Map<String, ?> userInfoKeys;

    private Map<String, ?> original;

    private static void resetDefaults(SharedPreferences readonlyPrefs) {
        SharedPreferences.Editor prefs = readonlyPrefs.edit();
        prefs.putString(Settings.API_HOST, "live");
        prefs.putString(Settings.CONTENT_CODE, "");
        prefs.putString(Settings.BEACON_FILTER, "");
        prefs.putInt(Settings.BEACON_SCAN_MODE, Settings.SCAN_MODE_ALWAYS);
        prefs.putInt(Settings.ANDROID_MAX_FILTERS, Integer.MAX_VALUE);
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
                    if (entry.getKey().equals(Settings.BEACON_SCAN_MODE)) {
                        ((Spinner) v).setSelection(positionForScanMode((Integer) entry.getValue()));
                    } else if (entry.getKey().equals(Settings.ANDROID_MAX_FILTERS)) {
                        ((Spinner) v).setSelection(positionForMaxFilters((Integer) entry.getValue()));
                    }
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
                    if (entry.getKey().equals(Settings.BEACON_SCAN_MODE)) {
                        editor.putInt(entry.getKey(), scanModeForPosition(((Spinner) v).getSelectedItemPosition()));
                    } else if (entry.getKey().equals(Settings.ANDROID_MAX_FILTERS)) {
                        editor.putInt(entry.getKey(), maxFiltersForPosition(((Spinner) v).getSelectedItemPosition()));
                    }
                } else if (entry.getValue() instanceof Boolean) {
                    editor.putBoolean(entry.getKey(), ((Switch) v).isChecked());
                }
            }
        }
        editor.commit();

        MainActivity.settingsChanged = !prefs.getAll().equals(original);
    }

    /*
        ANDROID_MAX_FILTERS

        Spinner     Value       Meaning
        ----------------------------------------
        0           0           No filters
        1           5           Max of 5 filters
        2           MAX INT     No limit
     */

    private int positionForMaxFilters(Integer maxFilters) {
        if (maxFilters == null || maxFilters > 5) {
            return 2;
        } else if (maxFilters > 0) {
            return 1;
        }
        return 0;
    }

    private int maxFiltersForPosition(int position) {
        switch (position) {
            default:
            case 0: return 0;
            case 1: return 5;
            case 2: return Integer.MAX_VALUE;
        }
    }

    private int positionForScanMode(Integer scanMode) {
        return (scanMode - Settings.SCAN_MODE_INUSE);
    }

    private int scanModeForPosition(int position) {
        return (position + Settings.SCAN_MODE_INUSE);
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

        Spinner maxFilters = (Spinner)findViewById(R.id.android_max_filters);
        List<String> maxFiltersArray = new ArrayList<>(5);
        maxFiltersArray.add("No Filters");
        maxFiltersArray.add("5 Filters Max");
        maxFiltersArray.add("No Limit");
        ArrayAdapter<String> maxFiltersAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, maxFiltersArray);
        maxFiltersAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        maxFilters.setAdapter(maxFiltersAdapter);

        Spinner scanMode = (Spinner)findViewById(R.id.beacon_scan_mode);
        List<String> scanModeArray = new ArrayList<>(5);
        scanModeArray.add("In Use");
        scanModeArray.add("Region");
        scanModeArray.add("Always");
        ArrayAdapter<String> scanModeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, scanModeArray);
        scanModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scanMode.setAdapter(scanModeAdapter);

        loadPreferences();

        Button bClearLocks = (Button) findViewById(R.id.clear_location_locks);
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
