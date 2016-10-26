package com.jacobarau.shoutcast;

/**
 * Created by jacob on 10/22/16.
 */
public interface IHTTPClient {
    public void httpGet(String url, IHTTPClientListener listener);
}
