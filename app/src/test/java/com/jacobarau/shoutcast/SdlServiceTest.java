

package com.jacobarau.shoutcast;

import android.util.Log;

import com.jacobarau.streamplayer.sdl.SdlService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

/**
 * Created by jacob on 10/22/16.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Log.class})
public class SdlServiceTest {

    @Test
    public void convertGenreTreeToInteractions() throws Exception {
        PowerMockito.mockStatic(Log.class);
        SdlService ss = new SdlService();
        ArrayList<Genre> genres = new ArrayList<>();
        Genre parent = new Genre("Parent", 1, null);
        Genre child1 = new Genre("Child1", 2, parent);
        parent.addChild(child1);
        Genre grandchild1 = new Genre("Grandchile1", 3, child1);
        child1.addChild(grandchild1);
        Genre child2 = new Genre("Child2", 4, parent);
        parent.addChild(child2);

        genres.add(parent);

        Genre parent2 = new Genre("Second parent", 5, null);
        genres.add(parent2);

//        SdlService.GenreTreeConversionResult gcs = ss.convertGenreList(genres);


    }
}