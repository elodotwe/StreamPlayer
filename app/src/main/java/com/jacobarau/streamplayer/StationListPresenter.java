package com.jacobarau.streamplayer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.jacobarau.streamplayer.sdl.LockScreenActivity;
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

    LockScreenStatusReceiver lockScreenReceiver;

    class MetadataChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "metadata refresh intent caught");
            //TODO: display on activity...
        }
    }

    class LockScreenStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SharedIntents.INTENT_LOCK_SCREEN_REQUIRED)) {
                Log.i(TAG, "onReceive: lock required, transitioning to lock screen activity");
                activity.getApplicationContext().unregisterReceiver(lockScreenReceiver);
                lockScreenReceiver = null;

                Intent lockIntent = new Intent(activity, LockScreenActivity.class);
                lockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                activity.startActivity(lockIntent);

                activity.finish();
            }
        }
    }

    public StationListPresenter(StationListActivity activity) {
        this.activity = activity;
        prefsManager = new PrefsManager(activity.getApplicationContext());
        updateStationList();

        lockScreenReceiver = new LockScreenStatusReceiver();
        activity.getApplicationContext().registerReceiver(lockScreenReceiver,
                new IntentFilter(SharedIntents.INTENT_LOCK_SCREEN_REQUIRED));

        //If SDL is running, we want to know about it. We don't want there to be a tight dependency
        //between this class and SDL, so we don't bind. However, we do announce that we have started.
        //If SDL wants us locked, we'll respond to the broadcast it sends.
        Intent metaRefresh = new Intent(SharedIntents.INTENT_ACTIVITY_STARTED);
        activity.getApplicationContext().sendBroadcast(metaRefresh);
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
        if (lockScreenReceiver != null) {
            activity.getApplicationContext().unregisterReceiver(lockScreenReceiver);
        }

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
