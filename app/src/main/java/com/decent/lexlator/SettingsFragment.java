package com.decent.lexlator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.Map;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "SettingsFragment";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);
        Log.d(TAG, "Created preferences...");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()...");

        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        Map<String, ?> preferencesMap = sharedPreferences.getAll();
        String[] updateKeys = {"address_preference", "city_preference", "zip_preference", "district_preference"};
        // iterate through the preference entries and update their summary
        for (String key: updateKeys) {
            updateSummary((EditTextPreference) findPreference(key));
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()...");
        //sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
        Log.d(TAG, "Size: " + getPreferenceManager().getSharedPreferences().getAll().size());
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d(TAG, "onSharedPreferenceChanged()...");
        Preference changed = findPreference(key);

        // and if it's an instance of EditTextPreference class, update its summary
        if (key.equals("address_preference") || key.equals("city_preference")
                || key.equals("zip_preference") || key.equals("district_preference")) {
            assert changed != null;
            updateSummary((EditTextPreference) changed);
        }
    }

    private void updateSummary(EditTextPreference preference) {
        // set the EditTextPreference's summary value to its current text
        if (null == preference) return;
        preference.setSummary(preference.getText());
    }
}
