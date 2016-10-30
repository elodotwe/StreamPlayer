package com.jacobarau.streamplayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;
import com.jacobarau.shoutcast.DirectoryClient;
import com.jacobarau.shoutcast.Genre;
import com.jacobarau.shoutcast.IGenreListQueryListener;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class StreamingService extends Service implements ExoPlayer.EventListener, AudioRendererEventListener {
    final String TAG = "StreamingService";
    private static final String ACTION_START = "com.jacobarau.streamplayer.action.START";
    private static final String ACTION_STOP = "com.jacobarau.streamplayer.action.STOP";

    private static final String ACTION_GET_GENRES = "com.jacobarau.streamplayer.action.GET_GENRES";

    SimpleExoPlayer player = null;
    public static boolean isStreaming = false;
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();

    DecoderCounters cntr = null;

    private DataSource.Factory mediaDataSourceFactory;

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

    public static void queryGenres(Context ctx) {
        Intent intent = new Intent(ctx, StreamingService.class);
        intent.setAction(StreamingService.ACTION_GET_GENRES);
        ctx.startService(intent);
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
     *     DataSource factory.
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
     *     DataSource factory.
     * @return A new HttpDataSource factory.
     */
    private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
        return new DefaultHttpDataSourceFactory(Util.getUserAgent(this, "ExoPlayerDemo"), useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand with intent " + intent + ", flags " + flags + ", startId " + startId);
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (intent.getAction() == ACTION_START) {
            Log.i(TAG, "START received");
            Notification not = new Notification.Builder(this).setContentTitle("Stream Player").setContentText("Playing streaming media").setOngoing(true).build();
            mgr.notify(1, not);

            if (player == null) {
                Log.i(TAG, "player is null");
                // 1. Create a default TrackSelector
                Handler mainHandler = new Handler();
                BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
                TrackSelection.Factory videoTrackSelectionFactory =
                        new AdaptiveVideoTrackSelection.Factory(bandwidthMeter);
                TrackSelector trackSelector =
                        new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);

// 2. Create a default LoadControl
                LoadControl loadControl = new DefaultLoadControl();


// 3. Create the player
                player =
                        ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);
                player.setAudioDebugListener(this);


                mediaDataSourceFactory = buildDataSourceFactory(true);

                MediaSource ms = new ExtractorMediaSource(Uri.parse(intent.getStringExtra("url")), mediaDataSourceFactory, new DefaultExtractorsFactory(),
                        mainHandler, null);


                player.addListener(this);
                Log.i(TAG, "About to prepare");
                player.prepare(ms);
                Log.i(TAG, "Prepare completed");

                Log.i(TAG, "Reached end of player null block");
            }

        }

        if (intent.getAction() == ACTION_STOP) {
            Log.i(TAG, "STOP received!");
            if (player != null) {
                player.setPlayWhenReady(false);
                player.stop();
                player = null;
            }
            mgr.cancel(1);
            this.stopSelf();
        }

        if (intent.getAction() == ACTION_GET_GENRES) {
            Log.i(TAG, "GET_GENRES received");
            DirectoryClient dc = new DirectoryClient(this);
            dc.queryGenres(new IGenreListQueryListener() {
                @Override
                public void onError() {
                    Log.e(TAG, "onError");
                }

                @Override
                public void onResultReturned(Genre[] genres) {
//                    int genreCount = 0;
//                    class DecGenre {
//                        Genre g;
//                        boolean dec;
//                    }
//                    DecGenre[] dec = new DecGenre[genres.length];
//                    for (int i = 0; i < genres.length; i++) {
//                        dec[i] = new DecGenre();
//                        dec[i].g = genres[i];
//                    }
//                    //For each top level genre, we want to print its children. (I'm guessing these are only genre and sub genre?)
//                    for (int i = 0; i< genres.length; i++) {
//                        if (genres[i].getChildren().size() != 0) {
//                            genreCount++;
//                            dec[i].dec = true;
//
//                            Log.i(TAG, "+" + genres[i].getName());
//                            for (int j = 0; j < genres.length; j++) {
//                                if (genres[j].getParentID() == genres[i].getId()) {
//                                    genreCount++;
//                                    dec[j].dec = true;
//                                    Log.i(TAG, "--" + genres[j].getName());
//                                }
//                            }
//                        }
//                    }
//                    Log.i(TAG, "Counted " + genreCount + " genres, compare that to " + genres.length);
//
//                    for (DecGenre g : dec) {
//                        if (!g.dec) {
//                            Log.i(TAG, "nondec " + g.g.getName());
//                        }
//                    }
                }
            }, null);
        }

        return START_STICKY;
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
        exec.schedule(new Runnable(){
            @Override
            public void run(){
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
