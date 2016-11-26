package com.jacobarau.streamplayer;

import com.jacobarau.shoutcast.IHTTPClient;
import com.jacobarau.shoutcast.IHTTPClientListener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

/**
 * Created by jacob on 10/25/16.
 */
public class HTTPClient implements IHTTPClient {
    @Override
    public void httpGet(final String url, final IHTTPClientListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                URLConnection connection = null;
                try {
                    connection = new URL(url).openConnection();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                try {
                    InputStream response = connection.getInputStream();
                    ArrayList<Byte> responseList = new ArrayList<>();
                    int b = response.read();
                    while (b != -1) {
                        responseList.add((byte) b);
                        b = response.read();
                    }
                    byte[] respBytes = new byte[responseList.size()];
                    for (int i = 0; i < responseList.size(); i++) {
                        respBytes[i] = responseList.get(i);
                    }

                    String respString = new String(respBytes, "UTF-8");

                    listener.onResult(respString);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
}
