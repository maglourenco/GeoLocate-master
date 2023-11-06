package com.example.elevation;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    // Default to 2 meters
    private int user_height = 2;

    // ToDo - see if I don't need to define a Result for the Activity with the user's height in the bundle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from the XML resource
        addPreferencesFromResource(R.xml.preferences);

        // Get the seek bar preference and its initial value
        // Set the summary to include the initial value
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int sliderValue = sharedPreferences.getInt("user_height", 2);
        Preference userHeightPreference = findPreference("user_height");
        userHeightPreference.setSummary(String.valueOf(sliderValue));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // Handle the preference changes
        if (key.equals("user_height")) {
            // Update the UI based on the new preference value
            user_height = sharedPreferences.getInt(key, 2);
            Preference userHeightPreference = findPreference(key);
            userHeightPreference.setSummary(String.valueOf(user_height));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
