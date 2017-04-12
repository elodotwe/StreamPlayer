package com.jacobarau.streamplayer.sdl;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import com.jacobarau.streamplayer.SharedIntents;
import com.jacobarau.streamplayer.StationListActivity;

/**
 * Created by jacob on 4/12/17.
 */

class LockScreenPresenter {
    private Activity activity;
    private final String TAG = "LockScreenPresenter";

    class LockScreenStatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SharedIntents.INTENT_LOCK_SCREEN_NOT_REQUIRED)) {
                Log.i(TAG, "onReceive: lock not required, transitioning to normal activity");
                activity.getApplicationContext().unregisterReceiver(lockScreenReceiver);
                lockScreenReceiver = null;
                //TODO: this might require some subtlety if I add more activities--
                //the user should be returned to where he was, not always the same activity.
                Intent lockIntent = new Intent(activity, StationListActivity.class);
                lockIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
                activity.startActivity(lockIntent);

                activity.finish();
            }
        }
    }

    private LockScreenStatusReceiver lockScreenReceiver;

    LockScreenPresenter(Activity activity) {
        Log.i(TAG, "LockScreenPresenter: constructor called");
        this.activity = activity;
        lockScreenReceiver = new LockScreenStatusReceiver();
        activity.getApplicationContext().registerReceiver(lockScreenReceiver,
                new IntentFilter(SharedIntents.INTENT_LOCK_SCREEN_NOT_REQUIRED));

        Intent metaRefresh = new Intent(SharedIntents.INTENT_ACTIVITY_STARTED);
        activity.getApplicationContext().sendBroadcast(metaRefresh);
    }

    void onStop() {
        Log.i(TAG, "onStop: shutting down lock screen");
        if (lockScreenReceiver != null) {
            activity.getApplicationContext().unregisterReceiver(lockScreenReceiver);
        }
        activity = null;
    }
}
