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

public class PrefsManager {
    Context context;
    final String TAG = "PrefsManager";

    private List<StationPreset> stations = null;

    public PrefsManager(Context context) {
        this.context = context;
        stations = readStations();
    }

    private List<StationPreset> readStations() {
        ArrayList<StationPreset> stations = new ArrayList<>();

        SharedPreferences sharedPreferences = context.getSharedPreferences("PrefsManager", Context.MODE_PRIVATE);
        String stationList = sharedPreferences.getString("stationList", "[]");
        try {
            JSONArray stationListJSON = new JSONArray(stationList);
            for (int i = 0; i < stationListJSON.length(); i++) {
                stations.add(unmarshalPreset(stationListJSON.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Station list gotten from shared preferences not parseable, replacing with empty list", e);
            writeStations(new ArrayList<StationPreset>());
        }
        return stations;
    }

    private void writeStations(List<StationPreset> stations) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("PrefsManager", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        JSONArray stationsToSave = new JSONArray();
        for (StationPreset sp : stations) {
            stationsToSave.put(marshalPreset(sp));
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

    public void addStation(StationPreset preset) {
        addStation(preset.name, preset.url);
    }

    private StationPreset unmarshalPreset(String presetString) {
        try {
            JSONObject presetJSON = new JSONObject(presetString);
            return unmarshalPreset(presetJSON);
        } catch (JSONException e) {
            return null;
        }
    }

    private StationPreset unmarshalPreset(JSONObject presetJSON) {
        try {
            return new StationPreset(presetJSON.getString("name"), presetJSON.getString("url"));
        } catch (JSONException e) {
            return null;
        }
    }

    private JSONObject marshalPreset(StationPreset preset) {
        JSONObject json = new JSONObject();
        try {
            json.put("name", preset.name);
            json.put("url", preset.url);
        } catch (JSONException e) {
            Log.e(TAG, "marshalPreset: Unable to marshal preset " + preset + ", got exception", e);
            return null;
        }
        return json;
    }

    public StationPreset getLastStation() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("PrefsManager", Context.MODE_PRIVATE);
        String station = sharedPreferences.getString("lastStation", "{}");
        return unmarshalPreset(station);
    }

    public void setLastStation(StationPreset lastStation) {

    }

    public void removeStations(List<StationPreset> toRemove) {
        stations.removeAll(toRemove);
        writeStations(stations);
    }
}
