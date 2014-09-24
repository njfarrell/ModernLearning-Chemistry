/**
 * Copyright 2013 MobileSmith, Inc. All Rights Reserved.
 */
package com.njfarrell.organicchemistry.app.database.tables;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.util.Xml;

import com.njfarrell.organicchemistry.app.database.FlashCardsDB;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * @author Nate Farrell <nate.farrell@mobilesmith.com>
 *
 * Base table class.
 */
public class TableBase {
    public static final String API_DO = "api_do";
    public static final String API_WITH = "api_with";
    public static final String API_FOR = "api_for";
    public static final String API_TOKEN = "api_token";
    public static final String RESPONSE = "response";
    public static final String TIMESTAMP = "timestamp";

    protected FlashCardsDB mFlashCardDB;
    protected Context mContext;
    protected int mAppId;

    /**
     * Table base constructor.
     *
     * @param context application context.
     */
    public TableBase(Context context) {
        ApplicationInfo appInfo = context.getApplicationInfo();
        PackageManager packageManager = context.getPackageManager();
        if (appInfo != null && packageManager != null) {
            mAppId = Math.abs(packageManager.getApplicationLabel(appInfo).toString().hashCode());
        }

        mFlashCardDB = new FlashCardsDB(context, mAppId);
        mContext = context;
    }

    /**
     * Reset the timestamp column for table.
     *
     * @param tableName name of the table to reset it for.
     */
    protected void resetTimestampColumn(String tableName) {
        String sql = String.format("update %s set %s = '%d'", tableName, TIMESTAMP, 0);

        SQLiteDatabase db = mFlashCardDB.getWritableDatabase();
        if (db != null) {
            db.execSQL(sql);
            db.close();
        }
    }
}
