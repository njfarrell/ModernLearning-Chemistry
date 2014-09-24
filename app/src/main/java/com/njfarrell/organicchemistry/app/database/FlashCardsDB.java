/**
 * Copyright Nate Farrell. All Rights Reserved.
 */
package com.njfarrell.organicchemistry.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.njfarrell.organicchemistry.app.database.tables.DecksTable;
import com.njfarrell.organicchemistry.app.database.tables.FlashCardsTable;

/**
 * @author Nate Farrell <njfarrel@gmail.com>
 *
 * Flash Card database.
 */
public class FlashCardsDB extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "Flash_Cards_DB_%d";
    private static final int DATABASE_VERSION = 1;

    private Context mContext;
    private int mAppId;

    /**
     * Constructor for the Flash cards database.
     *
     * @param context application context
     * @param appId application name id
     */
    public FlashCardsDB(Context context, int appId) {
        super(context, String.format(DATABASE_NAME, appId), null, DATABASE_VERSION);
        mAppId = appId;
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        DecksTable decksTable = new DecksTable(mContext);
        decksTable.createTableIfNotExists(db);

        FlashCardsTable flashCardsTable = new FlashCardsTable(mContext);
        flashCardsTable.createTableIfNotExists(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        dropTableIfExists(FlashCardsTable.getTableName(mAppId));
        dropTableIfExists(DecksTable.getTableName(mAppId));
    }

    /**
     * Drop table from database if it exists
     *
     * @param table table name to drop
     */
    public void dropTableIfExists(String table){
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            db.execSQL("drop table if exists " + table);
            db.close();
        }
    }

    /**
     * Replace a row in a table in the database
     *
     * @param table table name to replace a row in.
     * @param values values to replace row with.
     * @return position of the row replaced.
     */
    public synchronized long replaceRow(String table, ContentValues values){
        long result = -1;
        SQLiteDatabase db = getWritableDatabase();
        if (db != null) {
            try {
                result = db.replaceOrThrow(table, null, values);
            } catch (SQLiteException e) {
                db.endTransaction();
                db.close();
            }
        }
        return result;
    }
}
