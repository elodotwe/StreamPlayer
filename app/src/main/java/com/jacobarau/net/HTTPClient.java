package com.jacobarau.net;

import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

public class HTTPClient {
    private final String TAG = "HTTPClient";

    private String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    /**
     * Fetches the resource given by the URL, converts response bytes to String (assumes UTF-8).
     * Returns null on error.
     *
     * @param url URL to fetch
     * @return response from the given request, or null if an error occurred
     */
    public String fetchURL(URL url) {
        URLConnection conn;

        try {
            conn = url.openConnection();
        } catch (IOException e) {
            Log.e(TAG, "IOException trying to fetch URL " + url, e);
            return null;
        }

        String responseStr;
        try {
            responseStr = convertStreamToString(conn.getInputStream());
        } catch (IOException e) {
            Log.e(TAG, "IOException trying to fetch URL " + url, e);
            return null;
        }

        return responseStr;
    }
}
