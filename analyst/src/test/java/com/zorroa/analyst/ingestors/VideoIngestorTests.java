package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by chambers on 4/8/16.
 */
public class VideoIngestorTests extends AbstractTest {

    @Test
    public void testProcess() {


        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new VideoIngestor()))
                .add(initIngestProcessor(new ProxyIngestor()))
                .build();

        AssetBuilder asset = ingestFile(new File("src/test/resources/video/sample_ipad.m4v"), pipeline);
        assertEquals(640, (int) asset.getAttr("video.width"));
        assertEquals(360, (int) asset.getAttr("video.height"));
        assertEquals("Luke crashes sled", asset.getAttr("video.title"));
        assertEquals("A long description of luke sledding in winter.",
                asset.getAttr("video.synopsis"));
        assertEquals("A short description of luke sledding in winter.",
                asset.getAttr("video.description"));

        assertTrue(asset.attrContains("keywords.video", asset.getAttr("video.title")));
        assertTrue(asset.attrContains("keywords.video", asset.getAttr("video.description")));
        assertTrue(asset.attrContains("keywords.video", asset.getAttr("video.synopsis")));
    }
}
