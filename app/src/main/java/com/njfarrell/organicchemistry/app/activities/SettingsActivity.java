package com.njfarrell.organicchemistry.app.activities;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.njfarrell.organicchemistry.app.R;
import com.njfarrell.organicchemistry.app.database.tables.DecksTable;
import com.njfarrell.organicchemistry.app.database.tables.FlashCardsTable;
import com.njfarrell.organicchemistry.app.preferences.AppPreferences;

public class SettingsActivity extends PreferenceActivity{
	//TODO setup a reverse mode preference option
	
	@Override
	public void onCreate(Bundle savedInstanceState) {    
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.settings_preferences);
        final AppPreferences pref = AppPreferences.newInstance(this);
        CheckBoxPreference reversePreference = (CheckBoxPreference) findPreference("reverse_mode");
        ListPreference autoRefreshPreference = (ListPreference) findPreference(
                "autorefresh_frequency");
        Preference refreshPreference = findPreference("refresh_content");

        if (reversePreference != null) {
            reversePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    boolean isChecked = ((CheckBoxPreference) preference).isChecked();
                    pref.storeBoolean(AppPreferences.KEY_REVERSE_MODE, isChecked);
                    return false;
                }
            });
        }

        if (autoRefreshPreference != null) {
            autoRefreshPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    long updateInterval = Long.valueOf((String) newValue);
                    pref.storeLong(AppPreferences.KEY_UPDATE_INTERVAL, updateInterval);
                    return false;
                }
            });
        }

        if (refreshPreference != null) {
            refreshPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    FlashCardsTable flashCardsTable = new FlashCardsTable(SettingsActivity.this);
                    DecksTable decksTable = new DecksTable(SettingsActivity.this);

                    flashCardsTable.resetTimestamp();
                    decksTable.resetTimestamp();
                    return false;

                }
            });
        }
    }
}
