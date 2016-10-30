package com.jacobarau.shoutcast;

import android.content.Context;
import android.text.Html;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by jacob on 10/14/16.
 */

public class DirectoryClient {
    private static final String TAG = "DirectoryClient";

    final String DEVELOPER_KEY = "***REMOVED***";

    RequestQueue requestQueue = null;

    public DirectoryClient(Context ctx) {
        requestQueue = Volley.newRequestQueue(ctx);
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
        url = "http://api.shoutcast.com/genre/secondary?parentid=" + id + "&k=" + DEVELOPER_KEY + "&f=json";

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.i(TAG, "onResponse; got JSONObject " + response.toString(4));
                    if (response.getJSONObject("response").getInt("statusCode") != 200) {
                        Log.e(TAG, "response.statusCode != 200 trying to get genre list");
                        listener.onError();
                        return;
                    }

                    class GenreNode {
                        JSONObject children;
                        Genre parentGenre;

                        GenreNode(JSONObject children, Genre parentGenre) {
                            this.children = children;
                            this.parentGenre = parentGenre;
                        }
                    }
                    Queue<GenreNode> nodes = new LinkedList<>();
                    JSONObject baseDataNode = response.getJSONObject("response").getJSONObject("data");
                    Genre rootGenre = new Genre("(imaginary root genre)", 0, null);
                    nodes.add(new GenreNode(baseDataNode, rootGenre));
                    GenreNode node = nodes.poll();
                    while (node != null) {
                        Log.i(TAG, "Processing node with parent named " + node.parentGenre.getName());
                        //We know this node has children. Enumerate them, create Genre objects for each,
                        //and if they have children, create GenreNodes for them so they can be explored
                        //too.

                        JSONArray genreList = node.children.getJSONObject("genrelist").getJSONArray("genre");
                        for (int i = 0; i < genreList.length(); i++) {
                            JSONObject genre = genreList.getJSONObject(i);
                            String name = StringEscapeUtils.unescapeHtml4(genre.getString("name"));
                            int id = genre.getInt("id");
                            Genre child = new Genre(name, id, node.parentGenre);
                            Log.i(TAG, "A child is named " + name + " with ID " + id);
                            node.parentGenre.addChild(child);

                            if (genre.getBoolean("haschildren")) {
                                Log.i(TAG, "Child has children. Adding node to visit later.");
                                nodes.add(new GenreNode(genre, child));
                            }
                        }

                        node = nodes.poll();
                    }

                    Genre[] ret = new Genre[rootGenre.getChildren().size()];
                    ret = rootGenre.getChildren().toArray(ret);
                    listener.onResultReturned(ret);
                } catch (JSONException e) {
                    Log.e(TAG, "JSON exception trying to get genre list.", e);
                    listener.onError();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Volley Error returned..." + error);
                listener.onError();
            }
        });

        requestQueue.add(req);
    }
}
