/**
 * Copyright Nate Farrell. All Rights Reserved.
 */
package com.njfarrell.organicchemistry.app.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Nate Farrell <njfarrel@gmail.com>
 *
 * Utility class to handle application functions.
 */
public class AppUtil {

    /**
     * Get unique device info.
     *
     * @param context application context.
     * @return returns a json object of the device, manufacturer, build version, and a unique id.
     */
    public static JSONObject getDeviceInfo(Context context) {
        JSONObject deviceInfo = new JSONObject();
        try {
            deviceInfo.put("device", Build.MODEL);
            deviceInfo.put("manufacturer", Build.MANUFACTURER);
            deviceInfo.put("android", android.os.Build.VERSION.SDK);

            TelephonyManager tm = (TelephonyManager) context.getSystemService(
                    Context.TELEPHONY_SERVICE);

            String tmDevice = tm.getDeviceId();
            String androidId = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            String serial = null;
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO)
                serial = Build.SERIAL;
            if (androidId != null)
                deviceInfo.put("uid", androidId);
            else if (serial != null)
                deviceInfo.put("uid", serial);
            else if (tmDevice != null)
                deviceInfo.put("uid", tmDevice);
            deviceInfo.put("carrier", tm.getNetworkOperatorName());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return deviceInfo;
    }

    /**
     * Check if device currently is online.
     *
     * @param context application context.
     * @return true if device is online.
     */
    public static boolean isOnline(Context context) {
        final NetworkInfo networkInfo = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    /**
     * Check if a string is a valid string, not null and has a length greater than 0.
     *
     * @param testString string to check.
     * @return true if valid string.
     */
    public static boolean isValidString(String testString) {
        return testString != null && testString.length() > 0;
    }
}
