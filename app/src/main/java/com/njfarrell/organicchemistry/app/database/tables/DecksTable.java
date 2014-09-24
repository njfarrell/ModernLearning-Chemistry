/**
 * Copyright 2013 MobileSmith, Inc. All Rights Reserved.
 */
package com.njfarrell.organicchemistry.app.database.tables;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import com.njfarrell.organicchemistry.app.preferences.AppPreferences;

/**
 * @author Nate Farrell <nate.farrell@mobilesmith.com>
 *
 * Decks table that stores the list of decks as well as the deck groups.
 */
public class DecksTable extends TableBase {

    public static final String TABLE_NAME = "decks_table_%d";
    public static final String COL_ID = "id";
    public static final int ID_COLUMN_DECK_SET = 1;
    public static final int ID_COLUMN_DECK_LIST = 2;

    private String mTableName;

    public static String getTableName(int appId) {
        return String.format(TABLE_NAME, appId);
    }

    public DecksTable(Context context) {
        super(context);

        mTableName = String.format(TABLE_NAME, mAppId);
    }

    /**
     * Create table if it doesnt exist.
     *
     * @param db SQLite database.
     */
    public void createTableIfNotExists(SQLiteDatabase db) {
        String sql = String.format(
                "CREATE TABLE IF NOT EXISTS %s ("
                        + "%s INTEGER PRIMARY KEY, "
                        + "%s TEXT, %s TEXT, %s TEXT, "
                        + "%s TEXT, %s TEXT, %s NUMERIC);", mTableName, COL_ID,
                API_DO, API_WITH, API_FOR, API_TOKEN, RESPONSE, TIMESTAMP);
        db.execSQL(sql);
    }

    /**
     * Replace row in table.
     *
     * @param values values to replace row with.
     * @return true if successfully replaced row.
     */
    public boolean replaceRow(ContentValues values) {
        return mFlashCardDB.replaceRow(mTableName, values) >= 0;
    }

    /**
     * Query table based on the api action.
     * @param api_do api action.
     * @return response string from the server that is stored in the data table.
     */
    public String queryTableByAPI(String api_do) {
        String result = null;
        String sql = String.format("select * from %s where %s = '%s'" , mTableName, API_DO,
                api_do);

        SQLiteDatabase db = mFlashCardDB.getReadableDatabase();
        Cursor c;
        if (db != null) {
            try {
                c = db.rawQuery(sql, null);
            } catch (SQLiteException e) {
                c = null;
                db.close();
            }

            if (c != null && c.moveToFirst()) {
                ContentValues values = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(c, values);
                if (values.size() > 0) {
                    AppPreferences prefs = AppPreferences.newInstance(mContext);

                    if (values.containsKey(TIMESTAMP)) {
                        Long timestamp = values.getAsLong(TIMESTAMP);
                        if (timestamp != null) {
                            long updateInterval = prefs.getLongValue(AppPreferences.KEY_UPDATE_INTERVAL,
                                    172800000) + timestamp;
                            long currentTime = System.currentTimeMillis();
                            if (updateInterval > currentTime) {
                                result = values.getAsString(RESPONSE);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Reset the timestamp column to 0
     */
    public void resetTimestamp() {
        resetTimestampColumn(TABLE_NAME);
    }
}
