package com.jacobarau.streamplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.Ostermiller.util.CircularByteBuffer;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.decoder.DecoderCounters;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StreamingService extends Service implements ExoPlayer.EventListener, AudioRendererEventListener {
    final String TAG = "StreamingService";
    private static final String ACTION_START = "com.jacobarau.streamplayer.action.START";
    private static final String ACTION_STOP = "com.jacobarau.streamplayer.action.STOP";


    SimpleExoPlayer player = null;
    public static boolean isStreaming = false;

    DecoderCounters cntr = null;

    private final Object streamTitleLock = new Object();
    private String streamTitle = null;

    public String getStreamTitle() {
        synchronized (streamTitleLock) {
            return streamTitle;
        }
    }

    public static void startPlaying(Context ctx, String url) {
        isStreaming = true;
        Intent intent = new Intent(ctx, StreamingService.class);
        intent.setAction(StreamingService.ACTION_START);
        intent.putExtra("url", url);
        ctx.startService(intent);
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
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (intent == null) {
            Log.i(TAG, "onStartCommand: intent was null, disregarding");
            stopSelf();
            return START_NOT_STICKY;
        }

        if (intent.getAction().equals(ACTION_START)) {
            Log.i(TAG, "START received");
            Notification not = new Notification.Builder(this).setContentTitle("Stream Player")
                    .setContentText("Playing streaming media")
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .build();
            mgr.notify(1, not);

            //It is possible that we received two START commands in a row for different URLs.
            //In this case, only the most recent command should take effect.
            //If we have a live player instance, tear it down completely.
            if (player != null) {
                player.setPlayWhenReady(false);
                player.stop();
                //This will block until the player is dead.
                player.release();
                player = null;
            }

            Handler mainHandler = new Handler();
            TrackSelector trackSelector = new DefaultTrackSelector();

            //TODO: let the user decide parameters for this (buffer sizes etc)
            LoadControl loadControl = new DefaultLoadControl();

            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
            player.setAudioDebugListener(this);

            DataSource.Factory mediaDataSourceFactory = new IcyDataSourceFactory();

            MediaSource ms = new ExtractorMediaSource(Uri.parse(intent.getStringExtra("url")), mediaDataSourceFactory, new DefaultExtractorsFactory(),
                    mainHandler, null);

            player.addListener(this);
            Log.i(TAG, "About to prepare");
            player.prepare(ms);
            Log.i(TAG, "Prepare completed");
        }

        if (intent.getAction().equals(ACTION_STOP)) {
            Log.i(TAG, "STOP received!");
            spinDown();
            return START_NOT_STICKY;
        }

        return START_STICKY;
    }

    private void spinDown() {
        Log.i(TAG, "spinDown: starting spin down");
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (player != null) {
            Log.i(TAG, "spinDown: player is not null. setting play-when-ready to false");
            player.setPlayWhenReady(false);
            Log.i(TAG, "spinDown: Stopping playback");
            player.stop();
            Log.i(TAG, "spinDown: Releasing player");
            player.release();
            Log.i(TAG, "spinDown: player released");
            player = null;
        }
        mgr.cancel(1);
        this.stopSelf();
    }

    private class IcyDataSourceFactory implements DataSource.Factory {

        @Override
        public DataSource createDataSource() {
            return new IcyDataSource();
        }
    }

    private class IcyDataSource implements DataSource {
        Uri uri;
        URL url;
        HttpURLConnection conn;
        boolean isIcy;
        int icyDataChunkLength;

        volatile boolean dead = false;
        CircularByteBuffer availableDataBytes;

        @Override
        public long open(DataSpec dataSpec) throws IOException {
            uri = dataSpec.uri;
            url = new URL(uri.toString());
            Log.i(TAG, "open: Trying to open connection to " + url);
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("Icy-MetaData","1");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.connect();
            if (conn.getHeaderField("icy-metaint") != null) {
                icyDataChunkLength = Integer.valueOf(conn.getHeaderField("icy-metaint"));
                Log.d(TAG, "ICY interval is " + icyDataChunkLength);
                isIcy = true;
                availableDataBytes = new CircularByteBuffer();
                new Thread(new ReaderThread()).start();
            } else {
                isIcy = false;
                Log.d(TAG, "Server didn't give us an ICY interval; assuming non-ICY server.");
            }

            return dataSpec.length;
        }

        class ReaderThread implements Runnable {
            private final int WAITING_META_LENGTH = 0;
            private final int WAITING_METADATA = 1;
            private final int WAITING_DATA = 2;

            @Override
            public void run() {
                int decoderState = WAITING_DATA;
                int stateByteCount = 0;

                int metaChunkLength = 0;
                //Lazily allocated to the max size of the metadata chunk
                byte[] metaBuffer = new byte[4096];
                byte[] buf = new byte[1024];
                InputStream inputStream;
                try {
                    inputStream = conn.getInputStream();
                } catch (IOException e) {
                    dead = true;
                    Log.d(TAG, "Spinning down icy reader due to exception" ,e);
                    return;
                }

                readLoop:
                while (!dead) {
                    int bufferLen;
                    int bufferConsumed = 0;
                    try {
                        bufferLen = inputStream.read(buf, 0, 1024);
                    } catch (IOException e) {
                        dead = true;
                        Log.d(TAG, "Spinning down icy reader due to exception" ,e);
                        break;
                    }

                    while (true) {
                        if (bufferConsumed == bufferLen) break;

                        switch (decoderState) {
                            case WAITING_DATA:
                                //Grab either all the data available or the remainder of what we expect,
                                //whichever is smaller.
                                int dataToConsume = Math.min(bufferLen - bufferConsumed, icyDataChunkLength - stateByteCount);
                                try {
                                    availableDataBytes.getOutputStream().write(buf, bufferConsumed, dataToConsume);
                                } catch (IOException e) {
                                    Log.e(TAG, "ReaderThread.run(): WAITING_DATA state unable to write data bytes to the circular byte buffer. Spinning down.", e);
                                    dead = true;
                                    break readLoop;
                                }
                                stateByteCount += dataToConsume;
                                bufferConsumed += dataToConsume;

                                //If we still need bytes for this data chunk (but have exhausted the buffer)
                                if (icyDataChunkLength - stateByteCount == 0) {
                                    decoderState = WAITING_META_LENGTH;
                                }
                                break;
                            case WAITING_META_LENGTH:
                                metaChunkLength = (buf[bufferConsumed] & 0xFF) * 16;
                                bufferConsumed++;
                                if (metaChunkLength != 0) {
                                    decoderState = WAITING_METADATA;
                                } else {
                                    decoderState = WAITING_DATA;
                                }
                                stateByteCount = 0;
                                break;
                            case WAITING_METADATA:
                                int metaToConsume = Math.min(bufferLen - bufferConsumed, metaChunkLength - stateByteCount);
                                System.arraycopy(buf, bufferConsumed, metaBuffer, stateByteCount, metaToConsume);
                                stateByteCount += metaToConsume;
                                bufferConsumed += metaToConsume;

                                if (metaChunkLength - stateByteCount == 0) {
                                    decoderState = WAITING_DATA;
                                    parseMetaBuf(metaBuffer, 0, metaChunkLength);
                                    stateByteCount = 0;
                                }
                        }
                    }
                }
                conn.disconnect();
                try {
                    availableDataBytes.getInputStream().close();
                } catch (IOException e) {
                    Log.e(TAG, "run: Unable to close available data bytes input stream", e);
                }
                Log.d(TAG, "spun down icy reader");
            }

            private void parseMetaBuf(byte[] metaBuffer, int offset, int length) {
                try {
                    String metaResult = new String(metaBuffer, offset, length, "UTF-8").trim();
                    Log.d(TAG,"Meta result is "+ length + " bytes long, \"" + metaResult + "\'");
                    int idx = metaResult.indexOf("StreamTitle='");
                    if (idx == -1) {
                        Log.e(TAG, "StreamTitle not part of the received data, so ignoring the whole meta block");
                    } else {
                        metaResult = metaResult.substring(idx + "StreamTitle='".length());
                        idx = metaResult.indexOf("';");
                        if (idx == -1) {
                            Log.e(TAG, "End of StreamTitle missing. Ignoring the whole meta block.");
                        } else {
                            metaResult = metaResult.substring(0, idx);
                            Log.d(TAG, "meta result is now " + metaResult);
                            synchronized (streamTitleLock) {
                                streamTitle = metaResult;
                            }
                            Intent metaRefresh = new Intent(SharedIntents.INTENT_METADATA_REFRESH);
                            metaRefresh.putExtra("streamTitle", metaResult);
                            StreamingService.this.getApplicationContext().sendBroadcast(metaRefresh);
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    Log.e(TAG, "couldn't convert metadata result to utf8, unsupported encoding exception", e);
                }
            }
        }
        @Override
        public int read(byte[] buffer, int offset, int readLength) throws IOException {
            if (dead) {
                Log.i(TAG, "read: Dead, so throwing exception");
                throw new IOException("Read thread died for some reason (see logcat)");
            }

            if (isIcy) {
                return availableDataBytes.getInputStream().read(buffer, offset, readLength);
            } else {
                return conn.getInputStream().read(buffer, offset, readLength);
            }
        }

        @Override
        public Uri getUri() {
            return uri;
        }

        @Override
        public void close() throws IOException {
            conn.getInputStream().close();
            conn.getOutputStream().close();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "destrooooooy :((");
    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        Log.i(TAG, "Player onLoadingChanged " + isLoading);
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        Log.i(TAG, "Player onPlayerStateChanged playWhenReady " + playWhenReady + ", playbackState " + playbackState);
        if (!playWhenReady && (playbackState == PlaybackState.STATE_PLAYING)) {
            /*
            Why sleep for 500msec here? Doesn't the user want to hear the music NOW, not in .5sec?
            Well, let me tell you a story about ExoPlayer and I really not getting along.
            For whatever reason, there's some fucking bullshit going on wherein I initialize and
            start the player with no sleep whatsoever, and as a result my app shows up on SYNC,
            the logcat lines all show up like the player THINKS it is playing (the input and
            rendered buffers both accumulate) but no sound actually comes out! What the hell, says
            I, as I tweak 27 different things and query Google for everything I can think of. I
            want to call it a bug in ExoPlayer, but I'm not sure that's fair. Maybe there's
            something about the switch from internal audio to Bluetooth? Fuck if I know, but I'm
            done trying to figure it out for now. If we sleep for .5sec here, it seems to get us
            out of this shitty race condition and into worky worky.

            As an aside, SDL resumption was breaking 95% of the time before I made this change,
            but now it has worked 3/3 attempts in the last 5 minutes. No clue why.

            Oops, 3/4 now. Guess resumption is hard.
             */
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) { }
            player.setPlayWhenReady(true);
        }
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest) {
        Log.i(TAG, "Player onTimelineChanged timeline " + timeline + ", manifest " + manifest);
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.e(TAG, "Player onPlayerError " + error, error);
        spinDown();
        //TODO: articulate this error to our UI (SDL or otherwise)...retry strategy?
    }

    @Override
    public void onPositionDiscontinuity() {
        Log.i(TAG, "Player onPositionDiscontinuity");
    }

    boolean audioEnabled = false;

    @Override
    public void onAudioEnabled(DecoderCounters counters) {
        Log.i(TAG, "Player onAudioEnabled, " + counters);

        audioEnabled = true;

        final ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
        this.cntr = counters;
        exec.schedule(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Player " + decoderCountToString(cntr));
                if (audioEnabled) {
                    exec.schedule(this, 1, TimeUnit.SECONDS);
                }
            }
        }, 1, TimeUnit.SECONDS);
    }

    public String decoderCountToString(DecoderCounters counters) {
        String str = "";
        str += "decoderInitCount " + String.valueOf(counters.decoderInitCount) + ", ";
        str += "decoderReleaseCount " + counters.decoderReleaseCount + ", ";
        str += "droppedOutputBuffer " + counters.droppedOutputBufferCount + ", ";
        str += "inputBufferCount " + counters.inputBufferCount + ", ";
        str += "maxConsecutiveDroppedOutputBufferCount " + counters.maxConsecutiveDroppedOutputBufferCount + ", ";
        str += "renderedOutputBufferCount " + counters.renderedOutputBufferCount + ", ";
        str += "skippedOutputBufferCount " + counters.skippedOutputBufferCount;

        return str;
    }

    @Override
    public void onAudioSessionId(int audioSessionId) {
        Log.i(TAG, "Player onAudioSessionId: " + audioSessionId);
    }

    @Override
    public void onAudioDecoderInitialized(String decoderName, long initializedTimestampMs, long initializationDurationMs) {
        Log.i(TAG, "Player onAudioDecoderInitialized, decoderName " + decoderName + ", initializedTimestampMs " + initializedTimestampMs + ", initializationDurationMs " + initializationDurationMs);
    }

    @Override
    public void onAudioInputFormatChanged(Format format) {
        Log.i(TAG, "Player onAudioInputFormatChanged " + format);
    }

    @Override
    public void onAudioTrackUnderrun(int bufferSize, long bufferSizeMs, long elapsedSinceLastFeedMs) {
        Log.i(TAG, "Player onAudioTrackUnderrun bufferSize " + bufferSize + ", bufferSizeMs " + bufferSizeMs + ", elapsedSinceLastFeedMs " + elapsedSinceLastFeedMs);
    }

    @Override
    public void onAudioDisabled(DecoderCounters counters) {
        Log.i(TAG, "Player onAudioDisabled " + counters);
        audioEnabled = false;
    }
}
