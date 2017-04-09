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
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.jacobarau.streamplayer.sdl.SdlProxyHost;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StreamingService extends Service implements ExoPlayer.EventListener, AudioRendererEventListener {
    final String TAG = "StreamingService";
    private static final String ACTION_START = "com.jacobarau.streamplayer.action.START";
    private static final String ACTION_STOP = "com.jacobarau.streamplayer.action.STOP";


    SimpleExoPlayer player = null;
    public static boolean isStreaming = false;
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    DecoderCounters cntr = null;

    private DataSource.Factory mediaDataSourceFactory;

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


    /**
     * Returns a new DataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new DataSource factory.
     */
    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultDataSourceFactory(this, useBandwidthMeter ? BANDWIDTH_METER : null,
                buildHttpDataSourceFactory(useBandwidthMeter));
    }

    /**
     * Returns a new HttpDataSource factory.
     *
     * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
     *                          DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "ExoPlayerDemo"), useBandwidthMeter ? BANDWIDTH_METER : null);
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

            mediaDataSourceFactory = new IcyDataSourceFactory();

            MediaSource ms = new ExtractorMediaSource(Uri.parse(intent.getStringExtra("url")), mediaDataSourceFactory, new DefaultExtractorsFactory(),
                    mainHandler, null);

            player.addListener(this);
            Log.i(TAG, "About to prepare");
            player.prepare(ms);
            Log.i(TAG, "Prepare completed");
        }

        if (intent.getAction().equals(ACTION_STOP)) {
            Log.i(TAG, "STOP received!");
            if (player != null) {
                player.setPlayWhenReady(false);
                player.stop();
                player.release();
                player = null;
            }
            mgr.cancel(1);
            this.stopSelf();
        }

        return START_STICKY;
    }

    class IcyDataSourceFactory implements DataSource.Factory {

        @Override
        public DataSource createDataSource() {
            return new IcyDataSource();
        }
    }

    class IcyDataSource implements DataSource {

        Uri uri;
        URL url;
        HttpURLConnection conn;
        boolean isIcy;
        int icyDataChunkLength;
        int dataChunkBytesRead; //remains == icyDataChunkLength until metadata read finishes

        int metadataPos;
        int metadataLen;
        //No analogue exists for data chunks because we just return raw bytes to the caller
        //as they come in, but we store up the whole metadata buffer before doing a notification
        //of new metadata. Lazily allocated the max metadata length, what's 4K between friends.
        byte[] metadataBuf = new byte[4080];

        volatile boolean dead = false;
        ConcurrentLinkedQueue<Byte> availableDataBytes = new ConcurrentLinkedQueue<>();

        @Override
        public long open(DataSpec dataSpec) throws IOException {
            uri = dataSpec.uri;
            url = new URL(uri.toString());
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestProperty("Icy-MetaData","1");
            conn.connect();
            if (conn.getHeaderField("icy-metaint") != null) {
                icyDataChunkLength = Integer.valueOf(conn.getHeaderField("icy-metaint"));
                Log.d(TAG, "ICY interval is " + icyDataChunkLength);
                dataChunkBytesRead = 0;
                isIcy = true;
            } else {
                isIcy = false;
                Log.d(TAG, "Server didn't give us an ICY interval; assuming non-ICY server.");
            }
            new Thread(new ReaderThread()).start();
            return dataSpec.length;
        }

        class ReaderThread implements Runnable {
            @Override
            public void run() {
                while (!dead) {
                    byte[] buf = new byte[1024];
                    int len;
                    try {
                        len = conn.getInputStream().read(buf, 0, 1024);
                    } catch (IOException e) {
                        dead = true;
                        Log.d(TAG, "Spinning down icy reader due to exception" ,e);
                        break;
                    }

                    for (int i = 0; i < len; i++) {
                        byte b = buf[i];
                        if (dataChunkBytesRead < icyDataChunkLength) {
                            metadataLen = 0;
                            metadataPos = 0;
                            availableDataBytes.offer(b);
                            dataChunkBytesRead++;
                        } else if (metadataLen == 0) {
                            metadataLen = (b & 0xFF) * 16;
                            Log.d(TAG, "Meta length is " + metadataLen);
                            if (b == 0) {
                                dataChunkBytesRead = 0;
                            }
                        } else if (metadataPos < metadataLen - 1){
                            metadataBuf[metadataPos++] = b;
                        } else {
                            try {
                                byte[] truncated = new byte[metadataLen];
                                System.arraycopy(metadataBuf,0,truncated,0,metadataLen);
                                String metaResult = new String(truncated, "UTF-8").trim();
                                Log.d(TAG,"Meta result is "+ truncated.length + " bytes long, \"" + metaResult + "\'");
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
                                        Intent metaRefresh = new Intent(SdlProxyHost.INTENT_METADATA_REFRESH);
                                        metaRefresh.putExtra("streamTitle", metaResult);
                                        StreamingService.this.getApplicationContext().sendBroadcast(metaRefresh);
                                    }
                                }
                            } catch (UnsupportedEncodingException e) {
                                Log.e(TAG, "couldn't convert metadata result to utf8, unsupported encoding exception", e);
                            }
                            dataChunkBytesRead = 0;
                        }
                    }
                }
                Log.d(TAG, "spun down icy reader");
            }
        }
        @Override
        public int read(byte[] buffer, int offset, int readLength) throws IOException {
            if (isIcy) {
                int ret = 0;
                while (!availableDataBytes.isEmpty() && readLength > 0) {
                    Byte b = availableDataBytes.poll();
                    buffer[offset++] = b;
                    readLength--;
                    ret++;
                }
                return ret;
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
            } catch (InterruptedException e) {
                ;
            }
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
        Log.i(TAG, "Player onPlayerError " + error);
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
