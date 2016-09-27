package com.jacobarau.streamplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;

public class StreamingService extends Service implements MediaPlayer.OnPreparedListener {
    final String TAG = "StreamingService";
    private static final String ACTION_START = "com.jacobarau.streamplayer.action.START";
    private static final String ACTION_STOP = "com.jacobarau.streamplayer.action.STOP";

    MediaPlayer mMediaPlayer = null;
    public static boolean isStreaming = false;

    public static void startPlaying(Context ctx, String url) {
        if (!isStreaming) {
            isStreaming = true;
            Intent intent = new Intent(ctx, StreamingService.class);
            intent.setAction(StreamingService.ACTION_START);
            intent.putExtra("url", url);
            ctx.startService(intent);
        }
    }

    public static void stopPlaying(Context ctx) {
        if (isStreaming) {
            isStreaming = false;
            Intent intent = new Intent(ctx, StreamingService.class);
            intent.setAction(StreamingService.ACTION_STOP);
            ctx.startService(intent);
        }
    }

    public StreamingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand with intent " + intent + ", flags " + flags + ", startId " + startId);
        NotificationManager mgr = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (intent.getAction() == ACTION_START) {
            Notification not = new Notification.Builder(this).setContentTitle("Stream Player").setContentText("Playing streaming media").setOngoing(true).build();
            mgr.notify(1, not);

            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    Log.i(TAG, "About to set data source");
                    mMediaPlayer.setDataSource(intent.getStringExtra("url"));
                    Log.i(TAG, "Data source set. About to prepare");
                    mMediaPlayer.setOnPreparedListener(this);
                    mMediaPlayer.prepareAsync();
                    Log.i(TAG, "Prepare async called");
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
            mgr.cancel(1);
            this.stopSelf();
        }



        return START_STICKY;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        if (mMediaPlayer == null) {
            Log.i(TAG, "mMediaPlayer was null, just bailing out (because probs someone sent ACTION_STOP)");
        } else {
            Log.i(TAG, "actually prepared, starting");
            mMediaPlayer.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destrooooooy :((");
    }
}
