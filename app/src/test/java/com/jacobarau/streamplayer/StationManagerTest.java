package com.jacobarau.streamplayer;

import com.jacobarau.shoutcast.Station;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
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
public class StationManagerTest {
    StationManager stationManager;

    @Before
    public void setUp() {
        stationManager = new StationManager(RuntimeEnvironment.application.getApplicationContext());
    }

    @Test
    public void testStationManager() {
        final String S1_NAME = "Station 1";
        final String S1_URL = "http://1234.com";
        final String S2_NAME = "Station 2";
        final String S2_URL = "http://3456.com";

        stationManager.addStation(S1_NAME, S1_URL);
        stationManager.addStation(S2_NAME, S2_URL);

        List<StationPreset> stations = new ArrayList<>();
        stations.add(new StationPreset(S1_NAME, S1_URL));
        stations.add(new StationPreset(S2_NAME, S2_URL));
        assertEquals(stations, stationManager.getStations());

        List<StationPreset> removeList = new ArrayList<>();
        stationManager.removeStations(removeList);
        assertEquals(stations, stationManager.getStations());

        removeList.add(stations.get(0));
        stationManager.removeStations(removeList);
        stations.remove(0);
        assertEquals(stations, stationManager.getStations());
    }
}
