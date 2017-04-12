package com.jacobarau.streamplayer;

/**
 * Created by jacob on 4/12/17.
 */

public class SharedIntents {
    //An Android activity pertaining to the stream player has been presented to the user.
    //This is a cue for SDL to announce that a lock screen is required, optionally.
    public static final String INTENT_ACTIVITY_STARTED = "com.jacobarau.streamplayer.ACTIVITY_STARTED";

    //Streaming service will broadcast this with an extra containing the metadata string
    //(artist and album all in one string, formatted however the stream author formatted it)
    //It is broadcast when the stream starts and whenever the metadata changes.
    public static final String INTENT_METADATA_REFRESH = "com.jacobarau.streamplayer.metadata_refresh";

    //=====================
    //Intents from SDL: will be broadcast whenever the driver distraction status changes, including
    //when SDL itself starts/stops.
    //=====================
    //SDL's driver distraction flag indicates the app must be locked out. If activity exists, it
    //must transition to the lock screen.
    public static final String INTENT_LOCK_SCREEN_REQUIRED = "com.jacobarau.streamplayer.LOCK_SCREEN_REQUIRED";
    //SDL's driver distraction flag indicates the app no longer needs to be locked out. If
    //activity exists, it may now transition to the regular unlocked interface.
    public static final String INTENT_LOCK_SCREEN_NOT_REQUIRED = "com.jacobarau.streamplayer.LOCK_SCREEN_NOT_REQUIRED";
}
