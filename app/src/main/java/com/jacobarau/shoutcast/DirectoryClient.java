package com.jacobarau.shoutcast;

import android.util.Log;

/**
 * Created by jacob on 10/14/16.
 */

public class DirectoryClient {
    private static final String TAG = "DirectoryClient";
    IHTTPClient client;

    final String DEVELOPER_KEY = "***REMOVED***";

    public DirectoryClient(IHTTPClient httpClient) {
        client = httpClient;
    }

    /**
     * Start an asynchronous query to get stations playing a track matching the given nowPlaying
     * string (e.g. find stations playing Weird Al).
     *
     * @param listener object to notify once an error or valid results are available
     * @param nowPlaying Required to be not null; the keyword(s) to search within nowPlaying fields
     *                   of stations; can be artist, song name, or whatever a station might put in
     *                   that field.
     * @param genre If not null, result set will contain only this genre
     * @param limit If not null, result set will contain only this many stations
     * @param pageNumber If not null, limit must not be null as well. Result set will be "limit"
     *                   Stations long, and will be the "pageNumber"th block of "limit" stations.
     *                   Allows for pagination.
     * @param bitrate If not null, result set will contain stations with only this bitrate
     * @param mediaType If not null, result set will contain stations with only this media type
     */
    public void queryStationsByNowPlaying(IStationListQueryListener listener, String nowPlaying, String genre, Integer limit, Integer pageNumber, Integer bitrate, String mediaType) {

    }

    /**
     * Start an asynchronous query to get stations matching any single parameter or a combination
     * of parameters.
     *
     * @param listener object to notify once an error or valid results are available
     * @param keywords If not null, the keyword(s) to search for
     * @param genre If not null, the genre (gotten from the genre API) to filter by
     * @param limit If not null, result set will contain only this many stations
     * @param bitrate If not null, result set will contain stations with only this bitrate
     * @param mediaType If not null, result set will contain stations with only this media type
     */
    public void queryStations(IStationListQueryListener listener, String keywords, Genre genre, Integer limit, Integer bitrate, String mediaType) {

    }

    /**
     * Start an asynchronous query to get random stations(s).
     *
     * @param listener object to notify once an error or valid results are available
     * @param genre If not null, a genre to limit to
     * @param limit If not null, result set will contain only this many stations
     * @param bitrate If not null, result set will contain stations with only this bitrate
     * @param mediaType If not null, result set will contain stations with only this media type
     */
    public void queryRandomStations(IStationListQueryListener listener, Genre genre, Integer limit, Integer bitrate, String mediaType) {

    }

    /**
     * Start an asynchronous query to get a set of genres.
     *
     * If parent is null, all top-level genres are returned.
     * If parent is not null, all genres under the given parent genre are returned.
     *
     * @param listener
     * @param parent If not null, specifies the parent genre to query
     */
    public void queryGenres(IGenreListQueryListener listener, Genre parent) {
        String url;
        int id = 0;
        if (parent != null) {
            id = parent.getId();
        }
        url = "http://api.shoutcast.com/genre/secondary?parentid=" + id + "&k=" + DEVELOPER_KEY + "&f=xml";
        client.httpGet(url, new IHTTPClientListener() {
            @Override
            public void onError(String message) {
                Log.e(TAG, "Error callback from HTTP listener. '" + message + "'");
            }

            @Override
            public void onResult(String resultBody) {
                Log.i(TAG, "onResult: " + resultBody);
            }
        });
    }
}
