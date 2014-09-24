/**
 * Copyright Nate Farrell. All Rights Reserved.
 */
package com.njfarrell.organicchemistry.app.utilities;

import android.content.Context;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Nate Farrell <njfarrel@gmail.com>
 *
 * Utility class to handle the http requests.
 */
public class HttpUtil {
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 10000;
    private static final String POST_REQUEST = "POST";

    /**
     * Get http url connection.
     *
     * @param context application context.
     * @param url url string.
     * @param api_do api do parameter.
     * @param api_with api with parameter.
     * @param api_for api for parameter.
     * @param api_token api token parameter.
     * @return the url connection.
     * @throws IOException
     */
    public static HttpURLConnection getUrlConnection(Context context, String url, String api_do,
                                                     String api_with, String api_for,
                                                     String api_token) throws IOException {
        HttpURLConnection urlConnection = null;
        if (AppUtil.isOnline(context)) {
            urlConnection = (HttpURLConnection) new URL(url)
                    .openConnection();
            urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
            urlConnection.setReadTimeout(READ_TIMEOUT);
            urlConnection.setRequestMethod(POST_REQUEST);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            if (AppUtil.isValidString(api_do)) {
                params.add(new BasicNameValuePair("do", api_do));
            }
            if (AppUtil.isValidString(api_with)) {
                params.add(new BasicNameValuePair("with", api_with));
            }
            if (AppUtil.isValidString(api_for)) {
                params.add(new BasicNameValuePair("for", api_for));
            }
            if (AppUtil.isValidString(api_token)) {
                params.add(new BasicNameValuePair("token", api_token));
            }
            OutputStream os = urlConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();
            os.close();
        }
        return urlConnection;
    }

    /**
     * Create a string that contains all of the parameters to be sent in a post request.
     *
     * @param params parameters to send to server
     * @return Generated parameter string
     * @throws UnsupportedEncodingException
     */
    private static String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }
}
