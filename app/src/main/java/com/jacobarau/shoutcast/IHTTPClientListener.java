package com.jacobarau.shoutcast;

/**
 * Created by jacob on 10/25/16.
 */
public interface IHTTPClientListener {
    void onError(String message);

    void onResult(String resultBody);
}
