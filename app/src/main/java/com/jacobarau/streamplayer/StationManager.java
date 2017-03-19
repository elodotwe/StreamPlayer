package com.jacobarau.streamplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jacob on 3/18/17.
 */

public class StationManager {
    Context context;
    final String TAG = "StationManager";

    private List<StationPreset> stations = null;

    public StationManager(Context context) {
        this.context = context;
        stations = readStations();
    }

    private List<StationPreset> readStations() {
        ArrayList<StationPreset> stations = new ArrayList<>();

        SharedPreferences sharedPreferences = context.getSharedPreferences("StationManager", Context.MODE_PRIVATE);
        String stationList = sharedPreferences.getString("stationList", "[]");
        try {
            JSONArray stationListJSON = new JSONArray(stationList);
            for (int i = 0; i < stationListJSON.length(); i++) {
                JSONObject obj = stationListJSON.getJSONObject(i);
                stations.add(new StationPreset(obj.getString("name"), obj.getString("url")));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Station list gotten from shared preferences not parseable, replacing with empty list");
            writeStations(new ArrayList<StationPreset>());
        }
        return stations;
    }

    private void writeStations(List<StationPreset> stations) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("StationManager", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        JSONArray stationsToSave = new JSONArray();
        for (StationPreset sp : stations) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("name", sp.name);
                obj.put("url", sp.url);
            } catch (JSONException e) {
                Log.e(TAG, "JSONException thrown putting name into station list??", e);
            }
            stationsToSave.put(obj);
        }
        editor.putString("stationList", stationsToSave.toString());
        editor.apply();
    }

    public List<StationPreset> getStations() {
        return stations;
    }

    public void addStation(String name, String url) {
        stations.add(new StationPreset(name, url));
        writeStations(stations);
    }

    public void removeStations(List<StationPreset> toRemove) {
        stations.removeAll(toRemove);
        writeStations(stations);
    }
}
