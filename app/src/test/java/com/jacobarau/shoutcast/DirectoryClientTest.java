package com.jacobarau.shoutcast;

import android.util.Log;

import com.jacobarau.net.HTTPClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@PrepareForTest({Log.class})
@RunWith(PowerMockRunner.class)
public class DirectoryClientTest {
    DirectoryClient clientUnderTest;
    HTTPClient mockedClient;

    @Before
    public void setUp() {
        PowerMockito.mockStatic(Log.class);
        mockedClient = mock(HTTPClient.class);
        clientUnderTest = new DirectoryClient(mockedClient);
    }

    @Test
    public void queryStations() throws Exception {
        String url = "http://api.shoutcast.com/station/advancedsearch?k=" + DeveloperKey.DEVELOPER_KEY;
        url += "&f=json&search=radioseven";
        when(mockedClient.fetchURL(any(URL.class))).thenReturn("{\"response\":{\"statusCode\":200,\"data\":{\"stationlist\":{\"station\":[{\"id\":198582,\"genre\":\"Dance\",\"mt\":\"audio/mpeg\",\"name\":\"Radioseven - www.radioseven.se\",\"lc\":249,\"ml\":4225,\"br\":128,\"ct\":\"Armin Van Buuren feat. Lyrica Anderson - Gotta Be Love (Arston Remix)\"},{\"id\":9468112,\"genre\":\"Dance\",\"mt\":\"audio/aacp\",\"name\":\"Radioseven - www.radioseven.se\",\"lc\":5,\"ml\":200,\"br\":64,\"ct\":\"Armin Van Buuren feat. Lyrica Anderson - Gotta Be Love (Arston Remix)\"},{\"id\":699869,\"genre\":\"Dance\",\"mt\":\"audio/mpeg\",\"name\":\"Radioseven - www.radioseven.se\",\"lc\":0,\"ml\":1024,\"br\":128,\"ct\":\"Nicky Romero - The Moment (Novell)\"}],\"tunein\":{\"base-m3u\":\"/sbin/tunein-station.m3u\",\"base\":\"/sbin/tunein-station.pls\",\"base-xspf\":\"/sbin/tunein-station.xspf\"}}},\"statusText\":\"Ok\"}}");
        List<Station> stations = clientUnderTest.queryStations("radioseven", null, null, null, null);
        verify(mockedClient).fetchURL(new URL(url));

        List<Station> expectedStations = new LinkedList<>();
//        {
//            "id": 198582,
//                "genre": "Dance",
//                "mt": "audio\/mpeg",
//                "name": "Radioseven - www.radioseven.se",
//                "lc": 249,
//                "ml": 4225,
//                "br": 128,
//                "ct": "Armin Van Buuren feat. Lyrica Anderson - Gotta Be Love (Arston Remix)"
//        },
//        {
//            "id": 9468112,
//                "genre": "Dance",
//                "mt": "audio\/aacp",
//                "name": "Radioseven - www.radioseven.se",
//                "lc": 5,
//                "ml": 200,
//                "br": 64,
//                "ct": "Armin Van Buuren feat. Lyrica Anderson - Gotta Be Love (Arston Remix)"
//        },
//        {
//            "id": 699869,
//                "genre": "Dance",
//                "mt": "audio\/mpeg",
//                "name": "Radioseven - www.radioseven.se",
//                "lc": 0,
//                "ml": 1024,
//                "br": 128,
//                "ct": "Nicky Romero - The Moment (Novell)"
//        }
        expectedStations.add(new Station("Radioseven - www.radioseven.se", "audio/mpeg", 198582, 128, "Dance", "Armin Van Buuren feat. Lyrica Anderson - Gotta Be Love (Arston Remix)", 249, 4225));
        expectedStations.add(new Station("Radioseven - www.radioseven.se", "audio/aacp", 9468112, 64, "Dance", "Armin Van Buuren feat. Lyrica Anderson - Gotta Be Love (Arston Remix)", 5, 200));
        expectedStations.add(new Station("Radioseven - www.radioseven.se", "audio/mpeg", 699869, 128, "Dance", "Nicky Romero - The Moment (Novell)", 0, 1024));

        assertEquals(expectedStations, stations);
    }

    @Test
    public void queryGenres() throws Exception {

    }

}