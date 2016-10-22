package com.jacobarau.shoutcast;

/**
 * Created by jacob on 10/16/16.
 */
public interface IGenreListQueryListener {
    public void onError();
    public void onResultReturned(Genre genres[]);
}
