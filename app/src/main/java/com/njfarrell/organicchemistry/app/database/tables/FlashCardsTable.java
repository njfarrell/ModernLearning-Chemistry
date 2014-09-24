/**
 * Copyright Nate Farrell. All Rights Reserved.
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
 * @author Nate Farrell <njfarrel@gmail.com>
 *
 * Flash cards table that stores the list of flash cards and their deck id.
 */
public class FlashCardsTable extends TableBase{

    public static final String COL_DECK_ID = "deck_id";
    public static final String TABLE_NAME = "flash_cards_table_%d";

    private String mTableName;

    public static String getTableName(int appId) {
        return String.format(TABLE_NAME, appId);
    }

    public FlashCardsTable(Context context) {
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
                        + "%s TEXT, %s TEXT, %s NUMERIC);", mTableName, COL_DECK_ID,
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
     * Query the database for the response json string.
     *
     * @param deckId the deck id to query for (-1 if you want all decks).
     * @return response json string.
     */
    public String queryTableByDeckId(int deckId) {
        String result = null;
        String sql = String.format("select * from %s where %s = '%d'" , mTableName, COL_DECK_ID,
                deckId);

        SQLiteDatabase db = mFlashCardDB.getReadableDatabase();
        if (db != null) {
            Cursor c;
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
