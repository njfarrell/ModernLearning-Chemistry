/**
 * Copyright Nate Farrell. All Rights Reserved.
 */
package com.njfarrell.organicchemistry.app.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.njfarrell.organicchemistry.app.activities.DeckActivity;
import com.njfarrell.organicchemistry.app.activities.FlashCardActivity;
import com.njfarrell.organicchemistry.app.utilities.AppUtil;
import com.njfarrell.organicchemistry.app.utilities.HttpUtil;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

/**
 * @author Nate Farrell <njfarrel@gmail.com>
 *
 * Service designed to download content content for the flash cards, the list of decks
 * and deck groups.
 */
public class ContentDownloadService extends IntentService {
    public static final String KEY_RESPONSE_JSON = "response_json";
    public static final String KEY_RESPONSE_CODE = "response_code";

    private static final String KEY_API_DO = "do";
    private static final String KEY_API_WITH = "with";
    private static final String KEY_API_FOR = "for";
    private static final String KEY_API_TOKEN = "token";

    private static final String URL_STRING = "http://modernlearningtools.com/api.php?";

    public ContentDownloadService() {
        super(null);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        String responseString = null;
        int responseCode = -1;
        if (extras != null) {
            String api_do = extras.getString(KEY_API_DO);
            String api_with = extras.getString(KEY_API_WITH);
            String api_for = extras.getString(KEY_API_FOR);
            String api_token = extras.getString(KEY_API_TOKEN);

            try {
                HttpURLConnection connection = HttpUtil.getUrlConnection(this, URL_STRING, api_do,
                        api_with, api_for, api_token);
                if (connection != null) {
                    responseCode = connection.getResponseCode();

                    final BufferedReader br = new BufferedReader(new InputStreamReader(connection
                            .getInputStream()));
                    final StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);
                        sb.append("\n");
                    }
                    br.close();
                    responseString = sb.toString();
                }
            } catch (IOException e) {
                // Dont handle exception
            }

            // Send back broadcast to correct action depending on the api_do
            Intent i = new Intent();
            if(api_do != null && api_do.equals("get_cards")){
                i.setAction(FlashCardActivity.FlashCardResponseReceiver.FLASH_CARD_RESPONSE);
            } else {
                i.setAction(DeckActivity.CardDeckResponseReceiver.CARD_DECK_RESPONSE);
            }
            if (AppUtil.isValidString(responseString)) {
                i.putExtra(KEY_RESPONSE_JSON, responseString);
                i.putExtra(KEY_RESPONSE_CODE, responseCode);
            }

            LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
            localBroadcastManager.sendBroadcast(i);
        }
    }

    public static void startContentDownloadService(Context context, String api_do) {
        startContentDownloadService(context, api_do, null, null, null);
    }

    public static void startContentDownloadService(Context context, String api_do,
                                                   JSONObject api_with) {
        startContentDownloadService(context, api_do, api_with, null, null);
    }

    /**
     * Start content download service.
     *
     * @param context application context.
     * @param api_do api do parameter.
     * @param api_with api with parameter.
     * @param api_for api for parameter.
     * @param api_token api token parameter.
     */
    public static void startContentDownloadService(Context context, String api_do,
                                                   JSONObject api_with, String api_for,
                                                   String api_token) {
        Intent i = new Intent(context, ContentDownloadService.class);
        if (api_do != null) {
            i.putExtra(KEY_API_DO, api_do);
        }
        if (api_with != null) {
            i.putExtra(KEY_API_WITH, api_with.toString());
        }
        if (api_for != null) {
            i.putExtra(KEY_API_FOR, api_for);
        }
        if (api_token != null) {
            i.putExtra(KEY_API_TOKEN, api_token);
        }

        context.startService(i);
    }


}
