package com.zorroa.archivist.sdk.crawlers;

import com.zorroa.archivist.sdk.domain.AnalyzeRequestEntry;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 3/25/16.
 */
public class ShotgunCrawlerTests {

    static int assetCount = 0;
    static final Consumer<AnalyzeRequestEntry> consumer = entry -> {
        assetCount++;
    };

    URI uri = URI.create("shotgun://zorroa.shotgunstudio.com");

    @Test
    public void testStart() throws IOException {
        ShotgunCrawler c = new ShotgunCrawler();
        c.start(uri, consumer);
        assertTrue(assetCount > 0);
    }
}
