package com.zorroa.archivist.sdk.crawlers;

import com.zorroa.archivist.sdk.domain.AnalyzeRequestEntry;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;

/**
 * Not sure how to test this since the search can change.
 */
public class FlickrCrawlerTests {

    static int assetCount = 0;
    static final Consumer<AnalyzeRequestEntry> consumer = entry -> {
        assetCount++;
    };

    URI uri = URI.create("flickr://search?text=rambo&is_commons=true");

    @Test
    public void testStart() throws IOException {
        FlickrCrawler c = new FlickrCrawler();
        c.start(uri, consumer);
        assertTrue(assetCount > 0);
    }
}
