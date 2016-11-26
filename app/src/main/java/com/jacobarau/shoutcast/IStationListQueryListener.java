package com.jacobarau.shoutcast;

/**
 * Created by jacob on 10/14/16.
 */
public interface IStationListQueryListener {
    public void onError();

    public void onResultReturned(Station stations[]);
}
