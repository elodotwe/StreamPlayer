package com.jacobarau.streamplayer.sdl;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SdlService extends Service {
    private static final String TAG = "SdlService";
    // variable to contain the current state of the service
    private static SdlService instance = null;

    SdlProxyHost proxyHost = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        proxyHost = new SdlProxyHost(this);

        Log.d(TAG, "oncreate happened");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "start command happened");
        if (intent != null) {
            Log.d(TAG, "so did start proxy");
            if (!proxyHost.startProxy()) {
                Log.e(TAG, "startProxy() failed, so spinning down service");
                stopSelf();
            }
        } else {
            Log.d(TAG, "intent was null, no starty");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        proxyHost.disposeSyncProxy();
        //LockScreenManager.clearLockScreen();
        instance = null;
        super.onDestroy();
    }

    public static SdlService getInstance() {
        return instance;
    }
}
