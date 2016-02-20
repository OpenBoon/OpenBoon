package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

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

        List<String> keywords = asset.getAttr("face", "keywords");
        assertEquals(3, keywords.size());
        assertEquals("face", keywords.get(0));
        assertEquals("face1", keywords.get(1));
        assertEquals("face0.5", keywords.get(2));
    }
}
