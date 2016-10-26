package com.jacobarau.shoutcast;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

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
    public void queryGenres(final IGenreListQueryListener listener, Genre parent) {
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
                XmlPullParserFactory factory = null;
                try {
                    factory = XmlPullParserFactory.newInstance();
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                }
                factory.setNamespaceAware(true);
                ArrayList<Genre> genres = new ArrayList<Genre>();
                try {
                    XmlPullParser xpp = factory.newPullParser();
                    xpp.setInput( new StringReader( resultBody ) );
                    int eventType = xpp.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        if(eventType == XmlPullParser.START_DOCUMENT) {
                            System.out.println("Start document");
                        } else if(eventType == XmlPullParser.START_TAG) {
                            System.out.println("Start tag "+xpp.getName());
                            if (xpp.getName().equals("genre")) {
                                Log.i(TAG, "name = " + (String)xpp.getAttributeValue(null,"name"));
                                Log.i(TAG, "id = " + (String)xpp.getAttributeValue(null,"id"));
                                Log.i(TAG, "parentid = " + (String)xpp.getAttributeValue(null,"parentid"));
                                Log.i(TAG, "haschildren = " + xpp.getAttributeValue(null,"haschildren"));
                                Genre g = new Genre((String)xpp.getAttributeValue(null,"name"), Integer.decode((String)xpp.getAttributeValue(null,"id")), Integer.decode((String)xpp.getAttributeValue(null,"parentid")), xpp.getAttributeValue(null,"haschildren").equals("true"));
                                genres.add(g);
                            }
                        } else if(eventType == XmlPullParser.END_TAG) {
                            System.out.println("End tag "+xpp.getName());
                        } else if(eventType == XmlPullParser.TEXT) {
                            System.out.println("Text "+xpp.getText());
                        }
                        eventType = xpp.next();
                    }
                    System.out.println("End document");

                    Genre[] genresArray = new Genre[genres.size()];
                    genresArray = genres.toArray(genresArray);
                    listener.onResultReturned(genresArray);
                } catch (XmlPullParserException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}
