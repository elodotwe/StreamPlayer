package com.jacobarau.streamplayer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Created by jacob on 3/18/17.
 */

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class PrefsManagerTest {
    PrefsManager prefsManager;

    @Before
    public void setUp() {
        prefsManager = new PrefsManager(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testStationManager() {
        final String S1_NAME = "Station 1";
        final String S1_URL = "http://1234.com";
        final String S2_NAME = "Station 2";
        final String S2_URL = "http://3456.com";

        prefsManager.addStation(S1_NAME, S1_URL);
        prefsManager.addStation(S2_NAME, S2_URL);

        List<StationPreset> stations = new ArrayList<>();
        stations.add(new StationPreset(S1_NAME, S1_URL));
        stations.add(new StationPreset(S2_NAME, S2_URL));
        assertEquals(stations, prefsManager.getStations());

        List<StationPreset> removeList = new ArrayList<>();
        prefsManager.removeStations(removeList);
        assertEquals(stations, prefsManager.getStations());

        removeList.add(stations.get(0));
        prefsManager.removeStations(removeList);
        stations.remove(0);
        assertEquals(stations, prefsManager.getStations());
    }
}
