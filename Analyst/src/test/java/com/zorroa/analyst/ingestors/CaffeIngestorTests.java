package com.zorroa.analyst.ingestors;

import com.google.common.collect.ImmutableList;
import com.zorroa.analyst.AbstractTest;
import com.zorroa.archivist.sdk.domain.AssetBuilder;
import com.zorroa.archivist.sdk.processor.ingest.IngestProcessor;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class CaffeIngestorTests extends AbstractTest {

    @Test
    public void testProcess() {
        final List<IngestProcessor> pipeline = ImmutableList.<IngestProcessor>builder()
                .add(initIngestProcessor(new ImageIngestor()))
                .add(initIngestProcessor(new ProxyProcessor()))
                .add(initIngestProcessor(new CaffeIngestor()))
                .build();

        File file = getResourceFile("/images/faces.jpg");
        AssetBuilder asset = ingestFile(file, pipeline);

        List<String> keywords = asset.getAttr("caffe", "keywords");
        assertEquals(2, keywords.size());
        assertEquals("soccer ball", keywords.get(0));
        assertEquals("swing", keywords.get(1));
    }
}
