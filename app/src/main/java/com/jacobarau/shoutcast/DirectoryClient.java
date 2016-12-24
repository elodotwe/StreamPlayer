package com.jacobarau.shoutcast;

import android.util.Log;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class DirectoryClient {
    private static final String TAG = "DirectoryClient";

    //===========================================
    //WHY AM I MISSING THIS "DeveloperKey" CLASS?
    //===========================================
    //To develop on this project, you need a developer key for the SHOUTcast API.
    //Go request one at https://www.shoutcast.com/Developer.
    //Once you've received it, create a class called DeveloperKey like so:
    /*
    package com.jacobarau.shoutcast;

    public final class DeveloperKey {
        public static final String DEVELOPER_KEY = "{your_developer_key}";
    }
    */
    //This key is trivially extracted from existing SHOUTcast binaries, but if you are savvy enough
    //to know how to do this, you are probably also smart enough to know why you shouldn't.

    /**
     * Query stations matching any single parameter or a combination of parameters.
     * Blocks until success or failure of retrieval.
     *
     * @param keywords  If not null, the keyword(s) to search for
     * @param genre     If not null, the genre (gotten from the genre API) to filter by
     * @param limit     If not null, result set will contain only this many stations
     * @param bitrate   If not null, result set will contain stations with only this bitrate
     * @param mediaType If not null, result set will contain stations with only this media type
     */
    public List<Station> queryStations(String keywords, Genre genre, Integer limit, Integer bitrate, String mediaType) throws Exception {
        String url;
        //TODO: build URI properly
        boolean notAllNull = false;
        //NOTE: Error on the line below? See note at top of this class!
        url = "http://api.shoutcast.com/station/advancedsearch?k=" + DeveloperKey.DEVELOPER_KEY;
        url += "&f=json";
        if (keywords != null) {
            url += "&search=" + keywords;
            notAllNull = true;
        }
        if (genre != null) {
            url += "&genre_id=" + genre.getId();
            notAllNull = true;
        }
        if (limit != null) {
            url += "&limit=" + limit;
            notAllNull = true;
        }
        if (bitrate != null) {
            url += "&br=" + bitrate;
            notAllNull = true;
        }
        if (mediaType != null) {
            url += "&mt=" + mediaType;
            notAllNull = true;
        }
        if (notAllNull) {
            throw new IllegalArgumentException("At least one parameter must be non-null");
        }

        URL req = new URL(url);
        JSONObject response = getJSON(req);
        Log.i(TAG, "onResponse; got JSONObject " + response.toString(4));
        if (response.getJSONObject("response").getInt("statusCode") != 200) {
            Log.e(TAG, "response.statusCode != 200 trying to get genre list");
            throw new Exception("response.statusCode not 200");
        }

        JSONArray stations = response.getJSONObject("response").getJSONObject("data").getJSONObject("stationList").getJSONArray("station");
        LinkedList<Station> ret = new LinkedList<>();
        for (int i = 0; i < stations.length(); i++) {
            JSONObject station = stations.getJSONObject(i);
            int id = station.getInt("id");
            String genreStr = station.getString("genre");
            String mediaTypeStr = station.getString("mt");
            String nameStr = station.getString("name");
            int listenerCount = station.getInt("lc");
            int maxListeners = station.getInt("ml");
            int bitRate = station.getInt("br");
            String currentTrack = station.getString("ct");
            Station st = new Station(nameStr, mediaTypeStr, id, bitRate, genreStr, currentTrack, listenerCount, maxListeners);
            ret.add(st);
        }

        return ret;
    }

    private String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private JSONObject getJSON(URL url) throws Exception {
        URLConnection conn = url.openConnection();
        String responseStr = convertStreamToString(conn.getInputStream());
        JSONObject response = new JSONObject(responseStr);
        return response;
    }

    /**
     * Start a synchronous (blocking!) query to get the complete tree of Genres.
     *
     * The List returned contains all top-level Genres. Calling its getChildren() function will
     * return its list of children; if this is empty, the Genre has no children.
     */
    public List<Genre> queryGenres() throws Exception {
        String url;
        //TODO: build URI properly
        url = "http://api.shoutcast.com/genre/secondary?parentid=" + 0 + "&k=" + DeveloperKey.DEVELOPER_KEY + "&f=json";
        URL req = new URL(url);
        JSONObject response = getJSON(req);
        Log.i(TAG, "onResponse; got JSONObject " + response.toString(4));
        if (response.getJSONObject("response").getInt("statusCode") != 200) {
            Log.e(TAG, "response.statusCode != 200 trying to get genre list");
            throw new Exception("response.statusCode not 200");
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

        return rootGenre.getChildren();
    }
}
