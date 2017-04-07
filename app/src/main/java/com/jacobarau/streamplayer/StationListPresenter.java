package com.jacobarau.streamplayer;

import android.content.Intent;
import android.util.Log;

import com.jacobarau.streamplayer.sdl.SdlService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jacob on 4/7/17.
 */

public class StationListPresenter {
    StationListActivity activity;
    PrefsManager prefsManager;

    final String TAG = "StationListPresenter";

    public StationListPresenter(StationListActivity activity) {
        this.activity = activity;
        prefsManager = new PrefsManager(activity.getApplicationContext());
        updateStationList();
    }

    void updateStationList() {
        List<StationPreset> presets = prefsManager.getStations();

        ArrayList<String> presetStrings = new ArrayList<>(50);
        for (StationPreset preset : presets) {
            presetStrings.add(preset.name);
        }
        activity.setStationList(presetStrings);
    }

    public void onStop() {
        this.activity = null;
    }

    public void onKickSDL() {
        Log.i(TAG, "onKickSDL: ");
        SdlService serviceInstance = SdlService.getInstance();
        if (serviceInstance == null) {
            Log.i(TAG, "onKickSDL: service was null, starting");
            Intent startIntent = new Intent(activity.getApplicationContext(), SdlService.class);
            Intent bsIntent = new Intent();
            startIntent.putExtras(bsIntent);
            activity.getApplicationContext().startService(startIntent);
        }
    }
}
