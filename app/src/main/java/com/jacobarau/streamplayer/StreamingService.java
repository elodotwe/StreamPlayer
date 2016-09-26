package com.jacobarau.streamplayer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class StreamingService extends Service implements MediaPlayer.OnPreparedListener {
    final String TAG = "StreamingService";
    public static final String ACTION_START = "com.jacobarau.streamplayer.action.START";
    public static final String ACTION_STOP = "com.jacobarau.streamplayer.action.STOP";

    MediaPlayer mMediaPlayer = null;

    public StreamingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() == ACTION_START) {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    Log.i(TAG, "About to set data source");
                    mMediaPlayer.setDataSource(intent.getStringExtra("url"));
                    Log.i(TAG, "Data source set. About to prepare");
                    mMediaPlayer.setOnPreparedListener(this);
                    mMediaPlayer.prepareAsync();
                    Log.i(TAG, "Prepared");
                } catch (IOException e) {
                    Log.e(TAG, "Unable to set data source, it failed");
                    e.printStackTrace();
                    return 0;
                }
            }
        }

        if (intent.getAction() == ACTION_STOP) {
            if (mMediaPlayer != null) {
                mMediaPlayer.stop();
                mMediaPlayer = null;
            }
            this.stopSelf();
        }



        return START_STICKY;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.i(TAG, "actually prepared, starting");
        mMediaPlayer.start();
    }
}
