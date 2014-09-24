/**
 * Copyright Nate Farrell. All Rights Reserved.
 */
package com.njfarrell.organicchemistry.app.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Xml;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Nate Farrell <njfarrel@gmail.com>
 *
 * Specialized shared preferences file for flash cards.
 */
public class AppPreferences {
    public static final String KEY_APP_FIRST_RUN = "app_first_run";
    public static final String KEY_UPDATE_INTERVAL = "app_update_interval";
    public static final String KEY_REVERSE_MODE = "app_reverse_mode";

    private static final String APP_PREFERENCES_NAME = "flash_cards_prefs_%d";

    private static AppPreferences sInstance;

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mEd;

    /**
     * AppPreferences constructor.
     *
     * @param context application context.
     * @param appId unique key for the preference name.
     */
    private AppPreferences(Context context, int appId) {
        mPrefs = context.getSharedPreferences(String.format(APP_PREFERENCES_NAME, appId),
                Context.MODE_PRIVATE);
        mEd = mPrefs.edit();
    }

    /**
     * Singleton pattern.
     *
     * @param context application context.
     * @return instance of the AppPreferences.
     */
    public static AppPreferences newInstance(Context context) {
        String appName = null;
        ApplicationInfo appInfo = context.getApplicationInfo();
        PackageManager packageManager = context.getPackageManager();
        if (appInfo != null && packageManager != null) {
            appName = packageManager.getApplicationLabel(appInfo).toString();
        }

        if (sInstance == null) {
            sInstance = new AppPreferences(context, Math.abs(appName.hashCode()));
        }
        return sInstance;
    }

    /**
     * Store a boolean value into the app shared preferences.
     *
     * @param key key to store under.
     * @param value value to store.
     * @return true if value successfully stored
     */
    public boolean storeBoolean(String key, boolean value) {
        mEd.putBoolean(key, value);
        return mEd.commit();
    }

    /**
     * Store a long value into the app shared preferences.
     *
     * @param key key to store under.
     * @param value value to store.
     * @return true if value successfully stored
     */
    public boolean storeLong(String key, long value) {
        mEd.putLong(key, value);
        return mEd.commit();
    }

    /**
     * Return the boolean value stored in app preferences.
     *
     * @param key key where the value is stored.
     * @param defaultValue default value.
     * @return boolean value
     */
    public boolean getBooleanValue(String key, boolean defaultValue) {
        return mPrefs.getBoolean(key, defaultValue);
    }

    /**
     * Return the long value stored in app preferences.
     *
     * @param key key where the value is stored.
     * @param defaultValue default value.
     * @return long value
     */
    public long getLongValue(String key, long defaultValue) {
        return mPrefs.getLong(key, defaultValue);
    }
}
