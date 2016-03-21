package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FaceIngestorTests extends AbstractTest {

    @Test
    public void testProcess() throws InterruptedException {
        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(new ProxyProcessor()))
                .add(initIngestProcessor(new FaceIngestor()))
                .build();

        File file = getResourceFile("/images/faces.jpg");
        AssetBuilder asset = ingestFile(file, pipeline);

        Set<String> keywords = asset.getAttr("keywords:face");
        assertEquals(3, keywords.size());
        assertTrue(keywords.contains("face"));
        assertTrue(keywords.contains("face1"));
        assertTrue(keywords.contains("face0.5"));
    }
}
